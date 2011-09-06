/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.exception.TCRuntimeException;
import com.tc.object.bytecode.ManagerUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DsoNodeImpl implements DsoNodeInternal, Comparable {

  private final String             id;
  private final long               channelId;
  private final boolean            isLocalNode;

  private volatile DsoNodeMetaData metaData;

  public DsoNodeImpl(final String id, final long channelId) {
    this(id, channelId, false);
  }

  public DsoNodeImpl(final String id, final long channelId, boolean isLocalNode) {
    this.id = id;
    this.channelId = channelId;
    this.isLocalNode = isLocalNode;
  }

  public String getId() {
    return id;
  }

  public long getChannelId() {
    return channelId;
  }

  public String getIp() {
    return getOrRetrieveMetaData().getIp();
  }

  public String getHostname() {
    return getOrRetrieveMetaData().getHostname();
  }

  private boolean isLocalNode() {
    return this.isLocalNode;
  }

  private DsoNodeMetaData resolveLocalIPAndHostname() {
    InetAddress addr;
    try {
      addr = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new TCRuntimeException(e);
    }
    return new DsoNodeMetaData(addr.getHostAddress(), addr.getHostName());
  }

  private DsoNodeMetaData getOrRetrieveMetaData() {
    if (metaData == null) {
      if (isLocalNode()) {
        metaData = resolveLocalIPAndHostname();
      } else {
        // Doing this through the manager API to not have to keep a reference
        // to the cluster instance in the node instance. This makes it easier to
        // share DsoNodeImpl instances.
        metaData = getOrRetrieveMetaData(((DsoClusterInternal) ManagerUtil.getManager().getDsoCluster()));
      }
    }
    return metaData;
  }

  public DsoNodeMetaData getOrRetrieveMetaData(DsoClusterInternal cluster) {
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

  public int compareTo(final Object o) {
    return id.compareTo(((DsoNodeImpl) o).id);
  }
}
