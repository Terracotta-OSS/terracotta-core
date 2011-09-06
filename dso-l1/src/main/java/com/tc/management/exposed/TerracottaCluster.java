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
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    for (StackTraceElement element : stackTrace) {
      final String className = element.getClassName();
      // Only log the unsupported operation exception if a user application tried to
      // add a notification listener.
      // We can't do this all the time since the standard JMX server adds a notification listener
      // for all MBeans that are registered, resulting in confusing log messages. The exception is
      // swallowed though, so it still makes sense to throw it.
      // The stack trace is captured through the current thread static method to ensure that we have
      // the whole stack and that it isn't limited by the maximum stack trace depth of exceptions.
      if (!className.startsWith("com.tc.") &&
          !className.startsWith("com.sun.") &&
          !className.startsWith("java.") &&
          !className.startsWith("javax.")) {
        LOGGER.error(msg);
        break;
      }
    }
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
