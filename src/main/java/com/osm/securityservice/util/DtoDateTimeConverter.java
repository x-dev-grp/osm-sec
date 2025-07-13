package com.osm.securityservice.util;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DtoDateTimeConverter {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static Object convertDateTimes(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Collection<?> col) {
            List<Object> result = new ArrayList<>();
            for (Object item : col) {
                result.add(convertDateTimes(item));
            }
            return result;
        }
        if (obj instanceof Map<?,?> map) {
            Map<Object, Object> result = new HashMap<>();
            for (var entry : map.entrySet()) {
                result.put(entry.getKey(), convertDateTimes(entry.getValue()));
            }
            return result;
        }
        if (obj instanceof LocalDateTime ldt) {
            return ldt.format(formatter);
        }
        if (obj.getClass().getName().startsWith("java.")) {
            return obj;
        }
        Map<String, Object> result = new HashMap<>();
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                result.put(field.getName(), convertDateTimes(value));
            } catch (IllegalAccessException ignored) {}
        }
        return result;
    }
} 