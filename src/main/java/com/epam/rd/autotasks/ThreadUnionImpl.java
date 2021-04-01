package com.epam.rd.autotasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ThreadUnionImpl implements ThreadUnion {
    private final List<Thread> threads = new ArrayList<>();
    private final String name;
    private final AtomicInteger integer = new AtomicInteger(0);
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

    }

    @Override
    public boolean isFinished() {
        return isShutdown() && activeSize() == 0;
    }

    @Override
    public List<FinishedThreadResult> results() {
        synchronized (threads) {
            return threads.stream()
                    .filter(thread1 -> !thread1.isAlive())
                    .map(thread1 -> new FinishedThreadResult(thread1.getName()))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Thread newThread(Runnable r) {
        if (!isShutdown()) {
            Thread thread = new Thread(r, name + "-worker-" + integer.getAndIncrement());
            threads.add(thread);
            return thread;
        }
        throw new IllegalStateException();
    }
}