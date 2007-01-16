package com.tc.capabilities;

import java.util.Date;

public class StandardCapabilitiesImpl implements Capabilities {
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
}
