/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.test.config.model;

public enum ServerPauseMode {

  ALL_ACTIVE,
 ANY_ACTIVE, ALL_GROUP_PASSIVE, // All passive servers will be paused for every group
  ANY_GROUP_PASSIVE; // All passive servers will be paused for single group

}
