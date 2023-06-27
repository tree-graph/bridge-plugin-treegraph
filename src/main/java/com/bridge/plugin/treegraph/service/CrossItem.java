package com.bridge.plugin.treegraph.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CrossItem {
    @JsonProperty("token_id")
    public String tokenId;
    public String amount;
    public String uri;
}
