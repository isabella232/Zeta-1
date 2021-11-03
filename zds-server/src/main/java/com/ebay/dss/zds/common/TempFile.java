package com.ebay.dss.zds.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import static com.ebay.dss.zds.common.TempFile.Status.*;

public class TempFile {

    private static final Logger logger = LogManager.getLogger();
    private String fileNamePrefix;
    private Path root;
    private Path targetRoot;

    public String getFileNamePrefix() {
        return fileNamePrefix;
    }

    public Path getRoot() {
        return root;
    }

    public Path getTargetRoot() {
        return targetRoot;
    }

    public TempFile(Path root, String fileNamePrefix) {
        this.fileNamePrefix = fileNamePrefix;
        this.root = root;
        this.targetRoot = this.root.resolve(this.fileNamePrefix + ".");
    }

    public Path target() throws IOException {
        return Files.find(targetRoot.getParent(), 1,
                (path, basicFileAttributes) -> path.getFileName().toString().startsWith(fileNamePrefix))
                .findFirst()
                .get();
    }

    public static Status status(Path path) {
        try {
            Path fileNamePath = path.getFileName();
            String fileName = fileNamePath.toString();
            String[] fileNameComponents = fileName.split("\\.", -1);
            return valueOf(fileNameComponents[fileNameComponents.length - 1]);
        } catch (Exception e) {
            return unknown;
        }

    }

    private static Path changeStatus(Path target, Collection<Status> validPrevious, Status next) throws IOException {
        if (validPrevious.contains(status(target))) {
            return changeStatus(target, next);
        }
        return target;
    }

    private static Path changeStatus(Path target, Status next) throws IOException {
        String tmpFileName = target.getFileName().toString();
        int lastDotIdx = tmpFileName.lastIndexOf('.');
        tmpFileName = tmpFileName.substring(0, lastDotIdx) + "." + next;
        return Files.move(target, target.resolveSibling(tmpFileName));
    }

    private void createRoot() throws IOException {
        if (!Files.isDirectory(root)) {
            synchronized (TempFile.class) {
                if (!Files.isDirectory(root)) {
                    Files.deleteIfExists(root);
                    logger.info("Try create dir {}", root);
                    Files.createDirectory(root);
                }
            }
        }
    }

    public Status status() {
        try {
            return status(target());
        } catch (IOException e) {
            return unknown;
        }
    }

    public void create() throws IOException {
        createRoot();
        Files.createFile(root.resolve(fileNamePrefix + "." + created));
    }

    public void write(Consumer<Writer> doWrite) throws IOException {
        changeStatus(target(), Collections.singleton(created), writing);
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(target())) {
            if (status() == writing) {
                doWrite.accept(bufferedWriter);
            }
        } finally {
            changeStatus(target(), Collections.singleton(writing), write_complete);
        }
    }

    public void read(Consumer<Reader> doRead) throws IOException {
        if (status() == write_complete)
            try (BufferedReader bufferedReader = Files.newBufferedReader(target())) {
                doRead.accept(bufferedReader);
            }
    }

    public void delete() throws IOException {
        Path target = target();
        Files.deleteIfExists(target);
    }

    public enum Status {
        created,
        writing,
        write_complete,
        unknown
    }
}
