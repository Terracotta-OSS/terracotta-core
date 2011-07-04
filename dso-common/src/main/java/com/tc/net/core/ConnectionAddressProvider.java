/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.util.StringUtil;

public class ConnectionAddressProvider implements ClusterTopologyChangedListener {

  private volatile ConnectionInfo[] addresses;

  public ConnectionAddressProvider(ConnectionInfo[] addresses) {
    this.addresses = (addresses == null) ? ConnectionInfo.EMPTY_ARRAY : addresses;
  }

  public synchronized String toString() {
    return "ConnectionAddressProvider(" + StringUtil.toString(addresses) + ")";
  }

  public synchronized ConnectionAddressIterator getIterator() {
    return new ConnectionAddressIterator(addresses);
  }
  
  public synchronized void serversUpdated(ConnectionAddressProvider... addressProviders) {
    for(ConnectionAddressProvider cap: addressProviders) {
      if(cap.getGroupId() == this.getGroupId()) {
        this.addresses = cap.addresses;
      }
    }
  }

  public int getGroupId() {
    if (addresses == null || addresses[0] == null) {
      synchronized (this) {
        if (addresses == null || addresses[0] == null) { return -1; }
      }
    }
    return addresses[0].getGroupId();
  }
}