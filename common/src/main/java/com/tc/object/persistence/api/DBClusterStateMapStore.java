/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.persistence.api;

public interface DBClusterStateMapStore {
  // for maintaining objectdb state in the persistent map
  public static final String DBKEY_STATE    = "DBKEY_STATE";
  public final String        DB_STATE_CLEAN = "CLEAN";
  public final String        DB_STATE_DIRTY = "DIRTY";
}
