/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.NodeID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TxnBatchID;

import java.io.IOException;

public interface TransactionBatchReader {

  public ServerTransaction getNextTransaction() throws IOException;

  public TxnBatchID getBatchID();

  public int getNumberForTxns();

  public NodeID getNodeID();

  public TCByteBuffer[] getBackingBuffers();

  /**
   * @return the backing byte buffer that contains transactions from "from" to "to" inclusive.
   */
  public TCByteBuffer[] getBackingBuffers(ServerTransactionID from, ServerTransactionID to);

  public ObjectStringSerializer getSerializer();

  public boolean containsSyncWriteTransaction();
}
