/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

public class ConflictingModuleException extends RuntimeException {
  private final String v1;
  private final String symName;
  private final String v2;

  public ConflictingModuleException(String symName, String v1, String v2) {
    super(makeMessage(symName, v1, v2));
    this.symName = symName;
    this.v1 = v1;
    this.v2 = v2;
  }

  public String getSymbolicName() {
    return symName;
  }

  public String getV1() {
    return v1;
  }

  public String getV2() {
    return v2;
  }

  private static String makeMessage(String symName, String v1, String v2) {
    return "Conflicting versions of " + symName + " required. Versions are [" + v1 + "] and [" + v2 + "]";
  }
}
