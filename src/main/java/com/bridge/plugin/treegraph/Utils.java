package com.bridge.plugin.treegraph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class Utils {
    static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T toObj(Object from, Class<T> t) {
        try {
            return objectMapper.readValue(toJson(from), t);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getRpc(RestTemplate restTemplate, String url, Class<T> t) {
        HashMap map = restTemplate.getForObject(url, HashMap.class);
        if (!map.get("code").toString().equals("0")) {
            throw new RuntimeException("RPC failed:" + toJson(map));
        }
        return toObj(map.get("data"), t);
    }

    static HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent", "Application");
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public static void postRpc(RestTemplate restTemplate, String url, Object data) {
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(data, headers);

        ResponseEntity<HashMap> exchange = restTemplate.exchange(url, HttpMethod.POST, entity, HashMap.class);
        if (!exchange.hasBody()) {
            throw new RuntimeException("request failed, status code "+exchange.getStatusCode());
        }
        HashMap map = exchange.getBody();
        if (!map.get("code").toString().equals("0")) {
            throw new RuntimeException("RPC failed:" + toJson(map));
        }
    }

    static TimeZone tz = TimeZone.getTimeZone("UTC");
    static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
    public static String isoDate(Date dt) {
        df.setTimeZone(tz);
        String nowAsISO = df.format(dt);
        return nowAsISO;
    }
}
