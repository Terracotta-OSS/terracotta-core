/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.runtime;

/**
 * Thrown when unable to parse the runtime version information.
 */
public final class UnknownRuntimeVersionException extends Exception {
  UnknownRuntimeVersionException(final String jvmVersion, final String badVersion) {
    super("Unable to parse runtime version '" + badVersion + "' for JVM version '" + jvmVersion + "'");
  }
}
