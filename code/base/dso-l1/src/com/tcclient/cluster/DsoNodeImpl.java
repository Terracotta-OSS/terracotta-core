/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.object.bytecode.ManagerUtil;

public class DsoNodeImpl implements DsoNodeInternal, Comparable {

  private final String             id;
  private final long               channelId;

  private volatile DsoNodeMetaData metaData;

  public DsoNodeImpl(final String id, final long channelId) {
    this.id = id;
    this.channelId = channelId;
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

  private DsoNodeMetaData getOrRetrieveMetaData() {
    if (metaData != null) { return metaData; }
    return ((DsoClusterInternal) ManagerUtil.getManager().getDsoCluster()).retrieveMetaDataForDsoNode(this);
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
    return id.compareTo(((DsoNodeImpl)o).id);
  }
}
