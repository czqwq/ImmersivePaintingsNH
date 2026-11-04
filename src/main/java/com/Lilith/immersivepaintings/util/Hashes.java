package com.Lilith.immersivepaintings.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** MD5 helpers shared by the painting hashes and the local cache verification. */
public final class Hashes {

    private Hashes() {}

    /** MD5 is required by the Java platform, so a missing implementation is a hard error. */
    public static MessageDigest md5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available", e);
        }
    }

    public static String hex(MessageDigest digest) {
        return String.format("%032x", new BigInteger(1, digest.digest()));
    }

    public static String md5Hex(byte[] data) {
        MessageDigest md5 = md5();
        md5.update(data);
        return hex(md5);
    }
}
