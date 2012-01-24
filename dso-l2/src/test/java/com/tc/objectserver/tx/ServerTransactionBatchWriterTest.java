/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.locks.LockID;
import com.tc.object.locks.StringLockID;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.metadata.NVPair;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.Assert;
import com.tc.util.SequenceID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class ServerTransactionBatchWriterTest extends TestCase {

  private TxnBatchID batchID;
  private int        startIndex;
  private int        txnID;
  private int        sqID;
  private NodeID     sourceNodeID;
  private int        dnaObjId;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.txnID = 100;
    this.sqID = 100;
    this.batchID = TxnBatchID.NULL_BATCH_ID;
    this.startIndex = 1;
    this.sourceNodeID = new ServerID("localhost", new byte[] { 5, 6, 4, 3 });
    this.dnaObjId = 100;
  }

  public void testBasicServerEvictionTxn() throws Exception {
    final ObjectID oid = new ObjectID(55455);
    final String className = "com.tc.state.ConcurrentDistributedServerMap";
    final Map candidates = getCandidatesToEvict();
    final ServerTransactionFactory factory = new ServerTransactionFactory();
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ServerTransaction txn = factory.createServerMapEvictionTransactionFor(sourceNodeID, oid, className,
                                                                                candidates, serializer, "foo");
    final ServerTransactionBatchWriter txnWriter = new ServerTransactionBatchWriter(TxnBatchID.NULL_BATCH_ID,
                                                                                    serializer);
    final TCByteBuffer[] buffer = txnWriter.writeTransactionBatch(Collections.singletonList(txn));

    final TransactionBatchReader reader = new TransactionBatchReaderImpl(buffer, sourceNodeID, serializer,
                                                                         new ActiveServerTransactionFactory(), null);
    assertEquals(TxnBatchID.NULL_BATCH_ID, reader.getBatchID());
    assertEquals(1, reader.getNumberForTxns());
    assertEquals(false, reader.containsSyncWriteTransaction());

    final ServerTransaction txn2 = reader.getNextTransaction();
    assertTransactionsEqual(txn, txn2);
    final ServerTransaction txn3 = reader.getNextTransaction();
    assertNull(txn3);
  }

  public void testServerEvictionAmongOtherTxn() throws Exception {
    final ObjectID oid = new ObjectID(55455);
    final NodeID nodeID = new ServerID("localhost", new byte[] { 5, 6, 4, 3 });
    final String className = "com.tc.state.ConcurrentDistributedServerMap";
    final Map candidates = getCandidatesToEvict();

    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final ServerTransactionBatchWriter txnWriter = new ServerTransactionBatchWriter(batchID, serializer);

    List<ServerTransaction> serverTransactions = new ArrayList<ServerTransaction>();

    // general server transactions
    List<ServerTransaction> txnList1 = createServerTransactions(5);
    serverTransactions.addAll(txnList1);

    // server map eviction txn
    final ServerTransactionFactory factory = new ServerTransactionFactory();
    final ServerTransaction serverMapEvictionTxn = factory.createServerMapEvictionTransactionFor(nodeID, oid,
                                                                                                 className, candidates,
                                                                                                 serializer, "foo");
    serverTransactions.add(serverMapEvictionTxn);

    // few more general server transactions
    List<ServerTransaction> txnList2 = createServerTransactions(5);
    serverTransactions.addAll(txnList2);

    // writer
    final TCByteBuffer[] buffer = txnWriter.writeTransactionBatch(serverTransactions);

    // reader
    final TransactionBatchReader reader = new TransactionBatchReaderImpl(buffer, nodeID, serializer,
                                                                         new ActiveServerTransactionFactory(), null);
    assertEquals(this.batchID, reader.getBatchID());
    assertEquals(11, reader.getNumberForTxns());
    assertEquals(false, reader.containsSyncWriteTransaction());

    for (ServerTransaction serverTxn : txnList1) {
      assertTransactionsEqual(serverTxn, reader.getNextTransaction());
    }

    final ServerTransaction txn2 = reader.getNextTransaction();
    assertTransactionsEqual(serverMapEvictionTxn, txn2);

    for (ServerTransaction serverTxn : txnList2) {
      assertTransactionsEqual(serverTxn, reader.getNextTransaction());
    }

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

    MetaDataReader[] expectedMetaDataReaders = expected.getMetaDataReaders();
    MetaDataReader[] actualMetaDataReaders = actual.getMetaDataReaders();

    assertEquals(expectedMetaDataReaders.length, actualMetaDataReaders.length);
    for (int i = 0; i < expectedMetaDataReaders.length; i++) {
      MetaDataReader expectedReader = expectedMetaDataReaders[i];
      MetaDataReader actualReader = actualMetaDataReaders[i];

      Iterator<MetaDataDescriptorInternal> expectedIter = expectedReader.iterator();
      Iterator<MetaDataDescriptorInternal> actualIter = actualReader.iterator();

      while (true) {
        MetaDataDescriptorInternal expectedMdd = expectedIter.next();
        MetaDataDescriptorInternal actualMdd = actualIter.next();

        Assert.assertEquals(expectedMdd.getCategory(), actualMdd.getCategory());
        Assert.assertEquals(expectedMdd.getObjectId(), actualMdd.getObjectId());
        Assert.assertEquals(expectedMdd.numberOfNvPairs(), actualMdd.numberOfNvPairs());

        Iterator<NVPair> expectedNvpairs = expectedMdd.getMetaDatas();
        Iterator<NVPair> actualNvpairs = actualMdd.getMetaDatas();

        while (true) {
          NVPair expectedNv = expectedNvpairs.next();
          NVPair actualNv = actualNvpairs.next();
          assertEquals(expectedNv, actualNv);

          if (!expectedNvpairs.hasNext()) {
            assertFalse(actualNvpairs.hasNext());
            break;
          }
        }

        if (!expectedIter.hasNext()) {
          assertFalse(actualIter.hasNext());
          break;
        }
      }
    }
  }

  private Map getCandidatesToEvict() {
    final Map c = new HashMap();
    for (int i = 0; i < 1000; i++) {
      c.put(new UTF8ByteDataHolder("key-" + i), new ObjectID(i * 20));
    }
    return c;
  }

  private List<ServerTransaction> createServerTransactions(int count) {
    List<ServerTransaction> serverTransactions = new ArrayList<ServerTransaction>();
    int actionCount = 3;
    while (count-- > 0) {
      int endIndex = this.startIndex + actionCount;
      serverTransactions.add(new ServerTransactionImpl(this.batchID, new TransactionID(this.txnID++),
                                                       new SequenceID(this.sqID++), createLocks(this.startIndex,
                                                                                                endIndex),
                                                       this.sourceNodeID, createDNAs(this.startIndex, endIndex),
                                                       new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                       TxnType.NORMAL, new LinkedList(), DmiDescriptor.EMPTY_ARRAY,
                                                       new MetaDataReader[0], 1, new long[0]));
      this.startIndex = endIndex + 1;
    }
    return serverTransactions;
  }

  private LockID[] createLocks(int s, int e) {
    LockID[] locks = new LockID[e - s + 1];
    for (int j = s; j <= e; j++) {
      locks[j - s] = new StringLockID("@" + j);
    }
    return locks;
  }

  private List createDNAs(int s, int e) {
    List dnas = new ArrayList();

    // delta dnas
    for (int i = s; i <= e; i++) {
      dnas.add(createDNA(new ObjectID(this.dnaObjId++), true));
    }

    // new dnas
    for (int i = s; i <= e; i++) {
      dnas.add(createDNA(new ObjectID(this.dnaObjId++), false));
    }

    return dnas;
  }

  private DNA createDNA(ObjectID objectID, boolean isDelta) {

    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    final ObjectStringSerializer objectStringSerializer = new ObjectStringSerializerImpl();
    final DNAWriter dnaWriter = new DNAWriterImpl(out, objectID, this.getClass().getName(), objectStringSerializer,
                                                  new SerializerDNAEncodingImpl(), isDelta);

    final PhysicalAction action1 = new PhysicalAction("manoj.field1", Integer.valueOf(1), false);
    final LogicalAction action2 = new LogicalAction(12, new Object[] { "K1", "V1" });
    final PhysicalAction action3 = new PhysicalAction("manoj.field2", new ObjectID(99), true);

    dnaWriter.setParentObjectID(new ObjectID(100));

    dnaWriter.addPhysicalAction(action1.getFieldName(), action1.getObject());
    dnaWriter.addLogicalAction(action2.getMethod(), action2.getParameters());
    dnaWriter.addPhysicalAction(action3.getFieldName(), action3.getObject());
    dnaWriter.markSectionEnd();
    dnaWriter.finalizeHeader();

    DNAImpl rv = new DNAImpl(objectStringSerializer, true);
    final TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    try {
      rv.deserializeFrom(in);
    } catch (IOException e) {
      throw new AssertionError("DNA creation failed");
    }
    return rv;
  }
}
