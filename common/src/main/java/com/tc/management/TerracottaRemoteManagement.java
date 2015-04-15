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
