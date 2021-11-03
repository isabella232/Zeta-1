package com.ebay.dss.zds.service;

import com.ebay.dss.zds.common.TempFile;
import com.ebay.dss.zds.common.TempFile.Status;
import com.ebay.dss.zds.serverconfig.DumpProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

@Service
public class DumpFileService {

    private static final Logger logger = LogManager.getLogger();
    private final DumpProperties properties;

    public DumpFileService(DumpProperties properties) {
        this.properties = properties;
    }

    public static Predicate<Path> expirationPolicy(long amount, TemporalUnit unit) {
        return p -> {
            try {
                BasicFileAttributes fileAttributes = Files.readAttributes(p, BasicFileAttributes.class);
                FileTime time = fileAttributes.lastAccessTime();
                Duration timeElapsed = Duration.between(Instant.now(), time.toInstant()).abs();
                return timeElapsed.compareTo(Duration.of(amount, unit)) > 0;
            } catch (IOException e) {
                logger.error(e);
            }
            return false;
        };
    }

    public static Predicate<Path> tempStatusPolicy(Status... statuses) {
        return tempStatusPolicy(Arrays.asList(statuses));
    }

    public static Predicate<Path> tempStatusPolicy(Collection<Status> statuses) {
        return p -> statuses.contains(TempFile.status(p));
    }

    public static Predicate<Path> not(Predicate<Path> predicate) {
        return p -> !predicate.test(p);
    }

    @SafeVarargs
    public static void clean(Path root, Predicate<Path>... policies) throws IOException {
        Stream<Path> tmpPaths = Files.walk(root)
                .sorted(Comparator.reverseOrder())
                .filter(Files::isRegularFile);
        for (Predicate<Path> policy : policies) {
            tmpPaths = tmpPaths.filter(policy);
        }
        tmpPaths.forEach(path -> {
            try {
                Files.delete(path);
                logger.info("Dump file {} is deleted", path);
            } catch (IOException e) {
//                logger.error(e);
            }
        });
    }

    public static long size(Path start) throws IOException {
        return Files.walk(start)
                .mapToLong(p -> {
                    try {
                        return Files.size(p);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
    }

    public void validDiskSpace(String nt, Path ntRoot) throws IOException {
        if (Files.notExists(ntRoot)) {
            return;
        }

        if (size(ntRoot) > properties.getPerUserLimit()) {
            clean(ntRoot, tempStatusPolicy(Status.write_complete));
            if (size(ntRoot) > properties.getPerUserLimit()) {
                throw new RuntimeException("Exceed user dump disk space");
            }
        }

        synchronized (this) {
            if (size(properties.getRoot()) > properties.getTotalLimit()) {
                clean(ntRoot, tempStatusPolicy(Status.write_complete));
                if (size(properties.getRoot()) > properties.getTotalLimit()) {
                    throw new RuntimeException("Exceed total dump disk space");
                }
            }
        }
    }

    public TempFile newFile(String nt, String jobRequestId) throws IOException {
        Path ntRoot = properties.getRoot().resolve(nt);
        validDiskSpace(nt, ntRoot);
        return new TempFile(ntRoot, jobRequestId);
    }

    public TempFile get(String nt, String jobRequestId) throws IOException {
        Path ntRoot = properties.getRoot().resolve(nt);
        return new TempFile(ntRoot, jobRequestId);
    }

    @Async(value = "sundryTaskExecutor")
    @Scheduled(cron = "0 */1 * * * *")
    public void cleanNotWritingOrWrittenExpired10Min() throws IOException {
        clean(properties.getRoot(), expirationPolicy(10, MINUTES),
                not(tempStatusPolicy(Status.writing, Status.write_complete)));
    }

    @Async(value = "sundryTaskExecutor")
    @Scheduled(cron = "0 */10 * * * *")
    public void cleanWrittenExpired4Hour() throws IOException {
        clean(properties.getRoot(), expirationPolicy(4, HOURS),
                tempStatusPolicy(Status.write_complete));
    }

    @PostConstruct
    public void cleanOnStart() throws IOException {
        logger.info("Try to clean up dump files on startup...");
        if (Files.notExists(properties.getRoot()) || !Files.isDirectory(properties.getRoot())) {
            logger.info("Root dump path {} does not exist or is a file", properties.getRoot());
            Files.deleteIfExists(properties.getRoot());
            Files.createDirectory(properties.getRoot());
        }
        clean(properties.getRoot(), tempStatusPolicy(Status.writing, Status.created, Status.unknown));
    }

    @PreDestroy
    public void cleanOnDestory() throws IOException {
        logger.info("Try to clean up dump files on shutdown...");
        clean(properties.getRoot(), tempStatusPolicy(Status.writing, Status.created, Status.unknown));
    }

}
