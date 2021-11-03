package com.ebay.dss.zds.service;

import org.springframework.scheduling.annotation.Scheduled;

public class ElasticLogJobs {

    private ElasticLogService service;

    public ElasticLogJobs(ElasticLogService service) {
        this.service = service;
    }

    @Scheduled(fixedRateString = "#{@'elastic-log-schedule-properties'.fixedRate}",
            initialDelayString = "#{@'elastic-log-schedule-properties'.fixedDelay}")
    public void indexJob() {
        service.index();
    }
}
