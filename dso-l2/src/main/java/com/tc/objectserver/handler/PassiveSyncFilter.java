/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.l2.msg.ReplicationMessage;

/**
 *
 */
public interface PassiveSyncFilter {
  boolean filter(ReplicationMessage message);
}
