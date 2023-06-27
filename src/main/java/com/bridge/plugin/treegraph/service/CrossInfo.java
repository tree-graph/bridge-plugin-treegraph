package com.bridge.plugin.treegraph.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CrossInfo {
    @JsonProperty("chain_id")
    public String chainId;
    @JsonProperty("txn_hash")
    public String txnHash;
    public String asset;
    public String from;

    @JsonProperty("to_chain_id")
    public int toChainId;
    @JsonProperty("target_contract")
    public String targetContract;
    @JsonProperty("user_nonce")
    public int userNonce;
    @JsonProperty("block_number")
    public long blockNumber;
    @JsonProperty("block_time")
    public String blockTime;
}
