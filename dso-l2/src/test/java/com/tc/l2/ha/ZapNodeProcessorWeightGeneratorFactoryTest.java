/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.core.TCConnection;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.net.MockChannelManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TestServerTransaction;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchContext;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.test.TCTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

@Category(CheckShorts.class)
public class ZapNodeProcessorWeightGeneratorFactoryTest extends TCTestCase {
  TestDsoChannelManager          channelManager1           = new TestDsoChannelManager();
  TestDsoChannelManager          channelManager2           = new TestDsoChannelManager();
  TestTransactionBatchManager    transactionBatchManager1  = new TestTransactionBatchManager();
  TestTransactionBatchManager    transactionBatchManager2  = new TestTransactionBatchManager();
  TestWGServerTransactionManager serverTransactionManager1 = new TestWGServerTransactionManager();
  TestWGServerTransactionManager serverTransactionManager2 = new TestWGServerTransactionManager();
  String                         host1;
  String                         host2;
  int                            port1;
  int                            port2;

  public void testServerIdentifierWeightGenerator() {
    try {
      allocate();
      Assert.fail("Illegal hostname shall throw exception");
    } catch (TCRuntimeException e) {
      // expected
    }

    WeightGenerator wg1 = new ServerIdentifierWeightGenerator("localhost", 10);
    WeightGenerator wg2 = new ServerIdentifierWeightGenerator("localhost", 11);
    Assert.assertTrue(wg2.getWeight() > wg1.getWeight());

  }

  protected ServerIdentifierWeightGenerator allocate() {
    return new ServerIdentifierWeightGenerator("XXX", 0);
  }

  public void testZapNodeProcessorWeightGeneratorFactory() {
    host1 = "localhost";
    port1 = 1000;
    WeightGeneratorFactory wgf1 = new ZapNodeProcessorWeightGeneratorFactory(channelManager1, transactionBatchManager1,
                                                                             serverTransactionManager1, host1, port1);

    host2 = "localhost";
    port2 = 2000;
    WeightGeneratorFactory wgf2 = new ZapNodeProcessorWeightGeneratorFactory(channelManager2, transactionBatchManager2,
                                                                             serverTransactionManager2, host2, port2);

    // At fresh start, decided by host and port
    Assert.assertEquals(wgf2, whoWin(wgf1, wgf2));

    // who has more txn count at active
    setServerActive(serverTransactionManager1, true);
    addTxnCount(serverTransactionManager1, 10);
    setServerActive(serverTransactionManager2, true);
    addTxnCount(serverTransactionManager2, 5);
    Assert.assertEquals(wgf1, whoWin(wgf1, wgf2));

    addTxnCount(serverTransactionManager2, 50);
    Assert.assertEquals(wgf2, whoWin(wgf1, wgf2));

    setServerActive(serverTransactionManager1, false);
    addTxnCount(serverTransactionManager1, 50);
    Assert.assertEquals(wgf2, whoWin(wgf1, wgf2));

    // who has latest txn
    toughTxnTime(transactionBatchManager1);
    Assert.assertEquals(wgf1, whoWin(wgf1, wgf2));

    toughTxnTime(transactionBatchManager2);
    Assert.assertEquals(wgf2, whoWin(wgf2, wgf1));

    // who has more L1s
    setActiveChannels(channelManager1, 2);
    setActiveChannels(channelManager2, 3);
    Assert.assertEquals(wgf2, whoWin(wgf1, wgf2));

    setActiveChannels(channelManager1, 5);
    Assert.assertEquals(wgf1, whoWin(wgf1, wgf2));
  }

  public WeightGeneratorFactory whoWin(WeightGeneratorFactory wgf1, WeightGeneratorFactory wgf2) {
    WeightGeneratorFactory winner = compareWeights(wgf1, wgf2);
    if (winner == wgf1) {
      System.out.println("wgf1 win");
    } else if (winner == wgf2) {
      System.out.println("wgf2 win");
    } else {
      System.out.println("None win ");
    }
    return winner;
  }

  public WeightGeneratorFactory compareWeights(WeightGeneratorFactory wgf1, WeightGeneratorFactory wgf2) {
    long[] weights1 = wgf1.generateWeightSequence();
    long[] weights2 = wgf2.generateWeightSequence();
    int l1 = weights1.length;
    int l2 = weights2.length;

    StringBuffer buf = new StringBuffer();
    buf.append("wgf1: ");
    for (long l : weights1) {
      buf.append(l);
      buf.append(" ");
    }
    System.out.println(buf);

    buf = new StringBuffer();
    buf.append("wgf2: ");
    for (long l : weights2) {
      buf.append(l);
      buf.append(" ");
    }
    System.out.println(buf);

    if (l1 > l2) return wgf1;
    if (l2 > l1) return wgf2;

    for (int i = 0; i < l1; ++i) {
      if (weights1[i] > weights2[i]) return wgf1;
      if (weights2[i] > weights1[i]) return wgf2;
    }

    // no one win return null
    return null;
  }

  private void setActiveChannels(TestDsoChannelManager mgr, int chs) {
    mgr.setActiveChannels(chs);
  }

  private void toughTxnTime(TestTransactionBatchManager mgr) {
    mgr.addTransactionBatch(null);
  }

  private void addTxnCount(TestWGServerTransactionManager mgr, int count) {
    Map<ServerTransactionID, ServerTransaction> txnIDs = new HashMap<ServerTransactionID, ServerTransaction>();
    for (int i = 0; i < count; ++i) {
      ServerTransactionID id = new ServerTransactionID(new ClientID(i), new TransactionID(i));
      txnIDs.put(id, new TestServerTransaction(id, TxnBatchID.NULL_BATCH_ID));
    }
    mgr.incomingTransactions(ServerID.NULL_ID, txnIDs);
  }

  private void setServerActive(TestWGServerTransactionManager mgr, boolean active) {
    mgr.setActive(active);
  }

  private static class TestDsoChannelManager extends MockChannelManager {
    private int activeChannels = 0;

    void setActiveChannels(int channels) {
      activeChannels = channels;
    }

    @Override
    public TCConnection[] getAllActiveClientConnections() {
      return new TCConnection[activeChannels];
    }
  }

  private static class TestTransactionBatchManager implements TransactionBatchManager {
    private final List<TransactionBatchListener> txnListeners = new CopyOnWriteArrayList<TransactionBatchListener>();

    @Override
    public void addTransactionBatch(CommitTransactionMessage ctm) {
      fireBatchTxnEvent(ctm);
    }

    @Override
    public boolean batchComponentComplete(NodeID committerID, TransactionID txnID) {
      throw new ImplementMe();
    }

    @Override
    public void defineBatch(NodeID node, int numTxns) {
      throw new ImplementMe();
    }

    @Override
    public void nodeConnected(NodeID nodeID) {
      throw new ImplementMe();
    }

    @Override
    public void notifyServerHighWaterMark(NodeID nodeID, long serverHighWaterMark) {
      throw new ImplementMe();

    }

    @Override
    public void processTransactions(TransactionBatchContext batchContext) {
      throw new ImplementMe();
    }

    @Override
    public void registerForBatchTransaction(TransactionBatchListener listener) {
      txnListeners.add(listener);
    }

    private void fireBatchTxnEvent(CommitTransactionMessage ctm) {
      for (TransactionBatchListener listener : txnListeners) {
        listener.notifyTransactionBatchAdded(ctm);
      }
    }

    @Override
    public void shutdownNode(NodeID nodeID) {
      throw new ImplementMe();
    }

  }

  private static class TestWGServerTransactionManager extends TestServerTransactionManager {
    private boolean    isActive          = false;
    private final AtomicLong numOfTransactions = new AtomicLong(0);

    @Override
    public void incomingTransactions(NodeID nodeID, Map<ServerTransactionID, ServerTransaction> txns) {
      if (isActive) this.numOfTransactions.addAndGet(txns.size());
    }

    public void setActive(boolean active) {
      isActive = active;
    }

    @Override
    public long getTotalNumOfActiveTransactions() {
      return numOfTransactions.get();
    }

  }

}
