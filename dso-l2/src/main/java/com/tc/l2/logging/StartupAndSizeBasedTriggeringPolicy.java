package com.tc.l2.logging;

import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class StartupAndSizeBasedTriggeringPolicy<E> extends SizeBasedTriggeringPolicy<E> {
    private final AtomicBoolean firstTime = new AtomicBoolean();

    public boolean isTriggeringEvent(final File activeFile, final E event) {
        if (firstTime.compareAndSet(false, true) && activeFile != null && activeFile.length() > 0) {
            return true;
        }
        return super.isTriggeringEvent(activeFile, event);
    }
}