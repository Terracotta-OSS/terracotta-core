/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

public interface DsoNode {
  public String getId();

  public String getIp();

  public String getHostname();
}