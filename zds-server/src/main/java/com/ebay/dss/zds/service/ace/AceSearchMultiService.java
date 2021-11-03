package com.ebay.dss.zds.service.ace;

import com.ebay.dss.zds.model.ace.AceSearchEntry;
import com.ebay.dss.zds.model.ace.AceSearchOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class AceSearchMultiService implements AceSearchService {

    private static final Logger logger = LogManager.getLogger();
    private final AceSearchLogService logService;
    private final List<AceSearchService> services;
    private final ScheduledExecutorService cancellationPool;
    private final ExecutorService workerPool;
    private final long timeout;
    private final TimeUnit timeoutUnit;

    public AceSearchMultiService(AceSearchLogService logService,
                                 List<AceSearchService> services) {
        this.logService = logService;
        this.services = services;
        this.timeout = 3;
        this.timeoutUnit = TimeUnit.SECONDS;
        this.cancellationPool = Executors.newScheduledThreadPool(8);
        this.workerPool = new ThreadPoolExecutor(2, 16, 5, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(1024), new ThreadPoolExecutor.AbortPolicy());
    }


    @Override
    public Optional<AceSearchEntry> search(AceSearchOptions options) {
        List<CompletableFuture<Optional<AceSearchEntry>>> futures = new ArrayList<>();
        for (AceSearchService service : services) {
            // each service creates a search future
            Optional<CompletableFuture<Optional<AceSearchEntry>>> optionalFuture = launchIfScopesIn(service, options);
            optionalFuture.ifPresent(futures::add);
        }

        // join all search futures
        CompletableFuture<Void> doneFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        CompletableFuture<List<AceSearchEntry>> resultsFuture = doneFuture.thenApply(f -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList())
        ).thenApply(optionals -> optionals.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList())
        );
        // if all search requests are done, return
        AceSearchEntry entry = getResult(resultsFuture);
        logService.log(options, entry);
        return Optional.of(entry);
    }

    private Optional<CompletableFuture<Optional<AceSearchEntry>>> launchIfScopesIn(AceSearchService service, AceSearchOptions options) {
        final CompletableFuture<Optional<AceSearchEntry>> future;
        if (containSome(service.scopes(), options.getScopes())) {
            // create a search action
            future = CompletableFuture.supplyAsync(() -> service.search(options), workerPool);
            // cancel if timeout
            cancellationPool.schedule(() -> {
                if (!future.isDone()) {
                    future.complete(Optional.empty());
                    future.cancel(true);
                }
            }, timeout, timeoutUnit);
            return Optional.of(future);
        }
        return Optional.empty();
    }

    private AceSearchEntry getResult(CompletableFuture<List<AceSearchEntry>> resultsFuture) {
        AceSearchEntry entry = new AceSearchEntry();
        if (resultsFuture == null) {
            return entry;
        }
        try {
            List<AceSearchEntry> results = resultsFuture.get();
            for (AceSearchEntry result : results) {
                entry.putAll(result);
            }
        } catch (InterruptedException e) {
            logger.warn("ace search service interrupted", e);
        } catch (ExecutionException e) {
            logger.warn("ace search service execution error", e);
        } catch (CancellationException e) {
            logger.warn("ace search service cancelled", e);
        }
        return entry;
    }

    /**
     * If a has some value in b return true
     *
     * @param a
     * @param b
     * @return
     */
    private boolean containSome(Collection<String> a, String[] b) {
        for (String v : b) {
            if (a.contains(v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<String> scopes() {
        return services.stream()
                .flatMap(s -> s.scopes().stream())
                .collect(Collectors.toSet());
    }
}
