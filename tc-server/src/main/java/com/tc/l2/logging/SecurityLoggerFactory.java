/*
 * Copyright IBM Corp. 2025
 */
package com.tc.l2.logging;

import org.slf4j.Logger;
import com.tc.logging.TCLogging;

public class SecurityLoggerFactory {

    private static boolean isSecurityLogEnabled;

    public static Logger getLogger(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException("Class cannot be null");
        }
        return getLogger(clazz.getName());
    }

    public static Logger getLogger(String name) {
        if (name == null) {
            throw new NullPointerException("Logger name cannot be null");
        }
        return isSecurityLogEnabled ? new SecurityLogger(name) : TCLogging.getSilentLogger();
    }

    // exposing this method for testing purpose only
    static void setIsSecurityLogEnabled(boolean value) {
        isSecurityLogEnabled = value;
    }

}
