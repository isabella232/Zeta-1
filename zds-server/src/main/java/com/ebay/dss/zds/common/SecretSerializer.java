package com.ebay.dss.zds.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * User to serializer sensitive text
 */
public class SecretSerializer extends JsonSerializer<String> {

    static final String SECRET_SUBSTITUTE = "***";

    /**
     * this hide text by a substitute
     *
     * @param value
     * @param gen
     * @param serializers
     * @throws IOException
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(StringUtils.isBlank(value) ? null : SECRET_SUBSTITUTE);
    }
}

