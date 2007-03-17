package com.tc.capabilities;

import com.tc.properties.TCPropertiesImpl;

import java.util.Date;

public class StandardCapabilitiesImpl implements Capabilities {

  private static final boolean NETWORK_ENABLED_HA = TCPropertiesImpl.getProperties()
                                                      .getBoolean("l2.ha.network.enabled");

  public int maxL2Connections() {
    return Integer.MAX_VALUE;
  }

  public long maxL2RuntimeMillis() {
    return Integer.MAX_VALUE;
  }

  public Date l2ExpiresOn() {
    return new Date(Integer.MAX_VALUE);
  }

  public boolean canClusterPOJOs() {
    return true;
  }

  public boolean hasHA() {
    return true;
  }

  public String describe() {
    return "Unlimited capabilities";
  }

  public boolean hasHAOverNetwork() {
    return NETWORK_ENABLED_HA;
  }
}
