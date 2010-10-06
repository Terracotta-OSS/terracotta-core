/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.TxnBatchID;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class ServerTransactionBatchWriterTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testBasicServerEvictionTxn() throws Exception {
    final ObjectID oid = new ObjectID(55455);
    final NodeID nodeID = new ServerID("localhost", new byte[] { 5, 6, 4, 3 });
    final String className = "com.tc.state.ConcurrentDistributedServerMap";
    final String loaderDesc = "System.loader";
    final Map candidates = getCandidatesToEvict();
    final ServerTransactionFactory factory = new ServerTransactionFactory();
    final ObjectStringSerializer serializer = new ObjectStringSerializer();
    final ServerTransaction txn = factory.createServerMapEvictionTransactionFor(nodeID, oid, className, loaderDesc,
                                                                                candidates, serializer);
    final ServerTransactionBatchWriter txnWriter = new ServerTransactionBatchWriter(TxnBatchID.NULL_BATCH_ID, serializer);
    final TCByteBuffer[] buffer = txnWriter.writeTransactionBatch(Collections.singletonList(txn));

    final TransactionBatchReader reader = new TransactionBatchReaderImpl(buffer, nodeID, serializer,
                                                                         new ActiveServerTransactionFactory(), null);
    assertEquals(TxnBatchID.NULL_BATCH_ID, reader.getBatchID());
    assertEquals(1, reader.getNumberForTxns());
    assertEquals(false, reader.containsSyncWriteTransaction());

    final ServerTransaction txn2 = reader.getNextTransaction();
    assertTransactionsEqual(txn, txn2);
    final ServerTransaction txn3 = reader.getNextTransaction();
    assertNull(txn3);
  }

  private void assertTransactionsEqual(final ServerTransaction expected, final ServerTransaction actual) {
    assertEquals(expected.getNumApplicationTxn(), actual.getNumApplicationTxn());
    assertEquals(expected.getBatchID(), actual.getBatchID());
    assertEquals(expected.getClientSequenceID(), actual.getClientSequenceID());
    assertTrue(Arrays.equals(expected.getDmiDescriptors(), actual.getDmiDescriptors()));
    assertTrue(Arrays.equals(expected.getHighWaterMarks(), actual.getHighWaterMarks()));
    assertTrue(Arrays.equals(expected.getLockIDs(), actual.getLockIDs()));
    assertEquals(expected.getNewObjectIDs(), actual.getNewObjectIDs());
    assertEquals(expected.getNewRoots(), actual.getNewRoots());
    assertEquals(expected.getNotifies(), actual.getNotifies());
    assertEquals(expected.getObjectIDs(), actual.getObjectIDs());
    assertEquals(expected.getServerTransactionID(), actual.getServerTransactionID());
    assertEquals(expected.getSourceID(), actual.getSourceID());
    assertEquals(expected.getTransactionID(), actual.getTransactionID());
    assertEquals(expected.getTransactionType(), actual.getTransactionType());
  }

  private Map getCandidatesToEvict() {
    final Map c = new HashMap();
    for (int i = 0; i < 1000; i++) {
      c.put("key-" + i, new ObjectID(i * 20));
    }
    return c;
  }
}
