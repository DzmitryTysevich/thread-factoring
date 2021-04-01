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

    private static void uncaughtException(Thread th, Throwable ex) {
        System.out.println("" + ex);
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
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public boolean isFinished() {
        return isShutdown() && activeSize() == 0;
    }

    @Override
    public List<FinishedThreadResult> results() {
        synchronized (threads) {
            List<FinishedThreadResult> s = threads.stream()
                    .filter(thread1 -> !thread1.isAlive())
                    .map(thread1 -> new FinishedThreadResult(thread1.getName()))
                    .collect(Collectors.toList());
            return s;
        }
    }

    @Override
    public Thread newThread(Runnable r) {
        if (!isShutdown()) {
            Thread.UncaughtExceptionHandler exceptionHandler = getUncaughtExceptionHandler();
            Thread thread = new Thread(r, name + "-worker-" + integer.getAndIncrement());
            thread.setUncaughtExceptionHandler(exceptionHandler);
            threads.add(thread);
            return thread;
        }
        throw new IllegalStateException();
    }

    private Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return ThreadUnionImpl::uncaughtException;
    }
}