/*******************************************************************************************
 * Copyright (c) Jonas Boner, Alexandre Vasseur. All rights reserved.                      *
 * http://backport175.codehaus.org                                                         *
 * --------------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of Apache License Version 2.0 *
 * a copy of which has been included with this distribution in the license.txt file.       *
 *******************************************************************************************/
package com.tc.backport175;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Thrown when error in compilation of the annotations.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Boner </a>
 */
public class ReaderException extends RuntimeException {
    /**
     * Original exception which caused this exception.
     */
    private Throwable m_originalException;

    /**
     * Sets the message for the exception.
     *
     * @param message the message
     */
    public ReaderException(final String message) {
        super(message);
    }

    /**
     * Sets the message for the exception and the original exception being wrapped.
     *
     * @param message   the detail of the error message
     * @param throwable the original exception
     */
    public ReaderException(String message, Throwable throwable) {
        super(message);
        m_originalException = throwable;
    }

    /**
     * Print the full stack trace, including the original exception.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Print the full stack trace, including the original exception.
     *
     * @param ps the byte stream in which to print the stack trace
     */
    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
        if (m_originalException != null) {
            m_originalException.printStackTrace(ps);
        }
    }

    /**
     * Print the full stack trace, including the original exception.
     *
     * @param pw the character stream in which to print the stack trace
     */
    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
        if (m_originalException != null) {
            m_originalException.printStackTrace(pw);
        }
    }
}
