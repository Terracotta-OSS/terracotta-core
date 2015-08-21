/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.object.management.RemoteCallDescriptor;
import com.tc.object.management.RemoteCallHolder;
import com.tc.object.management.ServiceID;

import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Local representation of the registered management services
 */
public interface ManagementServicesManager {

  // L2 -> L1

  void registerService(ServiceID serviceID, Object service, ExecutorService executorService);

  void unregisterService(ServiceID serviceID);

  Set<RemoteCallDescriptor> listServices(Set<ServiceID> serviceIDs, boolean includeCallDescriptors);

  void asyncCall(RemoteCallHolder remoteCallHolder, ResponseListener responseListener);


  // L1 -> L2

  void sendEvent(TCManagementEvent event);


  // common

  static interface ResponseListener {
    void onResponse(Object response, Exception exception);
  }

}
