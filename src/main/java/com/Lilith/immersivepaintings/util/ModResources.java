package com.Lilith.immersivepaintings.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.Lilith.immersivepaintings.Main;

/**
 * Reads bundled mod resources straight from the classpath so the same code works on the client, the
 * integrated server and the dedicated server (1.7.10 has no datapack system).
 */
public final class ModResources {

    private static final String BASE = "assets/" + Main.DOMAIN + "/";

    private ModResources() {}

    public static InputStream open(String relativePath) {
        return ModResources.class.getClassLoader()
            .getResourceAsStream(BASE + relativePath);
    }

    /** Lists all classpath entries below the given domain relative directory, e.g. "objects". */
    public static List<String> list(String directory) {
        String prefix = BASE + directory + "/";
        List<String> result = new ArrayList<>();
        ClassLoader loader = ModResources.class.getClassLoader();

        try {
            Enumeration<URL> urls = loader.getResources(prefix);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if ("file".equals(url.getProtocol())) {
                    collectFromDirectory(Paths.get(url.toURI()), prefix, result);
                } else if ("jar".equals(url.getProtocol())) {
                    collectFromJar(((JarURLConnection) url.openConnection()).getJarFile(), prefix, result);
                }
            }
        } catch (Exception e) {
            Main.LOGGER.error("failed listing resources below {}", prefix, e);
        }

        Collections.sort(result);
        return result;
    }

    private static void collectFromDirectory(Path root, String prefix, List<String> result) {
        if (!Files.isDirectory(root)) return;
        try (java.util.stream.Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .forEach(path -> {
                    String full = path.toAbsolutePath()
                        .normalize()
                        .toString()
                        .replace('\\', '/');
                    int index = full.indexOf(prefix);
                    if (index >= 0) result.add(full.substring(index + prefix.length()));
                });
        } catch (IOException e) {
            Main.LOGGER.error("failed walking resource directory {}", root, e);
        }
    }

    private static void collectFromJar(JarFile jar, String prefix, List<String> result) {
        try {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(prefix)) {
                    result.add(name.substring(prefix.length()));
                }
            }
        } finally {
            try {
                jar.close();
            } catch (IOException ignored) {
                // closing a cached jar is harmless
            }
        }
    }

    /** Absolute path of the game directory, used for the optional external painting pack. */
    public static File gameDirectory() {
        return new File(".").getAbsoluteFile();
    }

    public static InputStream openFile(File file) {
        try {
            return new FileInputStream(file);
        } catch (IOException e) {
            Main.LOGGER.error("failed opening {}", file, e);
            return null;
        }
    }
}
