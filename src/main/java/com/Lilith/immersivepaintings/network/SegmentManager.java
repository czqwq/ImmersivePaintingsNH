package com.Lilith.immersivepaintings.network;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.Lilith.immersivepaintings.Main;
import com.Lilith.immersivepaintings.util.ImageManipulations;

/** Reassembles chunked image byte arrays. */
public class SegmentManager {

    private final Map<String, ByteArrayOutputStream> buffer = new HashMap<>();

    public synchronized Optional<BufferedImage> handleSegmentedPayload(String key, byte[] data, int segment,
        int totalSegments) {
        return handleSegmentedPayload(
            key,
            data,
            segment,
            totalSegments,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE);
    }

    public synchronized Optional<BufferedImage> handleSegmentedPayload(String key, byte[] data, int segment,
        int totalSegments, int maxBytes, int maxWidth, int maxHeight) {
        ByteArrayOutputStream byteBuffer = buffer.get(key);
        if (byteBuffer == null) {
            byteBuffer = new ByteArrayOutputStream();
            buffer.put(key, byteBuffer);
        }

        // The byte array is written in a single call, so the segment order must be preserved.
        if (segment < 0 || totalSegments < 1
            || segment >= totalSegments
            || byteBuffer.size() > maxBytes - data.length) {
            buffer.remove(key);
            return Optional.empty();
        }

        try {
            byteBuffer.write(data);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        if (segment + 1 == totalSegments) {
            buffer.remove(key);
            try {
                return Optional.ofNullable(ImageManipulations.decode(byteBuffer.toByteArray(), maxWidth, maxHeight));
            } catch (IOException e) {
                Main.LOGGER.error("could not combine segmented payloads for {}", key, e);
            }
        }

        return Optional.empty();
    }

    public synchronized void clear(String key) {
        buffer.remove(key);
    }
}
