/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.runtime;

/**
 * Thrown when unable to parse the JVM version information.
 */
public final class UnknownJvmVersionException extends Exception {
  UnknownJvmVersionException(final String badVersion) {
    super("Unable to parse JVM version '" + badVersion + "'");
  }
}