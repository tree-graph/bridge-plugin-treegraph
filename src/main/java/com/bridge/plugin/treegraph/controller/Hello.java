package com.bridge.plugin.treegraph.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
public class Hello {
    @GetMapping("/")
    public Object hello() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("message", "hello 你好");
        return map;
    }
}
