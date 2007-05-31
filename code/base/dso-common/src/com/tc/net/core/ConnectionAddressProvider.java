/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.util.StringUtil;

public class ConnectionAddressProvider {

  private final ConnectionInfo[] addresses;

  public ConnectionAddressProvider(ConfigItem source) {
    this((ConnectionInfo[]) source.getObject());
  }

  public ConnectionAddressProvider(ConnectionInfo[] addresses) {
    this.addresses = (addresses == null) ? ConnectionInfo.EMPTY_ARRAY : addresses;
  }

  public synchronized String toString() {
    return "ConnectionAddressProvider(" + StringUtil.toString(addresses) + ")";
  }

  public synchronized ConnectionAddressIterator getIterator() {
    return new ConnectionAddressIterator(addresses);
  }
}