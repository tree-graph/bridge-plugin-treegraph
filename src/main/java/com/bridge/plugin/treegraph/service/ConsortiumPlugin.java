package com.bridge.plugin.treegraph.service;

import com.bridge.plugin.treegraph.Utils;
import com.bridge.plugin.treegraph.blockchain.Abi;
import conflux.web3j.jsonrpc.Account;
import conflux.web3j.jsonrpc.Cfx;
import conflux.web3j.jsonrpc.Request;
import conflux.web3j.request.Epoch;
import conflux.web3j.request.LogFilter;
import conflux.web3j.response.Block;
import conflux.web3j.response.Log;
import conflux.web3j.response.Receipt;
import conflux.web3j.response.Status;
import conflux.web3j.types.Address;
import conflux.web3j.types.RawTransaction;
import conflux.web3j.types.SendTransactionResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.web3j.crypto.Hash;
import org.web3j.utils.Convert;

import java.math.BigInteger;
import java.util.*;

@Component
public class ConsortiumPlugin {
    static final String ClaimStepSendingTx = "sending_tx";
    static final String ClaimStepWaitingTx = "waiting_tx";
    static final String ClaimStepError = "error";
    Logger log = LoggerFactory.getLogger(getClass());
    private Cfx cfx;
    private LogFilter filter;
    private RestTemplate restTemplate;
    private ChainConfig config;
    private long latestEpoch;
    private Account account;

    public static void main(String[] args) {
    }

    String crossRequestTopic = "0x3863b4c2b672e3221642422fc9637df01babed1d750409520a373b4a787afc59";

//    @Value("${base32address}")
//    String base32address;
    @Value("${privateKey}")
    String privateKey;
    @Value("${chain.id}")
    String chainId;
    @Value("${bridge.host}")
    String bridgeHost;
    String vaultAddress;
    String cfxUrl;

    @PostConstruct
    public void init() {
        log.info("use bridge-service host [{}] , chain id {}", bridgeHost, chainId);
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
        log.info("updateLatestEpoch");
        updateLatestEpoch();

        LogFilter filter = new LogFilter();
        filter.setAddress(Collections.singletonList(vaultAddress));
        filter.setTopics(
                Collections.singletonList(
                        Collections.singletonList(crossRequestTopic)
                )
        );
        this.filter = filter;
        log.info("use pk {}", privateKey);
        this.account = Account.create(cfx, privateKey);
        log.info("use account {} , balance {}", account.getAddress(),
                Convert.fromWei(cfx.getBalance(account.getAddress()).sendAndGet().toString(), Convert.Unit.ETHER));
    }

    private void updateLatestEpoch() {
        Request<Status, Status.Response> request = cfx.getStatus();
        Status status = request.sendAndGet();
        this.latestEpoch = status.getEpochNumber().longValue();
        if (latestEpoch % 100 == 0) {
            log.info("latestEpoch {}", latestEpoch);
        }
    }

    public ReportCrossRequest fetchLogs() {
        EventCursor cursor = Utils.getRpc(this.restTemplate, bridgeHost + "/eventCursor?id=" + chainId, EventCursor.class);
        long useEpoch = cursor.block + 1;
        if (useEpoch > this.latestEpoch) {
            updateLatestEpoch();
            return null;
        }
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
        if (useEpoch % 100 == 0 || logs.size() > 0) {
            log.info("epoch {} log count {}", useEpoch, logs.size());
        }
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

    public void claim() throws Exception {
        String url = bridgeHost + "/getPooledClaim?id=" + chainId;
        log.info("url {}", url);
        HashMap claimPoolMap = Utils.getRpc(restTemplate, url, HashMap.class);
        log.info("claim pool is {}", Utils.toJson(claimPoolMap));
        if ((Boolean) claimPoolMap.get("hasPooledClaim")) {
            ClaimPool claimPool = Utils.toObj(claimPoolMap.get("claim"), ClaimPool.class);
            if (claimPool.step.equals(ClaimStepSendingTx)) {
                sendClaimTx(claimPool);
            } else if (claimPool.step.equals(ClaimStepWaitingTx)) {
                checkReceipt(claimPool);
            } else {
                throw new RuntimeException("unsupported claim step " + claimPool.step);
            }
        } else {
            // check claim
            checkClaimTask();
        }
    }

    void checkClaimTask() {
        String url = bridgeHost + "/checkClaimTask?id=" + chainId + "&clientName=pluginChain"+chainId;
        log.info("url {}", url);
        String ret = Utils.getRpc(restTemplate, url, String.class);
        log.info("checkClaimTask result {}", ret);
        if ("0".equals(ret)) {
            return;
        }
        log.info("no claim task");
        Utils.sleep(5_000);
    }

    private void checkReceipt(ClaimPool claimPool) throws InterruptedException {
        while (true) {
            Optional<Receipt> receiptOp = cfx.getTransactionReceipt(claimPool.txnHash).sendAndGet();
            if (!receiptOp.isPresent()) {
                Utils.sleep(2_000);
                continue;
            }
            Receipt receipt = receiptOp.get();
            log.info("tx {} status {}", receipt.getTransactionHash(), receipt.getOutcomeStatus());
            String comment = "";
            if (receipt.getOutcomeStatus() == 0) {
                log.info("tx succeeded");
                comment = "OK";
                String rpcRet = Utils.getRpc(restTemplate, this.bridgeHost
                        + "/moveToHistory?id=" + claimPool.id
                        + "&status=" + receipt.getOutcomeStatus()
                        + "&comment=" + comment, String.class);
                log.info("moveToHistory {}", rpcRet);
                break;
            } else {
                log.info("tx failed {}", receipt.getTransactionHash());
                Utils.sleep(15_000);
            }
        }
    }

    private void sendClaimTx(ClaimPool claimPool) throws Exception {
        String url = bridgeHost + "/buildCrossRequest?id=" + claimPool.crossInfoId;
        log.info("fetch claim info {}", url);
        ReportCrossRequest claimObj = Utils.getRpc(restTemplate, url, ReportCrossRequest.class);
        CrossInfo info = claimObj.infos.get(0);
        List<CrossItem> crossItems = claimObj.items.get(0);

        String fnData = Abi.encodeClaimByAdmin(
                Integer.parseInt(info.chainId),
                info.asset,
                info.targetContract,
                crossItems.stream().map(i -> i.tokenId).toArray(String[]::new),
                crossItems.stream().map(i -> i.amount).toArray(String[]::new),
                crossItems.stream().map(i -> i.uri).toArray(String[]::new),
                info.from,
                info.userNonce
        );

        String hexTo = sdk.Address.decode(vaultAddress);
        BigInteger nonce = account.getNonce();
        BigInteger remoteNonce = cfx.getNonce(account.getAddress()).sendAndGet();
        if (!remoteNonce.equals(nonce)) {
            log.warn("remoteNonce {} local nonce {} , use remote one", remoteNonce, nonce);
            account.setNonce(remoteNonce);
            nonce = remoteNonce;
        }
        log.info("tx nonce {} , call to {}", nonce, hexTo);

        BigInteger gasLimit = BigInteger.valueOf(14_000_000);
        BigInteger epoch = cfx.getEpochNumber().sendAndGet();
        RawTransaction cfxTx = RawTransaction.call(nonce, gasLimit, hexTo, BigInteger.ZERO, epoch, fnData);
        String signedTx = account.sign(cfxTx);
        String txHash = Hash.sha3(signedTx);
        log.info("claim tx hash {} user nonce {}", txHash, info.userNonce);

        SendTransactionResult result = account.send(signedTx);
        if (result.getRawError() == null) {
            updateClaimPool(claimPool, txHash, nonce);
            return;
        }
        log.info("send tx result, error type {}, raw error {}", result.getErrorType(), result.getRawError());
        throw new RuntimeException("send claim tx fail");
    }

    private void updateClaimPool(ClaimPool claimPool, String claimTxHash, BigInteger nonce) {
        String url = this.bridgeHost
                + "/updateClaimPool?id=" + claimPool.id
                + "&claimTxHash=" + claimTxHash
                + "&txFrom=" + account.getAddress()
                + "&nonce=" + nonce.toString();
        log.info("url {}", url);
        String info = Utils.getRpc(restTemplate, url, String.class);
        log.info("updateClaimPool result {}", info);
        if ("OK".equals(info)) {
            return;
        }
        throw new RuntimeException("updateClaimPool fail");
    }
}

