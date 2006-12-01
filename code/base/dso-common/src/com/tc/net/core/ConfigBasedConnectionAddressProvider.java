/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.core;

import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.ConfigItemListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ConfigBasedConnectionAddressProvider implements ConnectionAddressProvider, ConfigItemListener {

  private final ConfigItem source;

  private int              policy;
  private List             currentAddresses = new ArrayList();
  private int              current          = -1;

  public ConfigBasedConnectionAddressProvider(ConfigItem source) {
    this(ROUND_ROBIN, source);
  }

  private ConfigBasedConnectionAddressProvider(int policy, ConfigItem source) {
    this.policy = policy;
    this.source = source;

    this.source.addListener(this);
    this.currentAddresses.addAll(Arrays.asList((ConnectionInfo[]) source.getObject()));

    init();
  }

  private void init() {
    switch (policy) {
      case ROUND_ROBIN:
        if (currentAddresses.size() == 0) {
          current = -1;
        } else if (current < 0 || current >= currentAddresses.size()) {
          current = 0;
        }
        break;
      case LINEAR:
        if (currentAddresses.size() == 0) {
          current = -1;
        } else if (current < 0) {
          current = 0;
        }
        break;
      default:
        throw new AssertionError("Unimplemented policy for ConnectionAddressProvider !");
    }
  }

  public synchronized void valueChanged(Object oldValue, Object newValue) {
    ConnectionInfo currentServer;

    try {
      currentServer = getConnectionInfo();
    } catch (NoSuchElementException nsee) {
      currentServer = null;
    }

    ConnectionInfo[] newAddresses = (ConnectionInfo[]) this.source.getObject();
    this.currentAddresses.clear();
    this.currentAddresses.addAll(Arrays.asList(newAddresses));

    init();

    // Go find the first server in the new list that's the same as the server in the old list, and put our position
    // there. If we don't find one, the init() above will have simply made us start over anyway.
    Iterator iter = this.currentAddresses.iterator();
    int pos = 0;
    while (iter.hasNext()) {
      if (currentServer != null && iter.next().equals(currentServer)) {
        current = pos;
        break;
      }
      ++pos;
    }
  }

  private void reinit() {
    current = -1;
    init();
  }

  public String getHostname() {
    return getConnectionInfo().getHostname();
  }

  public int getPortNumber() {
    return getConnectionInfo().getPort();
  }

  public synchronized int getCount() {
    return currentAddresses.size();
  }

  public synchronized boolean hasNext() {
    switch (policy) {
      case ROUND_ROBIN:
        return currentAddresses.size() > 0;
      case LINEAR:
        return currentAddresses.size() > 0 && current < currentAddresses.size() - 1;
      default:
        return false;
    }
  }

  public synchronized ConnectionInfo getConnectionInfo() {
    if (current > -1 && current < currentAddresses.size()) { return (ConnectionInfo) currentAddresses.get(current); }
    throw new NoSuchElementException();
  }

  public synchronized ConnectionInfo next() {
    goNext();
    return getConnectionInfo();
  }

  private void goNext() {
    switch (policy) {
      case ROUND_ROBIN:
        if (currentAddresses.size() == 0) {
          current = -1;
        } else if (current < 0 || current >= currentAddresses.size() - 1) {
          current = 0;
        } else {
          current++;
        }
        break;
      case LINEAR:
        if (currentAddresses.size() == 0) {
          current = -1;
        } else if (current < currentAddresses.size()) {
          current++;
        }
        break;
      default:
        throw new AssertionError("Unimplemented policy for ConnectionAddressProvider !");
    }
  }

  public synchronized void setPolicy(int policy) {
    switch (policy) {
      case ROUND_ROBIN:
      case LINEAR:
        this.policy = policy;
        break;
      default:
        throw new AssertionError("Unimplemented policy for ConnectionAddressProvider !");
    }
    reinit();
  }

  public String toString() {
    return "ConnectionAddressProvider(current = " + current + ", policy = "
           + (policy == ROUND_ROBIN ? "ROUND_ROBIN" : "LINEAR") + ")[" + currentAddresses + "]";
  }
}
