package com.bridge.plugin.treegraph.service;

import com.bridge.plugin.treegraph.Utils;
import com.bridge.plugin.treegraph.blockchain.Abi;
import conflux.web3j.jsonrpc.Cfx;
import conflux.web3j.jsonrpc.Request;
import conflux.web3j.request.Epoch;
import conflux.web3j.request.LogFilter;
import conflux.web3j.response.Block;
import conflux.web3j.response.Log;
import conflux.web3j.response.Status;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class ConsortiumPlugin {
    Logger log = LoggerFactory.getLogger(getClass());
    private Cfx cfx;
    private LogFilter filter;
    private RestTemplate restTemplate;
    private ChainConfig config;
    private long latestEpoch;

    public static void main(String[] args) {
    }

    String crossRequestTopic = "0x3863b4c2b672e3221642422fc9637df01babed1d750409520a373b4a787afc59";

    @Value("${chain.id}")
    String chainId;
    @Value("${bridge.host}")
    String bridgeHost;
    String vaultAddress;
    String cfxUrl;

    @PostConstruct
    public void init() {
        log.info("use bridge host [{}] , chain id {}", bridgeHost, chainId);
        RestTemplate restTemplate = new RestTemplateBuilder()
                .build();
        this.restTemplate = restTemplate;
        log.info("fetch config from bridge...");

        ChainConfig config = Utils.getRpc(restTemplate, bridgeHost + "/pluginChainInfo?id=" + chainId, ChainConfig.class);
        log.info("config is {}", Utils.toJson(config));

        this.cfxUrl = config.rpc;
        this.vaultAddress = config.vaultAddr;
        this.config = config;

        Cfx cfx = Cfx.create(cfxUrl);
        this.cfx = cfx;
        updateLatestEpoch();

        LogFilter filter = new LogFilter();
        filter.setAddress(Collections.singletonList(vaultAddress));
        filter.setTopics(
                Collections.singletonList(
                        Collections.singletonList(crossRequestTopic)
                )
        );
        this.filter = filter;
    }

    private void updateLatestEpoch() {
        Request<Status, Status.Response> request = cfx.getStatus();
        Status status = request.sendAndGet();
        this.latestEpoch = status.getEpochNumber().longValue();
        log.info("latestEpoch {}", latestEpoch);
    }

    public ReportCrossRequest fetchLogs() {
        EventCursor cursor = Utils.getRpc(this.restTemplate, bridgeHost + "/eventCursor?id=" + chainId, EventCursor.class);
        long useEpoch = cursor.block + 1;
        ReportCrossRequest report = fetchLogs(useEpoch);
        if (report != null) {
            Utils.postRpc(restTemplate, bridgeHost+"/reportCrossRequest", report);
        }
        return report;
    }
    public ReportCrossRequest fetchLogs(long useEpoch) {
        if (useEpoch > this.latestEpoch + this.config.delayBlock) {
            updateLatestEpoch();
            return null;
        }
        Epoch objEpoch = Epoch.numberOf(useEpoch);
        filter.setFromEpoch(objEpoch);
        filter.setToEpoch(objEpoch);
        log.debug("use epoch {}", useEpoch);
        List<Log> logs = this.cfx.getLogs(filter).sendAndGet();
        log.info("epoch {} log count {}", useEpoch, logs.size());
        long blockTime;
        if (logs.size() > 0) {
            Block block = this.cfx.getBlockByEpoch(objEpoch).sendAndGet().get();
            blockTime = block.getTimestamp().longValue() * 1000;
        } else {
            blockTime = 0;
        }

        ReportCrossRequest report = new ReportCrossRequest();
        report.chainId = chainId;
        report.cursor = useEpoch;
        report.blockTime = blockTime;
        parseLogs(report, logs);

        report.infos.forEach(info->{
            info.blockNumber = useEpoch;
            info.blockTime = Utils.isoDate(new Date(blockTime));
            info.chainId = chainId;
        });
        return report;
    }

    private void parseLogs(ReportCrossRequest report, List<Log> logs) {
        List<CrossInfo> infos = new ArrayList<>(logs.size());
        List<List<CrossItem>> items = new ArrayList<>(logs.size());
        for (Log log : logs) {
            CrossInfo info = new CrossInfo();
            Abi.decodeCrossRequest(log, info);
            List<CrossItem> crossItems = Abi.decodeCrossRequest(log.getData(), info);
            infos.add(info);
            items.add(crossItems);
        }
        report.infos = infos;
        report.items = items;
    }
}
