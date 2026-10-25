package com.Lilith.immersivepaintings.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Queues runnables onto the main client or server thread from network threads. */
public final class ScheduledTasks {

    private static final List<Runnable> SERVER_TASKS = Collections.synchronizedList(new ArrayList<Runnable>());
    private static final List<Runnable> CLIENT_TASKS = Collections.synchronizedList(new ArrayList<Runnable>());

    private ScheduledTasks() {}

    public static void server(Runnable runnable) {
        SERVER_TASKS.add(runnable);
    }

    public static void client(Runnable runnable) {
        CLIENT_TASKS.add(runnable);
    }

    public static void tickServer() {
        drain(SERVER_TASKS);
    }

    public static void tickClient() {
        drain(CLIENT_TASKS);
    }

    private static void drain(List<Runnable> tasks) {
        Runnable[] snapshot;
        synchronized (tasks) {
            if (tasks.isEmpty()) return;
            snapshot = tasks.toArray(new Runnable[0]);
            tasks.clear();
        }
        for (Runnable task : snapshot) {
            try {
                task.run();
            } catch (Throwable t) {
                com.Lilith.immersivepaintings.Main.LOGGER.error("scheduled task failed", t);
            }
        }
    }
}
