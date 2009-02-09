/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.cluster;

public class DsoNodeImpl implements DsoNode {

  private final String id;

  public DsoNodeImpl(final String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public String getIp() {
    throw new UnsupportedOperationException();
  }

  public String getHostname() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return id;
  }
}
