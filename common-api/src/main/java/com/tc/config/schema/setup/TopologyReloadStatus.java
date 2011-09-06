/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

public enum TopologyReloadStatus {
  TOPOLOGY_CHANGE_ACCEPTABLE, TOPOLOGY_CHANGE_UNACCEPTABLE, TOPOLOGY_UNCHANGED, SPECIFY_MIRROR_GROUPS, SERVER_STILL_ALIVE
}