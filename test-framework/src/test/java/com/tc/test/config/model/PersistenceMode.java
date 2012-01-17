/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.config.model;

public enum PersistenceMode {
  PERMANENT_STORE("permanent-store"), TEMPORARY_SWAP_ONLY("temporary-swap-only");

  private final String mode;

  private PersistenceMode(String mode) {
    this.mode = mode;
  }

  public String getMode() {
    return mode;
  }
}
