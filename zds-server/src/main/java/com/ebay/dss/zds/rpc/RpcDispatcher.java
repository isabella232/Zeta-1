package com.ebay.dss.zds.rpc;

import com.ebay.dss.zds.rpc.message.RpcCallCancel;
import com.ebay.dss.zds.rpc.message.RpcMessage;
import com.ebay.dss.zds.rpc.message.RpcReplyMessage;
import com.ebay.dss.zds.rpc.message.RpcRequestMessage;
import com.ebay.dss.zds.runner.CustomizableThreadFactory;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.rpc.utils.FunctionUtils.repeat;

/**
 * Created by tatian on 2020-09-06.
 */
public class RpcDispatcher {

  protected static final Logger logger = LoggerFactory.getLogger(RpcDispatcher.class);

  private RpcEnv rpcEnv;
  private ReentrantLock registerLock = new ReentrantLock();
  private ConcurrentHashMap<RpcAddress, RpcEndpointRef> endpointRefs = new ConcurrentHashMap<>();
  private LinkedBlockingQueue<RpcEndpointRef> receivers;
  private LinkedBlockingQueue<RpcMessage> queue;
  // key is the target canceled message id, value is the cancel call
  private ConcurrentHashMap<Long, RpcCallCancel> canceledMessage = new ConcurrentHashMap<>();

  private ThreadPoolExecutor connectionPool;
  private ThreadPoolExecutor dispatchPool;
  private ThreadPoolExecutor processPool;

  private int dispatchThreadNum;
  private int processThreadNum;

  private volatile boolean started;

  public RpcDispatcher(RpcEnv rpcEnv) {
    this.rpcEnv = rpcEnv;
    this.dispatchThreadNum = conf().getRpcDispatcherDispatchPoolCore();
    this.processThreadNum = conf().getRpcDispatcherProcessPoolCore();
    this.connectionPool = configureConnectionPool();
    this.dispatchPool = configureDispatchPool();
    this.processPool = configureProcessPool();
    this.queue = new LinkedBlockingQueue<>();
    this.receivers = new LinkedBlockingQueue<>();
    threadTaskStartUp();

    this.started = true;
  }

  private RpcConf conf() {
    return this.rpcEnv.getRpcConf();
  }

  private void threadTaskStartUp() {
    repeat(dispatchThreadNum, () -> dispatchPool.submit(createDispatchLoopRunnable()));
    repeat(processThreadNum, () -> processPool.submit(createMessageLoopRunnable()));
  }

  private ThreadPoolExecutor configureConnectionPool() {
    return new ThreadPoolExecutor(conf().getRpcDispatcherConnectionPoolCore(),
            conf().getRpcDispatcherConnectionPoolMax(),
            conf().getRpcDispatcherConnectionPoolKeepAliveSecs(),
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new CustomizableThreadFactory("RpcDispatcher-Connection"));
  }

  private ThreadPoolExecutor configureDispatchPool() {
    return new ThreadPoolExecutor(dispatchThreadNum,
            conf().getRpcDispatcherDispatchPoolMax(),
            conf().getRpcDispatcherDispatchPoolKeepAliveSecs(),
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(conf().getRpcDispatcherDispatchPoolQueueSize()),
            new CustomizableThreadFactory("RpcDispatcher-Dispatch"));
  }

  private ThreadPoolExecutor configureProcessPool() {
    return new ThreadPoolExecutor(processThreadNum,
            conf().getRpcDispatcherProcessPoolMax(),
            conf().getRpcDispatcherProcessPoolKeepAliveSecs(),
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(conf().getRpcDispatcherProcessPoolQueueSize()),
            new CustomizableThreadFactory("RpcDispatcher-Process"));
  }

  public void postMessage(RpcMessage msg) {
    msg.setInTransport();
    this.queue.offer(msg);
  }

  public void broadcastMessageToLocal(RpcMessage msg) {
    for (RpcEndpointRef ref : endpointRefs.values()) {
      if (ref.isLocal()) {
        // only broadcast to local
        RpcMessage redirected = msg.cloneWithNewReceiver(ref.rpcAddress);
        postMessage(redirected);
      }
    }
  }

  protected boolean isCanceled(RpcMessage rpcMessage) {
    return this.canceledMessage.containsKey(rpcMessage.id);
  }

  private Runnable createDispatchLoopRunnable() {
    return () -> {
      while (true) {
        try {
          dispatchMessage();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    };
  }

  private Runnable createMessageLoopRunnable() {
    return () -> {
      try {
        while (true) {
          processMessage();
        }
      } catch (Exception ex) {
        logger.error("The process thread exit by exception: " + ExceptionUtils.getFullStackTrace(ex));
      }
    };
  }

  private void dispatchMessage() {
    RpcMessage message = null;
    try {
      message = this.queue.take();

      if (isCanceled(message)) {
        logger.warn("This rpc message has been canceled: {}", message.toJson());
        rpcEnv.handleRpcCallCanceled(message);
        return;
      }

      RpcCallContext rpcCallContext = warpRpcCallContext(message);
      if (message instanceof RpcRequestMessage && rpcEnv.isOutbound(message)) {
        RpcEndpointRef localSenderRef = endpointRefs.get(message.getSender());
        // add in the un replied box of the local sender
        // in this case the local ref must registered
        localSenderRef.unRepliedBox.offer(rpcCallContext);
      }

      RpcAddress receiver = message.getReceiver();
      if (!endpointRefs.containsKey(receiver)) {
        // no registered rpc
        if (isLocalAddress(receiver)) {
          logger.info("reply the endpoint not found exception: {}", receiver.toString());
          rpcEnv.handleRpcEndpointNotFound(message);
        } else {
          try {
            registerLock.lock();
            if (!endpointRefs.containsKey(receiver)) {
              logger.info("Can't find any endpoint ref of: {}", receiver.toString());
              try {
                logger.info("remote address, try to create new ref: {}", receiver.toString());
                registerRpcEndpointRef(receiver);
              } catch (Exception ex) {
                logger.error("Failed to register endpoint ref: {}, ex:{}", receiver.toString(), ex.toString());
                rpcEnv.handleRpcEndpointCallFailed(message, ex);
                return;
              }
            }
          } finally {
            registerLock.unlock();
          }
        }
      }

      RpcEndpointRef rpcEndpointRef = endpointRefs.get(receiver);
      rpcEndpointRef.inboxOffer(rpcCallContext);
      receivers.offer(rpcEndpointRef);
    } catch (Exception ex) {
      if (message != null) {
        rpcEnv.handleRpcEndpointCallFailed(message, ex);
      }
      logger.error(ex.toString());
    }
  }

  private void processMessage() {
    RpcCallContext rpcCallContext = null;
    try {
      RpcEndpointRef ref = receivers.take();

      if (!ref.isStarted()) {
        logger.warn("The RpcEndpointRef " + ref.getRpcAddress().toString() + " is stopped");
        return;
      }

      rpcCallContext = ref.inboxTake();
      RpcMessage message = rpcCallContext.rpcMessage;

//        logger.info("{} processing message: {}, content: {}",
//                this.rpcEnv.serverAddress.toString(), message.getClass().getName(), message.toJson());

      if (rpcEnv.isCanceled(message)) {
        logger.warn("This rpc message has been canceled: {}", message.toJson());
        rpcEnv.handleRpcCallCanceled(message);
        return;
      }

      if (message instanceof RpcReplyMessage && rpcEnv.isInbound(message)) {
        // a inbound reply message, we set the value directly to the unReplied rpc request
        // and don't need to call on Receive
        boolean replied = ref.handleReplyMessage((RpcReplyMessage) message);
        if (!replied) {
          logger.warn("{} find a RpcReplyMessage to: {}, content : {}, bu has no source: {}, forward to endpoint directly",
                  this.rpcEnv.serverAddress.toString(),
                  ref.rpcAddress.toString(),
                  message.getClass().getName(),
                  message.toString());
          ref.onReceive(rpcCallContext);
        } else {
          logger.info("{} replied: {}", message.getReceiver().toString(), ref.rpcAddress.toString());
        }
      } else {
        ref.onReceive(rpcCallContext);
      }

    } catch (Exception ex) {
      if (rpcCallContext != null) {
        rpcEnv.handleRpcEndpointCallFailed(rpcCallContext.rpcMessage, ex);
      }
      logger.error(ex.toString());
    }
  }

  private boolean isLocalAddress(RpcAddress rpcAddress) {
    return rpcAddress.host.equals(rpcEnv.host) && rpcAddress.port == rpcEnv.port;
  }

  protected RpcCallContext warpRpcCallContext(RpcMessage message) {
    return new RpcCallContext(message, rpcEnv);
  }

  public RpcEndpointRef getRegisteredRpcEndpointRef(RpcAddress rpcAddress) {
    try {
      registerLock.lock();
      return this.endpointRefs.get(rpcAddress);
    } finally {
      registerLock.unlock();
    }
  }

  public RpcEndpointRef registerRpcEndpointRef(RpcAddress rpcAddress, RpcEndpointRef ref) throws Exception {
    try {
      registerLock.lock();
      checkIfRegistered(rpcAddress);
      this.endpointRefs.put(rpcAddress, ref);
      logger.info("RpcEndpoint registered, address: {}", rpcAddress.toString());
      return ref;
    } finally {
      registerLock.unlock();
    }
  }

  public RpcEndpointRef registerRpcEndpointRef(RpcAddress rpcAddress, RpcEndpoint rpcEndpoint) throws Exception {
    try {
      registerLock.lock();
      checkIfRegistered(rpcAddress);
      RpcEndpointRef ref = new RpcEndpointRef(rpcEnv, rpcEndpoint);
      this.endpointRefs.put(rpcAddress, ref);
      logger.info("RpcEndpoint registered, address: {}", rpcAddress.toString());
      return ref;
    } finally {
      registerLock.unlock();
    }
  }

  public RpcEndpointRef registerRpcEndpointRef(RpcAddress rpcAddress) throws Exception {
    try {
      registerLock.lock();
      checkIfRegistered(rpcAddress);
      RpcEndpointRef ref = new RpcEndpointRef(rpcEnv, rpcAddress);
      this.endpointRefs.put(rpcAddress, ref);
      logger.info("RpcEndpoint registered, address: {}", rpcAddress.toString());
      return ref;
    } finally {
      registerLock.unlock();
    }
  }

  public RpcEndpointRef unregisterRpcEndpointRef(RpcAddress rpcAddress) {
    try {
      registerLock.lock();
      RpcEndpointRef ref = this.endpointRefs.remove(rpcAddress);
      if (ref != null) {
        ref.destroy(false);
        logger.info("RpcEndpoint unregistered, address: {}", rpcAddress.toString());
      } else {
        logger.info("RpcEndpoint: {} is not found in local env", rpcAddress.toString());
      }
      return ref;
    } finally {
      registerLock.unlock();
    }
  }

  public Future<RpcEndpointRef> registerRpcEndpointRefAsync(RpcAddress rpcAddress) throws Exception {
    return this.connectionPool.submit(() -> registerRpcEndpointRef(rpcAddress));
  }

  public List<RpcEndpointRef> getRegisteredRpcEndpointRefs() {
    return this.endpointRefs.values().stream().collect(Collectors.toList());
  }

  private void checkIfRegistered(RpcAddress rpcAddress) throws Exception {
    if (this.endpointRefs.containsKey(rpcAddress)) throw new Exception("The" + rpcAddress + " already registered");
  }


  public boolean tryAbortMessageAndCallOut(RpcCallContext callCancel) {
    boolean canceled = false;
    try {
      RpcMessage message = doAbortMessage((RpcCallCancel) callCancel.rpcMessage);
      if (message != null) {
        // if we got the message, it means the message has not been processed or replied
        // so we should reply this message to clean up the sender's un-replied message
        rpcEnv.handleRpcCallCanceled(message);
        canceled = true;
      }
      return canceled;
    } finally {
      callCancel.reply(canceled);
    }
  }

  public RpcMessage tryAbortLocalMessage(long id, RpcAddress receiver, boolean clearUnReplyBox) {
    RpcMessage target = doAbortMessage(new RpcCallCancel(id, rpcEnv.serverAddress, receiver));
    if (target != null && clearUnReplyBox) {
      RpcEndpointRef ref = endpointRefs.get(receiver);
      if (ref != null) {
        Iterator<RpcCallContext> it = ref.unRepliedBox.iterator();
        while (it.hasNext()) {
          RpcCallContext unReplied = it.next();
          if (unReplied.rpcMessage.id == id) {
            it.remove();
          }
        }
      }
    }
    return target;
  }

  private RpcMessage doAbortMessage(RpcCallCancel callCancel) {

    long targetMessageId = callCancel.getMessage();
    try {
      // report the canceled message first
      this.canceledMessage.put(targetMessageId, callCancel);

      // We follow behind the trace of the message workflow to cancel it, it could be slower to cancel but
      // won't miss it when opposite meet

      // 1. find in the queue first
      for (Iterator<RpcMessage> it = this.queue.iterator(); it.hasNext(); ) {
        RpcMessage message = it.next();
        if (callCancel.canRetrieve(message)) {
          it.remove();
          return message;
        }
      }

      // 2. if not in the queue that means it might be reach in one dispatch thread or process thread or the
      // inbox queue. The dispatch thread and process thread would check if the message is canceled, so we next
      // just check target endpoint's inbox
      RpcEndpointRef ref = this.endpointRefs.get(callCancel.getReceiver());
      if (ref != null) {
        for (Iterator<RpcCallContext> it = ref.getInbox().iterator(); it.hasNext(); ) {
          RpcMessage message = it.next().rpcMessage;
          if (callCancel.canRetrieve(message)) {
            it.remove();
            return message;
          }
        }
      } else {
        // the endpoint is not exist or in starting up
      }

      // the task but be a dispatch thread's local variable or being processed, the task could stop
      // in the first case, cancel success but not success in the second one
      // let's consider the second case as cancel success although there could be inconsistency

      return null;
    } finally {
      this.canceledMessage.remove(targetMessageId);
    }
  }

  public void stop() {
    if (started) {
      // todo: implement
    }
  }
}
