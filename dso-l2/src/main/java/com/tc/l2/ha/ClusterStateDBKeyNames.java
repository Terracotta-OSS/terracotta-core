/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

public interface ClusterStateDBKeyNames {
  public static final String DATABASE_CREATION_TIMESTAMP_KEY = "BERKELEYDB::DB_CREATION_TIMESTAMP_KEY";
  public static final String L2_STATE_KEY                    = "CLUSTER_STATE::L2_STATE_KEY";
  public static final String CLUSTER_ID_KEY                  = "CLUSTER_STATE::CLUSTER_ID_KEY";
  public static final String STRIPE_KEY_PREFIX               = "STRIPE_STATE::KEY_PREFIX";
}
