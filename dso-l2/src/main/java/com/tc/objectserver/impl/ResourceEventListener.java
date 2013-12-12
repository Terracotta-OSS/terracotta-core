/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
