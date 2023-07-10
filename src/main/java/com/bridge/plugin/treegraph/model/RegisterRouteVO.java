package com.bridge.plugin.treegraph.model;

import com.bridge.plugin.treegraph.blockchain.Abi;

public class RegisterRouteVO {
    public String remoteContract;
    public int remoteChainId;
    public Abi.OP op;
    public Abi.UriMode uriMode;
    public String localContract;
}
