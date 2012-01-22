/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.management.TerracottaMBean;

import java.util.SortedSet;

public interface ObjectManagementMonitorMBean extends TerracottaMBean {

  boolean runGC();

  boolean isGCRunning();

  boolean isGCStarted();

  SortedSet getAllObjectIds();
}
