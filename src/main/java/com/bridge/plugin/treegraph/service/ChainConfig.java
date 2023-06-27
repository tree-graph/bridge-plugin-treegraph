package com.bridge.plugin.treegraph.service;

public class ChainConfig {
    public String id;
    public String name;
    public String vaultAddr;
    public String rpc;
    public String chainId;
    public String chainType;
    public Long delayBlock;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVaultAddr() {
        return vaultAddr;
    }

    public void setVaultAddr(String vaultAddr) {
        this.vaultAddr = vaultAddr;
    }

    public String getRpc() {
        return rpc;
    }

    public void setRpc(String rpc) {
        this.rpc = rpc;
    }

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getChainType() {
        return chainType;
    }

    public void setChainType(String chainType) {
        this.chainType = chainType;
    }

    public Long getDelayBlock() {
        return delayBlock;
    }

    public void setDelayBlock(Long delayBlock) {
        this.delayBlock = delayBlock;
    }
}
