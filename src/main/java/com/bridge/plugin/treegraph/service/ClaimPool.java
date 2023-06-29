package com.bridge.plugin.treegraph.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClaimPool {
    public Long id;
    @JsonProperty("cross_info_id")
    public Long crossInfoId;

    @JsonProperty("target_chain_id")
    public Long targetChain;

    @JsonProperty("target_contract")
    public String targetContract;

    @JsonProperty("txn_hash")
    public String txnHash;

    public String from;
    public Long nonce;
    public String step;
    public Integer status;

}
