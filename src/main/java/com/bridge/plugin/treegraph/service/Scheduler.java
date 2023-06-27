package com.bridge.plugin.treegraph.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
public class Scheduler {
    Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    ConsortiumPlugin plugin;

    @PostConstruct
    public void run() {
        new Thread(()->{
            while(true) {
                int sleepSeconds = 0;
                try {
                    ReportCrossRequest report = plugin.fetchLogs();
                    if (report == null) {
                        sleepSeconds = 1;
                    }
                } catch (ResourceAccessException rae) {
                    sleepSeconds = 10;
                    log.error("fetch logs error {}", rae.getMessage());
                } catch (Exception e) {
                    log.error("fetch logs error", e);
                    sleepSeconds = 10;
                }
                if (sleepSeconds > 0) {
                    try {
                        Thread.sleep(sleepSeconds * 1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }, "fetch logs").start();
    }
}
