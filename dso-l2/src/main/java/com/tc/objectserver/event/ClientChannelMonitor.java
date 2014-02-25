/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.event;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;

public interface ClientChannelMonitor {

  void monitorClient(ClientID clientToBeMonitored, ObjectID subscriber);

}
