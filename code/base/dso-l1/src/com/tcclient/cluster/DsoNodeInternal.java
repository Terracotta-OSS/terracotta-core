/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cluster;

public interface DsoNodeInternal extends DsoNode {

  public long getChannelId();

  public void setMetaData(DsoNodeMetaData metaData);

}