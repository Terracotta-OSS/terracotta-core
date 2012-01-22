/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.objectserver.api.GCStats;

import java.util.EventListener;

public interface DGCListener extends EventListener {
  void statusUpdate(GCStats gcStats);
}
