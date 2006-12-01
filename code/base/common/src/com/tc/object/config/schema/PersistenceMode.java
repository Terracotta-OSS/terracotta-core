/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.util.Assert;

/**
 * Represents the persistence mode of the server.
 */
public class PersistenceMode {

  private final String mode;

  private PersistenceMode(String mode) {
    Assert.assertNotBlank(mode);

    this.mode = mode;
  }

  public static final PersistenceMode TEMPORARY_SWAP_ONLY = new PersistenceMode("temporary-swap-only");
  public static final PersistenceMode PERMANENT_STORE     = new PersistenceMode("permanent-store");

  public boolean equals(Object that) {
    if (!(that instanceof PersistenceMode)) return false;
    return ((PersistenceMode) that).mode.equals(this.mode);
  }

  public String toString() {
    return this.mode;
  }

}
