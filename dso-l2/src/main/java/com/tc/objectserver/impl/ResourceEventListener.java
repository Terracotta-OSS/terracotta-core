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
package com.tc.objectserver.impl;

import org.terracotta.corestorage.monitoring.MonitoredResource;

/**
 *
 * @author mscott
 */
public interface ResourceEventListener {
    void resourcesConstrained(MonitoredResource usage);
    void resourcesFreed(MonitoredResource usage);
    void resourcesUsed(MonitoredResource usage);
    void requestEvictions(MonitoredResource usage);
    void cancelEvictions(MonitoredResource usage);
    void requestThrottle(MonitoredResource usage);
    void cancelThrottle(MonitoredResource usage);
    void requestStop(MonitoredResource usage);
    void cancelStop(MonitoredResource usage);
}
