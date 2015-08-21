/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

/**
 * @author Ludovic Orban
 */
public interface RemoteManagement {

  void registerEventListener(ManagementEventListener listener);

  void unregisterEventListener(ManagementEventListener listener);

  void sendEvent(TCManagementEvent event);

}
