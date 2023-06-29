package com.bridge.plugin.treegraph.service;

import conflux.web3j.jsonrpc.RpcException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
public class Scheduler {
    Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    ConsortiumPlugin plugin;

    @PostConstruct
    public void run() {
        runEventFetcher();
        runClaimer();
    }

    public void runClaimer() {
        new Thread(() -> {
            while (true) {
                int sleepSeconds = 0;
                try {
                    plugin.claim();
                } catch (Exception e) {
                    sleepSeconds = 10;
                    log.error("claim error", e);
                }
                if (sleepSeconds > 0) {
                    try {
                        Thread.sleep(sleepSeconds * 1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }, "claimer").start();
    }

    public void runEventFetcher() {
        new Thread(() -> {
            while (true) {
                int sleepSeconds = 0;
                try {
                    ReportCrossRequest report = plugin.fetchLogs();
                    if (report == null) {
                        sleepSeconds = 1;
                    }
                } catch (RpcException rae) {
                    sleepSeconds = 5;
                    log.error("rpc exception {} data = {}", rae.getError().getMessage(), rae.getError().getData());
                } catch (ResourceAccessException rae) {
                    sleepSeconds = 10;
                    String message = rae.getMessage();
                    log.error("fetch logs error {}", message);
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
