package com.ebay.dss.zds.runner;

import com.ebay.dss.zds.common.BucketReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static com.ebay.dss.zds.common.PropertiesUtil.getInt;
import static com.ebay.dss.zds.common.PropertiesUtil.getLong;
import static com.ebay.dss.zds.runner.Tenant.*;

/**
 * Created by tatian on 2020-08-27.
 */
public class ExecutionDispatcher {

  protected static final Logger logger = LoggerFactory.getLogger(ExecutionDispatcher.class);

  public static final String EXECUTION_EXECUTOR_CREATE_CONCURRENCE_LIMIT = "zds.execution.executor.create.concurrence.limit";
  public static final String DEFAULT_EXECUTION_PREFIX = "zds.execution.default.";

  public static final String DEFAULT_CORE_POOL_SIZE = DEFAULT_EXECUTION_PREFIX + CORE_POOL_SIZE;
  public static final String DEFAULT_MAX_POOL_SIZE = DEFAULT_EXECUTION_PREFIX + MAX_POOL_SIZE;
  public static final String DEFAULT_QUEUE_CAPACITY = DEFAULT_EXECUTION_PREFIX + QUEUE_CAPACITY;
  public static final String DEFAULT_KEEP_ALIVE_SECONDS = DEFAULT_EXECUTION_PREFIX + KEEP_ALIVE_SECONDS;
  public static final String DEFAULT_TASK_START_TIMEOUT = DEFAULT_EXECUTION_PREFIX + TASK_START_TIMEOUT;

  private final ConcurrentHashMap<String, DelegatingExecutor> tenants = new ConcurrentHashMap<>();
  private final BucketReentrantLock bucketReentrantLock;

  private final Properties properties;
  private final int defaultCorePoolSize;
  private final int defaultMaxPoolSize;
  private final int defaultQueueCapacity;
  private final int defaultKeepAliveSeconds;
  private final long defaultTaskStartTimeout;

  private int executorCreateConcurrenceLimit;

  private static ExecutionDispatcher _instance;

  public ExecutionDispatcher(Properties prop) {
    this.properties = prop;
    this.defaultCorePoolSize = getInt(DEFAULT_CORE_POOL_SIZE, prop, 10);
    this.defaultMaxPoolSize = getInt(DEFAULT_MAX_POOL_SIZE, prop, 512);
    this.defaultQueueCapacity = getInt(DEFAULT_QUEUE_CAPACITY, prop, 128);
    this.defaultKeepAliveSeconds = getInt(DEFAULT_KEEP_ALIVE_SECONDS, prop, 600);
    this.defaultTaskStartTimeout = getLong(DEFAULT_TASK_START_TIMEOUT, prop, -1);
    this.executorCreateConcurrenceLimit = getInt(EXECUTION_EXECUTOR_CREATE_CONCURRENCE_LIMIT, prop, 0);
    this.bucketReentrantLock = new BucketReentrantLock(executorCreateConcurrenceLimit <= 0 ? 1 : executorCreateConcurrenceLimit);
    logger.info("ExecutionDispatcher created with:\n" +
                    "defaultCorePoolSize: {}\n" +
                    "defaultMaxPoolSize: {}\n" +
                    "defaultQueueCapacity: {}\n" +
                    "defaultKeepAliveSeconds: {}\n" +
                    "defaultTaskStartTimeout: {}\n" +
                    "executorCreateConcurrence: {}",
            defaultCorePoolSize,
            defaultMaxPoolSize,
            defaultQueueCapacity,
            defaultKeepAliveSeconds,
            defaultTaskStartTimeout,
            executorCreateConcurrenceLimit);
  }

  public static synchronized ExecutionDispatcher singleton(Properties prop) {
    if (_instance == null) {
      _instance = new ExecutionDispatcher(prop);
    }
    return _instance;
  }

  protected synchronized void initExecutor(String tenantName) {
    initExecutor(tenantName, properties);
  }

  protected synchronized void initExecutor(String tenantName, Properties properties) {
    if (!tenants.containsKey(tenantName)) {
      tenants.put(tenantName, createExecutor(tenantName, properties));
    } else {
      logger.warn("There is already a tenant called: {}", tenantName);
    }
  }

  public DelegatingExecutor createExecutor(String tenantName) {
    return createExecutor(tenantName, properties);
  }

  public final DelegatingExecutor createExecutor(String tenantName, Properties properties) {

    String tenantFullName = getFullTenantName(tenantName);

    int corePoolSize = getInt(tenantFullName + "." + CORE_POOL_SIZE, properties, defaultCorePoolSize);
    int maxPoolSize = getInt(tenantFullName + "." + MAX_POOL_SIZE, properties, defaultMaxPoolSize);
    int queueCapacity = getInt(tenantFullName + "." + QUEUE_CAPACITY, properties, defaultQueueCapacity);
    int keepAliveSeconds = getInt(tenantFullName + "." + KEEP_ALIVE_SECONDS, properties, defaultKeepAliveSeconds);
    long taskStartTimeout = getLong(tenantFullName + "." + TASK_START_TIMEOUT, properties, defaultTaskStartTimeout);
    String threadNamePrefix = "ZetaExecutionThread-" + tenantName + "-";

    BlockingQueue<Runnable> queue;
    if (queueCapacity == 0) {
      taskStartTimeout = -1;
      queue = new SynchronousQueue<>();
    } else {
      queue = new LinkedBlockingDeque<>(queueCapacity);
    }

    ThreadPoolExecutor delegatedExecutor = new ThreadPoolExecutor(corePoolSize,
            maxPoolSize,
            keepAliveSeconds,
            TimeUnit.SECONDS,
            queue,
            // The threadPriority = 5, make sure to be the same as the InBound/Broker/Outbound
            new CustomizableThreadFactory(threadNamePrefix),
            new ThreadPoolExecutor.AbortPolicy());

    logger.info("Executor: {} created with:\n" +
                    "corePoolSize: {}\n" +
                    "maxPoolSize: {}\n" +
                    "queueCapacity: {}\n" +
                    "keepAliveSeconds: {}\n" +
                    "taskStartTimeout: {}",
            tenantName, corePoolSize, maxPoolSize, queueCapacity, keepAliveSeconds, taskStartTimeout);

    //SecurityContext securityContext = SecurityContextHolder.getContext();
    return new DelegatingExecutor(new Tenant(tenantName, properties),
            this, delegatedExecutor,
            // securityContext,
            taskStartTimeout);
  }

  protected DelegatingExecutor findExecutor(String tenantName) {
    return this.tenants.get(tenantName);
  }

  private DelegatingExecutor findOrCreateExecutor(String tenantName) {
    DelegatingExecutor executor;
    if (executorCreateConcurrenceLimit > 0) {
      ReentrantLock lock = bucketReentrantLock.getLock(tenantName);
      try {
        lock.lock();
        executor = innerFindOrCreateExecutor(tenantName);
      } finally {
        lock.unlock();
      }
    } else {
      executor = innerFindOrCreateExecutor(tenantName);
    }
    return executor;
  }

  // this is not thread safe
  private DelegatingExecutor innerFindOrCreateExecutor(String tenantName) {
    DelegatingExecutor executor = tenants.get(tenantName);
    if (executor == null) {
      logger.info("No tenant: {} found, create it", tenantName);
      executor = createExecutor(tenantName);
      tenants.put(tenantName, executor);
    }
    return executor;
  }

  public boolean hasTenant(String tenantName) {
    return this.tenants.containsKey(tenantName);
  }

  public void destroyTenant(String tenantName, boolean force) {
    DelegatingExecutor delegatingExecutor = tenants.remove(tenantName);
    if (delegatingExecutor != null) {
      logger.info("Shutdown tenant: {}, force: {}", tenantName, force);
      delegatingExecutor.shutdown(force);
      logger.info("Already shutdown tenant: {}, force: {}", tenantName, force);
    }
  }

  public final void dispatch(String tenantName, Runnable task) {
    DelegatingExecutor executor = findOrCreateExecutor(tenantName);
    executor.execute(task);
    logger.info("Task dispatched to the tenant: {}", tenantName);
  }

  public Map<String, String> showTenants() {
    Map<String, String> map = new HashMap<>();
    tenants.keySet().forEach(key -> map.put(key, tenants.get(key).toString()));
    return map;
  }

  public void destroy(boolean force) {
    logger.info("Shutdown all tenants...");
    tenants.values().stream().parallel().forEach(e -> e.shutdown(force));
    logger.info("Already shutdown all tenants");
  }

  public static class DelegatingExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(DelegatingExecutor.class);

    public final Tenant tenant;
    private final ExecutionDispatcher executionDispatcher;
    private final ThreadPoolExecutor delegate;
    //private final SecurityContext securityContext;
    private final long taskStartTimeout;
    private Thread taskCheckThread;
    private volatile boolean start;

    public DelegatingExecutor(Tenant tenant,
                              ExecutionDispatcher executionDispatcher,
                              ThreadPoolExecutor delegateExecutor,
                              //SecurityContext securityContext,
                              long taskStartTimeout) {
      Assert.notNull(delegateExecutor, "delegateExecutor cannot be null");
      this.tenant = tenant;
      this.executionDispatcher = executionDispatcher;
      this.delegate = delegateExecutor;
      //this.securityContext = securityContext;
      this.taskStartTimeout = taskStartTimeout;
      this.start = true;
      if (tenant.fallbackTenants().size() > 0 && taskStartTimeoutEnabled()) {
        checkQueue(this.delegate);
        this.taskCheckThread = new Thread(this::checkTimeoutTask);
        this.taskCheckThread.setName("TimeoutTaskCheckThread-" + tenant.getName());
        this.taskCheckThread.start();
        logger.info("TimeoutTaskCheckThread started: {}", tenant.getName());
      }
    }

    private void checkQueue(ThreadPoolExecutor delegateExecutor) {
      assert delegateExecutor.getQueue() instanceof LinkedBlockingDeque;
    }

    // if this start, it means has fallback tenant otherwise the thread won't start
    private void checkTimeoutTask() {
      while (this.start) {
        try {
          LinkedBlockingDeque<Runnable> queue = (LinkedBlockingDeque) this.getQueue();
          Iterator<Runnable> it = queue.iterator();
          while (it.hasNext()) {
            // Don't pull it out since the task may can't be executed by the fallbacks
            DelayedRunnable runnable = (DelayedRunnable) it.next();
            if (runnable != null && runnable.canExecute() && runnable.isTimeout(taskStartTimeout)) {
              List<String> fallbacks = tenant.fallbackTenants();
              for (String tenant : fallbacks) {

                if (!runnable.canExecute()) {
                  logger.info("The task already been executed", this.tenant.getName(), tenant);
                  break;
                }

                try {
                  // If this task dispatched to the target tenant, it will be
                  // marked as can't be execute, so there is no double execute
                  logger.info("Detected timeout task in current tenant: {}, dispatch to: {}",
                          this.tenant.getName(), tenant);
                  this.executionDispatcher.dispatch(tenant, runnable);
                  it.remove();
                  break;
                } catch (RejectedExecutionException ree) {
                  logger.info("Got reject by put tasks to tenant: {} from: {}", tenant, this.tenant.getName());
                }
              }

            } else if (runnable != null && runnable == queue.peekFirst()) {
              // This is a FIFO queue, so if the head is not timeout, the rest won't timeout
              // And if the head is timeout the rest might timeout
              break;
            }
          }
          try {
            Thread.sleep(1000L);
          } catch (InterruptedException ie) {
            logger.info(Thread.currentThread().getName() + ": has been interrupted");
            this.start = false;
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }

    private boolean taskStartTimeoutEnabled() {
      return this.taskStartTimeout > 0;
    }

    public DelegatingExecutor(Tenant tenant, ExecutionDispatcher executionDispatcher, ThreadPoolExecutor delegate) {
      this(tenant, executionDispatcher, delegate,
              //null,
              -1);
    }

    protected ThreadPoolExecutor delegate() {
      return this.delegate;
    }

    public final void execute(Runnable task) {
      //task = this.secured(task);
      this.delegate.execute(new DelayedRunnable(task));
    }

    public Future<?> submit(Runnable task) {
      return this.delegate.submit(task);
    }

    public <T> Future<T> submit(Callable<T> task) {
      return this.submit(task);
    }

    protected final ThreadPoolExecutor getDelegateExecutor() {
      return this.delegate;
    }

//    protected final Runnable secured(Runnable delegate) {
//      return DelegatingSecurityContextRunnable.create(delegate, this.securityContext);
//    }

    // guaranteed by checkQueue()
    protected BlockingQueue<Runnable> getQueue() {
      return this.delegate.getQueue();
    }

    public void shutdown(boolean force) {
      this.start = false; // this will make the timeout thread exit
      if (force) {
        this.delegate.shutdownNow();
      } else {
        this.delegate.shutdown();
      }
    }

    @Override
    public String toString() {
      return this.delegate.toString();
    }
  }

  public static class DelayedRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DelayedRunnable.class);

    public final long submitTime;
    private Runnable runnable;
    private AtomicBoolean canExecute;

    public DelayedRunnable(Runnable runnable) {
      this.runnable = runnable;
      this.submitTime = System.currentTimeMillis();
      this.canExecute = new AtomicBoolean(true);
    }

    public void run() {
      if (canExecute.compareAndSet(true, false)) {
        this.runnable.run();
      } else {
        logger.info("This task has been executed once...");
      }
    }

    public boolean isTimeout(long threshold) {
      return threshold < System.currentTimeMillis() - submitTime;
    }

    public boolean canExecute() {
      return this.canExecute.get();
    }

    public Runnable getDelegate() {
      return this.runnable;
    }
  }

}
