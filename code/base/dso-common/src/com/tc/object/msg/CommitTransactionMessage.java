/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.lang.Recyclable;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.TransactionBatch;

public interface CommitTransactionMessage extends TCMessage, Recyclable {

  public ObjectStringSerializer getSerializer();

  public void setBatch(TransactionBatch batch, ObjectStringSerializer serializer);

  public TCByteBuffer[] getBatchData();

}
