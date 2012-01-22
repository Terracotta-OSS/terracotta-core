/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
