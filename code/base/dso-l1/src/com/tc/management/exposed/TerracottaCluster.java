/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.exposed;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.AbstractTerracottaMBean;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

public class TerracottaCluster extends AbstractTerracottaMBean implements TerracottaClusterMBean {
  private static final TCLogger LOGGER = TCLogging.getLogger(TerracottaCluster.class);

  public TerracottaCluster() throws NotCompliantMBeanException {
    super(TerracottaClusterMBean.class, true);
  }

  private UnsupportedOperationException createUnsupportedOperationException() {
    final String msg = "JMX Cluster Events have been deprecated. You can now use DsoCluster instance injection and DsoClusterListener in plain Java from within your application.";
    LOGGER.error(msg);
    return new UnsupportedOperationException(msg);
  }

  public void reset() {
    // nothing to do
  }

  public String getNodeId() {
    throw createUnsupportedOperationException();
  }

  public String[] getNodesInCluster() {
    throw createUnsupportedOperationException();
  }

  public boolean isConnected() {
    throw createUnsupportedOperationException();
  }

  @Override
  public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter,
                                      final Object obj) {
    throw createUnsupportedOperationException();
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return new MBeanNotificationInfo[] {};
  }
}
