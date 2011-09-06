/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.dna.impl.ObjectStringSerializer;

import java.util.Collection;

/**
 * @author steve
 */
public interface RequestManagedObjectResponseMessage extends TCMessage {

  public Collection getObjects();
  
  public void initialize(TCByteBuffer[] dnas, int count, ObjectStringSerializer aSerializer, long bid, int tot);

  public ObjectStringSerializer getSerializer();

  public long getBatchID();

  public int getTotal();

  public void doRecycleOnRead();
}
