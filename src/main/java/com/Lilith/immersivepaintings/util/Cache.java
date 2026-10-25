package com.Lilith.immersivepaintings.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.Lilith.immersivepaintings.Main;

/** Simple disk backed LRU cache, ported from upstream. */
public abstract class Cache<K, V> {

    public static final Path CACHE_PATH = Path.of(Main.MOD_ID + "_cache");

    private final Map<K, V> cache;

    public Cache() {
        this(200);
    }

    public Cache(int maxEntries) {
        this.cache = Collections.synchronizedMap(new LinkedHashMap<K, V>(maxEntries + 1, 0.75F, true) {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
                return size() > maxEntries;
            }
        });
    }

    public abstract String getCachePath(K key);

    public abstract V decode(byte[] bytes) throws IOException;

    public abstract byte[] encode(V key);

    private Path getPath(K key) {
        Path root = CACHE_PATH.toAbsolutePath()
            .normalize();
        Path path = root.resolve(getCachePath(key))
            .normalize();
        return path.startsWith(root) ? path : null;
    }

    private File getFile(K key) {
        Path path = getPath(key);
        return path != null && Files.exists(path) ? path.toFile() : null;
    }

    public boolean exists(K key) {
        return getFile(key) != null;
    }

    public Optional<V> get(K key) {
        return get(key, true);
    }

    public Optional<V> get(K key, boolean persistent) {
        if (cache.containsKey(key)) return Optional.of(cache.get(key));
        if (!persistent) return Optional.empty();

        File file = getFile(key);
        if (file == null) return Optional.empty();

        try (FileInputStream stream = new FileInputStream(file)) {
            V data = decode(readAll(stream));
            if (data == null) return Optional.empty();
            cache.put(key, data);
            return Optional.of(data);
        } catch (IOException e) {
            Main.LOGGER.error("failed getting cached file {}", file, e);
        }

        return Optional.empty();
    }

    public void set(K key, V value) {
        set(key, value, true);
    }

    public void set(K key, V value, boolean persistent) {
        if (!persistent) {
            cache.put(key, value);
            return;
        }

        Path path = getPath(key);
        if (path == null) return;

        if (!Files.exists(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                Main.LOGGER.error("failed creating directories for {}", path.getParent(), e);
                return;
            }
        }

        try (FileOutputStream outputStream = new FileOutputStream(path.toFile())) {
            byte[] encoded = encode(value);
            if (encoded != null) {
                outputStream.write(encoded);
                cache.put(key, value);
            }
        } catch (IOException e) {
            Main.LOGGER.error("failed writing cached file {}", path, e);
        }
    }

    public boolean delete(K key) {
        cache.remove(key);
        File file = getFile(key);
        if (file == null) return false;

        boolean deleted = file.delete();

        Path parent = file.toPath()
            .getParent();
        try (Stream<Path> entries = Files.list(parent)) {
            if (!entries.findAny()
                .isPresent()) Files.delete(parent);
        } catch (IOException e) {
            Main.LOGGER.error("failed deleting cache directory {}", parent, e);
        }

        return deleted;
    }

    /** Reads a stream into a byte array without relying on Java 9+ APIs. */
    public static byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
