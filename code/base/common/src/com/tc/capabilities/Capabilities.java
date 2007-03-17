package com.tc.capabilities;

import java.util.Date;

public interface Capabilities {
  int maxL2Connections();
  long maxL2RuntimeMillis();
  Date l2ExpiresOn();
  boolean canClusterPOJOs();
  boolean hasHA();
  boolean hasHAOverNetwork();
  String describe();
}
