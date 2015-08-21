/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cluster;

public class NodeImpl implements NodeInternal, Comparable<NodeImpl> {

  private final String             id;
  private final long               channelId;

  public NodeImpl(String id, long channelId) {
    this.id = id;
    this.channelId = channelId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public long getChannelId() {
    return channelId;
  }

  @Override
  public String toString() {
    return id.toString();
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (null == obj) { return false; }
    if (getClass() != obj.getClass()) { return false; }
    NodeImpl other = (NodeImpl) obj;
    if (null == id) {
      return null == other.id;
    } else {
      return id.equals(other.id);
    }
  }

  @Override
  public int compareTo(NodeImpl other) {
    return id.compareTo(other.id);
  }
}
