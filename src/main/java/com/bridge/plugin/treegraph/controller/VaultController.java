package com.bridge.plugin.treegraph.controller;

import com.bridge.plugin.treegraph.Utils;
import com.bridge.plugin.treegraph.model.RegisterRouteVO;
import com.bridge.plugin.treegraph.model.ResponseVO;
import com.bridge.plugin.treegraph.service.ConsortiumPlugin;
import conflux.web3j.types.SendTransactionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class VaultController {
    Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    ConsortiumPlugin plugin;

    @PostMapping("/registerArrival")
    public Object registerArrival(@RequestBody RegisterRouteVO req) throws Exception {
        log.info("registerArrival req : {}", Utils.toJson(req));
        SendTransactionResult txRet = plugin.registerArrival(req);
        log.info("registerArrival result : {}", Utils.toJson(txRet));
        return ResponseVO.ok(txRet);
    }

    @PostMapping("/registerDeparture")
    public Object registerDeparture(@RequestBody RegisterRouteVO req) throws Exception {
        log.info("RegisterDeparture req : {}", Utils.toJson(req));
        SendTransactionResult txRet = plugin.registerDeparture(req);
        log.info("RegisterDeparture result : {}", Utils.toJson(txRet));
        return ResponseVO.ok(txRet);
    }

    @GetMapping("/receipt")
    public Object receipt(@RequestParam String txHash) {
        return ResponseVO.ok(plugin.cfx.getTransactionReceipt(txHash).sendAndGet());
    }
}

