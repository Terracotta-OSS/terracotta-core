/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

/**
 *
 * @author mscott
 */
public class ClientNotFoundException extends Exception {

    /**
     * Creates a new instance of
     * <code>ClientNotFoundException</code> without detail message.
     */
    public ClientNotFoundException() {
    }

    /**
     * Constructs an instance of
     * <code>ClientNotFoundException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public ClientNotFoundException(String msg) {
        super(msg);
    }
}
