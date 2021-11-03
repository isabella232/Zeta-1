package com.ebay.dss.zds.serverconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "zds.dump")
public class DumpProperties {

    private Path root = Paths.get("/tmp/zds-dump");
    private long perUserLimit = Long.MAX_VALUE;
    private long totalLimit = Long.MAX_VALUE;

    public Path getRoot() {
        return root;
    }

    public DumpProperties setRoot(Path root) {
        this.root = root;
        return this;
    }

    public long getPerUserLimit() {
        return perUserLimit;
    }

    public DumpProperties setPerUserLimit(long perUserLimit) {
        this.perUserLimit = perUserLimit;
        return this;
    }

    public long getTotalLimit() {
        return totalLimit;
    }

    public DumpProperties setTotalLimit(long totalLimit) {
        this.totalLimit = totalLimit;
        return this;
    }
}
