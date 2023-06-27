package com.bridge.plugin.treegraph.service;

import java.util.List;

public class ReportCrossRequest {
    public String chainId;
    public long cursor;
    public long blockTime;

    public List<CrossInfo> infos;
    public List<List<CrossItem>> items;
}
