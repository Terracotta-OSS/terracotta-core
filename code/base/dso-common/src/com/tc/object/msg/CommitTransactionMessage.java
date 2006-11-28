/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.TransactionBatch;

/**
 * @author steve
 */
public interface CommitTransactionMessage {

  public ObjectStringSerializer getSerializer();

  public void setBatch(TransactionBatch batch, ObjectStringSerializer serializer);

  public TCByteBuffer[] getBatchData();

  public void send();
}
