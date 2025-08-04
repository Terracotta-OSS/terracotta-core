/*
 * Copyright IBM Corp. 2025
 */
package com.tc.l2.logging;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

public class SecurityLogger implements Logger {

    private final String name;
    static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_LOGGER");

    public SecurityLogger(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Logger name cannot be null");
        }
        this.name = name;
    }

    private void logWithName(String message, Consumer<String> consumer) {
        consumer.accept( name + " - " + message);
    }

    private <T> void logWithName(String message, T arg, BiConsumer<String, T> biConsumer) {
        biConsumer.accept(name + " - " + message, arg);
    }

    private void logWithName(BiConsumer<String, Object[]> biConsumer, String format, Object... arguments) {
        biConsumer.accept(name + " - " + format, arguments);
    }

    private void logWithName(String format, Object arg1, Object arg2, TriConsumer<String, Object, Object> triConsumer) {
        triConsumer.accept(name + " - " + format, arg1, arg2);
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return securityLogger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logWithName(msg, securityLogger::trace);
    }

    @Override
    public void trace(String format, Object arg) {
        logWithName(format, arg, securityLogger::trace);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logWithName(format, arg1, arg1, securityLogger::trace);
    }

    @Override
    public void trace(String format, Object... arguments) {
        logWithName(securityLogger::trace, format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logWithName(msg, t, securityLogger::trace);
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
        return securityLogger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logWithName(msg, securityLogger::debug);
    }

    @Override
    public void debug(String format, Object arg) {
        logWithName(format, arg, securityLogger::debug);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logWithName(format, arg1, arg2, securityLogger::debug);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logWithName(securityLogger::debug, format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logWithName(msg, t, securityLogger::debug);
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
        return securityLogger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logWithName(msg, securityLogger::info);
    }

    @Override
    public void info(String format, Object arg) {
        logWithName(format, arg, securityLogger::info);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logWithName(format, arg1, arg2, securityLogger::info);
    }

    @Override
    public void info(String format, Object... arguments) {
        logWithName(securityLogger::info, format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        logWithName(msg, t, securityLogger::info);
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
        return securityLogger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logWithName(msg, securityLogger::warn);
    }

    @Override
    public void warn(String format, Object arg) {
        logWithName(format, arg, securityLogger::warn);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logWithName(securityLogger::warn, format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logWithName(format, arg1, arg2, securityLogger::warn);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logWithName(msg, t, securityLogger::warn);
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
        return securityLogger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logWithName(msg, securityLogger::error);
    }

    @Override
    public void error(String format, Object arg) {
        logWithName(format, arg, securityLogger::error);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logWithName(format, arg1, arg2, securityLogger::error);
    }

    @Override
    public void error(String format, Object... arguments) {
        logWithName(securityLogger::error, format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        logWithName(msg, t, securityLogger::error);
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
