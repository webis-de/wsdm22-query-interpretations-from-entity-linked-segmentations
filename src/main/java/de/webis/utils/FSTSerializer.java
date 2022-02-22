package de.webis.utils;

import org.nustaq.serialization.FSTConfiguration;

public class FSTSerializer implements Serializer {
    private static final FSTConfiguration serialConf = FSTConfiguration.createFastBinaryConfiguration();

    public static byte[] serialize(Object value) {
        return serialConf.asByteArray(value);
    }

    public static Object deserialize(byte[] value) {
        return serialConf.asObject(value);
    }
}
