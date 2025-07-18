/*
 * Copyright IBM Corp. 2025
 */
package com.tc.l2.logging;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MDC;

public class SecurityLogger implements Logger {

    private final String name;
    static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("SECURITY_LOGGER");

    public SecurityLogger(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Logger name cannot be null");
        }
        this.name = name;
    }

    private void logWithMDC(String message, Consumer<String> consumer) {
        try {
            MDC.put("SECURITY_LOGGER_NAME", name);
            consumer.accept(message);
        } finally {
            MDC.remove("SECURITY_LOGGER_NAME");
        }
    }

    private <T> void logWithMDC(String message, T arg, BiConsumer<String, T> biConsumer) {
        try {
            MDC.put("SECURITY_LOGGER_NAME", name);
            biConsumer.accept(message, arg);
        } finally {
            MDC.remove("SECURITY_LOGGER_NAME");
        }
    }

    private void logWithMDC(BiConsumer<String, Object[]> biConsumer, String format, Object... arguments) {
        try {
            MDC.put("SECURITY_LOGGER_NAME", name);
            biConsumer.accept(format, arguments);
        } finally {
            MDC.remove("SECURITY_LOGGER_NAME");
        }
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {

        void accept(T t, U u, V v);
    }

    private void logWithMDC(String format, Object arg1, Object arg2, TriConsumer<String, Object, Object> triConsumer) {
        try {
            MDC.put("SECURITY_LOGGER_NAME", name);
            triConsumer.accept(format, arg1, arg2);
        } finally {
            MDC.remove("SECURITY_LOGGER_NAME");
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return SECURITY_LOGGER.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logWithMDC(msg, SECURITY_LOGGER::trace);
    }

    @Override
    public void trace(String format, Object arg) {
        logWithMDC(format, arg, SECURITY_LOGGER::trace);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logWithMDC(format, arg1, arg1, SECURITY_LOGGER::trace);
    }

    @Override
    public void trace(String format, Object... arguments) {
        logWithMDC(SECURITY_LOGGER::trace, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logWithMDC(msg, t, SECURITY_LOGGER::trace);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void trace(Marker marker, String msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isDebugEnabled() {
        return SECURITY_LOGGER.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logWithMDC(msg, SECURITY_LOGGER::debug);
    }

    @Override
    public void debug(String format, Object arg) {
        logWithMDC(format, arg, SECURITY_LOGGER::debug);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logWithMDC(format, arg1, arg2, SECURITY_LOGGER::debug);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logWithMDC(SECURITY_LOGGER::debug, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logWithMDC(msg, t, SECURITY_LOGGER::debug);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void debug(Marker marker, String msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isInfoEnabled() {
        return SECURITY_LOGGER.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logWithMDC(msg, SECURITY_LOGGER::info);
    }

    @Override
    public void info(String format, Object arg) {
        logWithMDC(format, arg, SECURITY_LOGGER::info);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logWithMDC(format, arg1, arg2, SECURITY_LOGGER::info);
    }

    @Override
    public void info(String format, Object... arguments) {
        logWithMDC(SECURITY_LOGGER::info, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        logWithMDC(msg, t, SECURITY_LOGGER::info);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void info(Marker marker, String msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isWarnEnabled() {
        return SECURITY_LOGGER.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logWithMDC(msg, SECURITY_LOGGER::warn);
    }

    @Override
    public void warn(String format, Object arg) {
        logWithMDC(format, arg, SECURITY_LOGGER::warn);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logWithMDC(SECURITY_LOGGER::warn, format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logWithMDC(format, arg1, arg2, SECURITY_LOGGER::warn);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logWithMDC(msg, t, SECURITY_LOGGER::warn);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void warn(Marker marker, String msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isErrorEnabled() {
        return SECURITY_LOGGER.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logWithMDC(msg, SECURITY_LOGGER::error);
    }

    @Override
    public void error(String format, Object arg) {
        logWithMDC(format, arg, SECURITY_LOGGER::error);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logWithMDC(format, arg1, arg2, SECURITY_LOGGER::error);
    }

    @Override
    public void error(String format, Object... arguments) {
        logWithMDC(SECURITY_LOGGER::error, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        logWithMDC(msg, t, SECURITY_LOGGER::error);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void error(Marker marker, String msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
