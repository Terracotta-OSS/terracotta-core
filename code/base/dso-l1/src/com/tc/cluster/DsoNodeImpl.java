/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tc.net.NodeID;
import com.tc.util.Assert;

public class DsoNodeImpl implements DsoNodeInternal {

  private final transient DsoClusterImpl cluster;

  private final NodeID                   id;

  private volatile DsoNodeMetaData       metaData;

  public DsoNodeImpl(final DsoClusterImpl cluster, final NodeID id) {
    Assert.assertNotNull(cluster);
    Assert.assertNotNull(id);
    this.cluster = cluster;
    this.id = id;
  }

  public String getId() {
    return id.toString();
  }

  public NodeID getNodeID() {
    return id;
  }

  public String getIp() {
    return getOrRetrieveMetaData().getIp();
  }

  public String getHostname() {
    return getOrRetrieveMetaData().getHostname();
  }

  private DsoNodeMetaData getOrRetrieveMetaData() {
    if (metaData != null) { return metaData; }
    return cluster.retrieveMetaDataForDsoNode(this);
  }

  public void setMetaData(final DsoNodeMetaData metaData) {
    this.metaData = metaData;
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
  public boolean equals(final Object obj) {
    if (this == obj) { return true; }
    if (null == obj) { return false; }
    if (getClass() != obj.getClass()) { return false; }
    DsoNodeImpl other = (DsoNodeImpl) obj;
    if (null == id) {
      return null == other.id;
    } else {
      return id.equals(other.id);
    }
  }
}
