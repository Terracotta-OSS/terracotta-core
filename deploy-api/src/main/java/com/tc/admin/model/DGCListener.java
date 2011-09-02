/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.model;

import com.tc.objectserver.api.GCStats;

import java.util.EventListener;

public interface DGCListener extends EventListener {
  void statusUpdate(GCStats gcStats);
}
