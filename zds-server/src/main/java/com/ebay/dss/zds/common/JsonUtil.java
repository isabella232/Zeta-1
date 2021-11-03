package com.ebay.dss.zds.common;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by zhouhuang on Apr 9, 2018
 */
public class JsonUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtil.class);
    public static ObjectMapper MAPPER = new ObjectMapper();
    // this object is thread-safe
    public static Gson GSON = new Gson();
    public static JsonParser jsonParser = new JsonParser();

    public JsonUtil() {
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error convert Object to json. ", e);
            throw new ToolSetCheckException("Error convert Object to json");
        }
    }

    public static <T> T fromJson(String json, Class<T> cls) {
        try {
            return MAPPER.readValue(json, cls);
        } catch (IOException e) {
            LOGGER.error("Error read Object from json. ", e);
            throw new ToolSetCheckException("Error read Object from " + cls.getName());
        }
    }

    public static <T> T fromJson(String json, @SuppressWarnings("rawtypes") TypeReference<T> valueTypeRef) {
        try {
            return MAPPER.readValue(json, valueTypeRef);
        } catch (IOException e) {
            LOGGER.error("Error read Object from json. ", e);
            throw new ToolSetCheckException("Error read Object from json");
        }
    }

    public static Object mapToObject(Map<String, Object> map, Class<?> beanClass) {
        if (map == null)
            return null;

        try {
            Object obj = beanClass.newInstance();
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
                    continue;
                }

                field.setAccessible(true);
                field.set(obj, map.get(field.getName()));
            }
            return obj;
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Error convert map to object. ", e);
            throw new ToolSetCheckException("Error convert map to object");
        }

    }

}
