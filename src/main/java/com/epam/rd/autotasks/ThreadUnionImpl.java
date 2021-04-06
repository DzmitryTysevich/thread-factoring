package com.epam.rd.autotasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadUnionImpl implements ThreadUnion {
    private final List<Thread> threads = Collections.synchronizedList(new ArrayList<>());
    private final List<FinishedThreadResult> results = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger counter = new AtomicInteger(0);
    private final String name;
    private final String WORKER = "-worker-";
    private boolean shutdown;

    public ThreadUnionImpl(String name) {
        this.name = name;
    }

    @Override
    public int totalSize() {
        return threads.size();
    }

    @Override
    public int activeSize() {
        return (int) threads.stream()
                .filter(Thread::isAlive)
                .count();
    }

    @Override
    public void shutdown() {
        shutdown = true;
        threads.forEach(Thread::interrupt);
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public void awaitTermination() {
        synchronized (threads) {
            threads.forEach(thread -> {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public boolean isFinished() {
        return isShutdown() && activeSize() == 0;
    }

    @Override
    public List<FinishedThreadResult> results() {
        return results;
    }

    @Override
    public Thread newThread(Runnable r) {
        if (isShutdown()) {
            throw new IllegalStateException();
        }
        Thread thread = getThread(r);
        thread.setUncaughtExceptionHandler((t, e) -> results.add(new FinishedThreadResult(t.getName(), e)));
        threads.add(thread);
        return thread;
    }

    private Thread getThread(Runnable r) {
        return new Thread(r, name + WORKER + counter.getAndIncrement()) {
            @Override
            public void run() {
                super.run();
                results.add(new FinishedThreadResult(this.getName()));
            }
        };
    }
}