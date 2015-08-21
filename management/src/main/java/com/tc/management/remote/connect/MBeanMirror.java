/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.connect;

import javax.management.DynamicMBean;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

public interface MBeanMirror extends DynamicMBean {
  public MBeanServerConnection getMBeanServerConnection();

  public ObjectName getRemoteObjectName();

  public ObjectName getLocalObjectName();
}
