/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

public class MultipleServersPersistenceMode {
  public static final String PERMANENT_STORE     = "permanent-store";
  public static final String TEMPORARY_SWAP_ONLY = "temporary-swap-only";

  private final String       mode;

  public MultipleServersPersistenceMode(String mode) {
    if (!mode.equals(PERMANENT_STORE) && !mode.equals(TEMPORARY_SWAP_ONLY)) { throw new AssertionError(
                                                                                                       "Unrecognized persistence mode ["
                                                                                                           + mode + "]"); }
    this.mode = mode;
  }

  public String getMode() {
    if (mode == null) { throw new AssertionError("Mode was not set"); }
    return mode;
  }
}
