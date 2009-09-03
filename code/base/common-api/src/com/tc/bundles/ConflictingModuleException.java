/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

public class ConflictingModuleException extends RuntimeException {
  public ConflictingModuleException(String symName, String v1, String v2) {
    super(makeMessage(symName, v1, v2));
  }

  private static String makeMessage(String symName, String v1, String v2) {
    return "Conflicting versions of " + symName + " required. Versions are [" + v1 + "] and [" + v2 + "]";
  }
}
