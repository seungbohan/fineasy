package com.fineasy.external.kis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class KisResponseParser {

    private KisResponseParser() {

    }

    public static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : "";
    }

    public static BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public static long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getOutput(Map<String, Object> response) {
        Object output = response.get("output");
        if (output instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getOutput1(Map<String, Object> response) {
        Object output = response.get("output1");
        if (output instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getOutput2(Map<String, Object> response) {
        Object output = response.get("output2");
        if (output instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getOutputList(Map<String, Object> response) {
        Object output = response.get("output");
        if (output instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return Collections.emptyList();
    }
}
