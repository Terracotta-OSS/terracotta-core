/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.L2Info;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.beans.TCServerInfoMBean;

import javax.management.NotCompliantMBeanException;

public class NullTCServerInfo extends AbstractTerracottaMBean implements TCServerInfoMBean {

  public NullTCServerInfo() throws NotCompliantMBeanException {
    super(TCServerInfoMBean.class, false);
  }
  
  public void reset() {
    // nothing to reset
  }

  public long getActivateTime() {
    return 0;
  }

  public String getBuildID() {
    return "";
  }

  public String getCopyright() {
    return "";
  }

  public String getDescriptionOfCapabilities() {
    return "";
  }
  
  public L2Info[] getL2Info() {
    return null;
  }

  public long getStartTime() {
    return 0;
  }

  public String getVersion() {
    return "";
  }

  public boolean isActive() {
    return false;
  }

  public boolean isStarted() {
    return false;
  }

  public void shutdown() {
    //
  }

  public void stop() {
    //
  }

  public String getHealthStatus() {
    return "";
  }

}
