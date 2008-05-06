/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

import java.util.Collections;
import java.util.Map;

public class NullObjectInstanceMonitor implements ObjectInstanceMonitor {

  public NullObjectInstanceMonitor() {
    //
  }

  public void instanceCreated(String type) {
    //
  }

  public void instanceDestroyed(String type) {
    //
  }

  public Map getInstanceCounts() {
    return Collections.EMPTY_MAP;
  }

}
