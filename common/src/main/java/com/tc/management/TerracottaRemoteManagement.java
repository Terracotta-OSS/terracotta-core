/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

/**
 * Holder of the {@link RemoteManagement} instance.
 */
public class TerracottaRemoteManagement {

  private static final TCLogger LOGGER = TCLogging.getLogger(TerracottaRemoteManagement.class);

  private static volatile RemoteManagement remoteManagement;

  public static void setRemoteManagementInstance(RemoteManagement instance) {
    if (instance != null && remoteManagement != null) {
      throw new IllegalStateException("Instance already loaded");
    }
    remoteManagement = instance;
  }

  public static RemoteManagement getRemoteManagementInstance() {
    if (remoteManagement == null) {
      return new RemoteManagement() {
        @Override
        public void registerEventListener(ManagementEventListener listener) {
        }

        @Override
        public void unregisterEventListener(ManagementEventListener listener) {
        }

        @Override
        public void sendEvent(TCManagementEvent event) {
          LOGGER.warn("Trying to send a management event while the RemoteManagement instance was not set");
        }
      };
    }
    return remoteManagement;
  }

}
