/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

/**
 *
 * @author mscott
 */
public interface ResourceEventListener {
    void resourcesUsed(DetailedMemoryUsage usage);
}
