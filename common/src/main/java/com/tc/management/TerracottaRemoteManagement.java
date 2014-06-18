/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

/**
 * Holder of the {@link RemoteManagement} instance.
 */
public class TerracottaRemoteManagement {

  private static volatile RemoteManagement remoteManagement;

  public static void setRemoteManagementInstance(RemoteManagement instance) {
    if (instance != null && remoteManagement != null) {
      throw new IllegalStateException("Instance already loaded");
    }
    remoteManagement = instance;
  }

  public static RemoteManagement getRemoteManagementInstance() {
    return remoteManagement;
  }

}
