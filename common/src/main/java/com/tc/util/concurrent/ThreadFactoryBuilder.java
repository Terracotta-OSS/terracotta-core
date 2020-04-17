/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 */
package com.tc.util.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author vmad
 */
public class ThreadFactoryBuilder {
    private ThreadFactory threadFactory;
    private String nameFormat;
    private Integer priority;
    private Boolean isDaemon;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public ThreadFactoryBuilder setThreadFactory(ThreadFactory threadFactory) {
        if(threadFactory == null) {
            throw new IllegalArgumentException("threadFactory should not be null");
        }
        this.threadFactory = threadFactory;
        return this;
    }

    public ThreadFactoryBuilder setNameFormat(String nameFormat) {
        if(nameFormat == null) {
            throw new IllegalArgumentException("nameFormat should not be null");
        }
        this.nameFormat = nameFormat;
        return this;
    }

    public ThreadFactoryBuilder setPriority(int priority) {
        if(priority >= Thread.MIN_PRIORITY && priority <= Thread.MAX_PRIORITY) {
            this.priority = priority;
        } else {
            throw new IllegalArgumentException("priority should be in range [" + Thread.MIN_PRIORITY + "," + Thread.MAX_PRIORITY + "]");
        }
        return this;
    }

    public ThreadFactoryBuilder setDaemon(boolean daemon) {
        isDaemon = daemon;
        return this;
    }

    public ThreadFactoryBuilder setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        if(uncaughtExceptionHandler == null) {
            throw new IllegalArgumentException("uncaughtExceptionHandler should not be null");
        }
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        return this;
    }

    public ThreadFactory build() {
        return build(this);
    }

    public static ThreadFactory build(ThreadFactoryBuilder builder) {
        final ThreadFactory backing = (builder.threadFactory != null) ?  builder.threadFactory : Executors.defaultThreadFactory();
        final String nameFormat = builder.nameFormat;
        final Integer priority = builder.priority;
        final Boolean isDaemon = builder.isDaemon;
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = builder.uncaughtExceptionHandler;
        final AtomicLong sequence = new AtomicLong(0);
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = backing.newThread(r);
                if(nameFormat != null) {
                    thread.setName(String.format(nameFormat, sequence.getAndIncrement()));
                }
                if(priority != null) {
                    thread.setPriority(priority);
                }
                if(isDaemon != null) {
                    thread.setDaemon(isDaemon);
                }
                if(uncaughtExceptionHandler != null) {
                    thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
                }
                return thread;
            }
        };
    }
 }
