package com.ebay.dss.zds.service;

import com.ebay.dss.zds.kerberos.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KerberosContextService implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger();
    private final KerberosContextStore store = new KerberosContextStore();

    private final KerberosContextRefreshService refreshService;

    @Autowired
    public KerberosContextService(KerberosContextRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    public @Nullable
    KerberosContextUser getUser(String name) {
        return store.keySet().stream().filter(u -> u.getUsername().equals(name)).findFirst().orElse(null);
    }

    public @Nullable
    KerberosContext getContext(String name) {
        KerberosContextUser user = getUser(name);
        if (user != null) {
            return store.getContext(user);
        }
        return null;
    }

    public @Nullable
    KerberosContext get(KerberosContextUser user) throws Exception {
        KerberosContext kc = getOrCreate(user);
        processRefresh(kc);
        return kc;
    }

    synchronized @Nullable
    KerberosContext getOrCreate(KerberosContextUser user) throws Exception {
        removeIfPasswordModified(user);
        removeIfKerberosContextClosed(user);
        if (!store.containsKey(user)) {
            logger.info("No existed kerberos context, create {}", user);
            KerberosContext kc = create(user);
            store.putContext(user, kc);
        }
        logger.info("Get kerberos context {}", user);
        return store.getContext(user);
    }

    KerberosContext create(KerberosContextUser user) {
        return user.isPasswordBased() ?
                new UsernamePasswordKerberosContext(
                        user.getUsername(), user.getPassword()) :
                user.isTicketCacheBased() ?
                        new TicketCacheKerberosContext(
                                user.getUsername(), user.getFileLoc()) :
                        new KeytabKerberosContext(
                                user.getUsername(), user.getFileLoc());
    }

    void processRefresh(KerberosContext context) throws Exception {
        if (!refreshService.exist(context)) {
            refreshService.register(context);
            refreshService.schedule(context);
        }
    }

    void removeIfPasswordModified(KerberosContextUser user) {
        KerberosContextUser maybeOldUser = store.getUser(user);
        if (Objects.nonNull(maybeOldUser)
                && user.equals(maybeOldUser)
                && user.isPasswordBased()
                && !Arrays.equals(user.getPassword(), maybeOldUser.getPassword())) {
            logger.info("Password changed, removing {}", user);
            close(maybeOldUser);
        }
    }

    void removeIfKerberosContextClosed(KerberosContextUser user) {
        KerberosContext maybeClosedContext = store.getContext(user);
        if (Objects.nonNull(maybeClosedContext)
                && maybeClosedContext.isClosed()) {
            logger.info("Previous kerberos context closed, removing {}", user);
            close(user);
        }
    }

    private void close(KerberosContextUser user) {
        KerberosContext kc = store.removeContext(user);
        if (refreshService.exist(kc)) {
            refreshService.stop(kc);
        }
        logger.info("Kerberos context is removed for {}", user);
    }

    @PreDestroy
    @Override
    public synchronized void close() {
        store.keySet()
                .forEach(this::close);
    }

    static class KerberosContextStore extends ConcurrentHashMap<KerberosContextUser, KerberosContextUserPair> {

        KerberosContextUser getUser(KerberosContextUser user) {
            return KerberosContextUserPair.getUser(this.get(user));
        }

        KerberosContext getContext(KerberosContextUser user) {
            return KerberosContextUserPair.getContext(this.get(user));
        }

        KerberosContext removeContext(KerberosContextUser user) {
            return KerberosContextUserPair.getContext(this.remove(user));
        }

        KerberosContext putContext(KerberosContextUser user, KerberosContext kc) {
            return KerberosContextUserPair.getContext(this.put(user, new KerberosContextUserPair(user, kc)));
        }
    }

    static class KerberosContextUserPair extends AbstractMap.SimpleImmutableEntry<KerberosContextUser, KerberosContext> {

        KerberosContextUserPair(KerberosContextUser key, KerberosContext value) {
            super(key, value);
        }

        static KerberosContextUser getUser(KerberosContextUserPair pair) {
            return Objects.isNull(pair) ? null : pair.getKey();
        }

        static KerberosContext getContext(KerberosContextUserPair pair) {
            return Objects.isNull(pair) ? null : pair.getValue();
        }
    }

}
