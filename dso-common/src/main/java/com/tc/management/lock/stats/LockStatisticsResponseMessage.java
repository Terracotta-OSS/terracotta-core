/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.net.protocol.tcm.TCMessage;

import java.util.Collection;

public interface LockStatisticsResponseMessage extends TCMessage {
  // This interface exists to remove cyclic dependency between modules dso-common and management

  void initialize(Collection allTCLockStatElements);
}
