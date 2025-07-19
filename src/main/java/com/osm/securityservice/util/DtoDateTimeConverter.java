package com.osm.securityservice.util;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DtoDateTimeConverter {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static Object convertDateTimes(Object obj) {
        return convertDateTimes(obj, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static Object convertDateTimes(Object obj, Set<Object> visited) {
        if (obj == null) return null;
        if (obj instanceof LocalDateTime ldt) {
            return ldt.format(formatter);
        }
        if (obj instanceof Collection<?> col) {
            Collection<Object> result;
            if (col instanceof List) {
                result = new ArrayList<>();
            } else if (col instanceof Set) {
                result = new HashSet<>();
            } else {
                result = new ArrayList<>();
            }
            for (Object item : col) {
                result.add(convertDateTimes(item, visited));
            }
            return result;
        }
        if (obj instanceof Map<?,?> map) {
            Map<Object, Object> result = new HashMap<>();
            for (var entry : map.entrySet()) {
                result.put(entry.getKey(), convertDateTimes(entry.getValue(), visited));
            }
            return result;
        }
        if (obj.getClass().getName().startsWith("java.")) {
            return obj;
        }
        if (visited.contains(obj)) {
            return obj;
        }
        visited.add(obj);
        Map<String, Object> result = new HashMap<>();
        Class<?> current = obj.getClass();
        while (current != null && !current.getName().startsWith("java.")) {
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    if (value instanceof LocalDateTime ldt) {
                        result.put(field.getName(), ldt.format(formatter));
                    } else if (value instanceof Collection<?> || value instanceof Map<?,?>) {
                        result.put(field.getName(), convertDateTimes(value, visited));
                    } else if (value != null && !value.getClass().getName().startsWith("java.")) {
                        result.put(field.getName(), convertDateTimes(value, visited));
                    } else {
                        result.put(field.getName(), value);
                    }
                } catch (IllegalAccessException ignored) {}
            }
            current = current.getSuperclass();
        }
        return result;
    }
} 