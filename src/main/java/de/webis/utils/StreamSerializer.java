package de.webis.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class StreamSerializer implements Serializer {
    public static byte[] serialize(Object value) {
        if (value instanceof String) {
            return ((String) value).getBytes(StandardCharsets.UTF_8);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;

        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(value);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    public static Object deserialize(byte[] value) {
        ByteArrayInputStream bis = new ByteArrayInputStream(value);
        ObjectInput in;
        try {
            in = new ObjectInputStream(bis);

            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {

            return new String(value, StandardCharsets.UTF_8);
//            e.printStackTrace();
        }

//        return null;
    }
}
