/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.beans.object.ObjectManagementMonitor.GCComptroller;

import javax.management.NotCompliantMBeanException;

public final class MockObjectManagementMonitor extends AbstractTerracottaMBean implements ObjectManagementMonitorMBean {

  public MockObjectManagementMonitor() throws NotCompliantMBeanException {
    super(ObjectManagementMonitorMBean.class, false);
  }

  public boolean isGCRunning() {
    return false;
  }

  public void registerGCController(GCComptroller controller) {
    // do nothing
  }

  public void runGC() {
    return;
  }

  public void reset() {
    // nothing to reset
  }

  public String getGCResult() {
    return "";
  }

}
