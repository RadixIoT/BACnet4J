package com.serotonin.bacnet4j.type.primitive.encoding;

/**
 * To add a new encoder:
 * - create it as a subclass of CharacterEncoder
 * - add the class to resources/META-INF.services/com.serotonin.bacnet4j.type.primitive.encoding.CharacterEncoder
 */
public interface CharacterEncoder {
    boolean isEncodingSupported(CharacterEncoding encoding);

    byte[] encode(String value);

    String decode(byte[] bytes);
}
