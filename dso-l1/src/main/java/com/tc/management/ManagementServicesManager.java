/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
