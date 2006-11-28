/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.api;

import java.util.Map;

public interface ObjectInstanceMonitor {

  void instanceCreated(String type);

  void instanceDestroyed(String type);

  Map getInstanceCounts();

}
