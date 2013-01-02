/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core;

import com.tc.util.StringUtil;

public class ConnectionAddressProvider implements ClusterTopologyChangedListener {

  private volatile ConnectionInfo[] addresses;

  public ConnectionAddressProvider(ConnectionInfo[] addresses) {
    this.addresses = (addresses == null) ? ConnectionInfo.EMPTY_ARRAY : addresses;
  }

  @Override
  public synchronized String toString() {
    return "ConnectionAddressProvider(" + StringUtil.toString(addresses) + ")";
  }

  public synchronized ConnectionAddressIterator getIterator() {
    return new ConnectionAddressIterator(addresses);
  }
  
  @Override
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

  public SecurityInfo getSecurityInfo() {
    SecurityInfo securityInfo = null;

    for (ConnectionInfo address : addresses) {
      if(securityInfo != null && !securityInfo.equals(address.getSecurityInfo())) {
        throw new IllegalStateException("Multiple SecurityInfo differ!");
      }
      securityInfo = address.getSecurityInfo();
    }

    return securityInfo;
  }
}