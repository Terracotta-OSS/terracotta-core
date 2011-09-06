/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.util.Assert;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

public class BatchedTransactionImplUnitTest extends TestCase {

  public void testBatchedTransactionImplSingleChangePerCommit() {
    int[] batchSizes = { 1, 10, 500, 5000 };
    for (int batchSize : batchSizes) {
      final AtomicInteger newTxnCallsCount = new AtomicInteger();
      final AtomicInteger txnCommitCallsCount = new AtomicInteger();
      final PersistenceTransactionProvider mockPtp = Mockito.mock(PersistenceTransactionProvider.class);
      final PersistenceTransaction mockPersistenceTransaction = Mockito.mock(PersistenceTransaction.class);

      Mockito.when(mockPtp.newTransaction()).thenAnswer(new Answer<PersistenceTransaction>() {
        public PersistenceTransaction answer(InvocationOnMock invocation) throws Throwable {
          newTxnCallsCount.incrementAndGet();
          return mockPersistenceTransaction;
        }
      });

      Mockito.doAnswer(new Answer<Void>() {
        public Void answer(InvocationOnMock invocation) throws Throwable {
          txnCommitCallsCount.incrementAndGet();
          return null;
        }
      }).when(mockPersistenceTransaction).commit();

      BatchedTransaction bt = new BatchedTransactionImpl(mockPtp, batchSize);
      bt.startBatchedTransaction();
      int changesCount = 0;
      for (int i = 1; i <= 10000; i++) {
        changesCount++;
        bt.optionalCommit(1);

        int expectedNumTxns = (changesCount / batchSize) + 1;
        Assert.assertEquals("Failed for batchSize: " + batchSize + ", changesCount: " + changesCount + ", i=" + i,
                            expectedNumTxns - 1, txnCommitCallsCount.get());
        Assert.assertEquals("Failed for batchSize: " + batchSize + ", changesCount: " + changesCount + ", i=" + i,
                            expectedNumTxns, newTxnCallsCount.get());
      }
      long totalCount = bt.completeBatchedTransaction();
      long expectedNumTxns = (totalCount / batchSize) + 1;
      Assert.assertEquals("Failed for batchSize: " + batchSize + ", changesCount: " + changesCount,
                          Long.valueOf(expectedNumTxns - 1), Long.valueOf(txnCommitCallsCount.get()));
      Assert.assertEquals("Failed for batchSize: " + batchSize + ", changesCount: " + changesCount,
                          Long.valueOf(changesCount), Long.valueOf(totalCount));
    }
  }

  public void testBatchedTransactionImplMultipleChangesPerCommit() {
    int[] batchSizes = { 1, 10, 500, 5000 };
    for (int batchSize : batchSizes) {
      final AtomicInteger newTxnCallsCount = new AtomicInteger();
      final AtomicInteger txnCommitCallsCount = new AtomicInteger();
      final PersistenceTransactionProvider mockPtp = Mockito.mock(PersistenceTransactionProvider.class);
      final PersistenceTransaction mockPersistenceTransaction = Mockito.mock(PersistenceTransaction.class);

      Mockito.when(mockPtp.newTransaction()).thenAnswer(new Answer<PersistenceTransaction>() {
        public PersistenceTransaction answer(InvocationOnMock invocation) throws Throwable {
          newTxnCallsCount.incrementAndGet();
          return mockPersistenceTransaction;
        }
      });

      Mockito.doAnswer(new Answer<Void>() {
        public Void answer(InvocationOnMock invocation) throws Throwable {
          txnCommitCallsCount.incrementAndGet();
          return null;
        }
      }).when(mockPersistenceTransaction).commit();

      BatchedTransaction bt = new BatchedTransactionImpl(mockPtp, batchSize);
      bt.startBatchedTransaction();
      int expectedTotalChanges = 0;
      int changesCount = 0;
      int expectedNewTxnCount = 1;
      for (int i = 1; i <= 10000; i++) {
        expectedTotalChanges += i;
        changesCount += i;
        bt.optionalCommit(i);

        if (changesCount >= batchSize) {
          changesCount = 0;
          expectedNewTxnCount++;
        }

        Assert.assertEquals("Failed for batchSize: " + batchSize + ", changesCount: " + changesCount + ", i=" + i,
                            expectedNewTxnCount - 1, txnCommitCallsCount.get());
        Assert.assertEquals("Failed for batchSize: " + batchSize + ", changesCount: " + changesCount + ", i=" + i,
                            expectedNewTxnCount, newTxnCallsCount.get());
      }
      long actualTotalChangesCount = bt.completeBatchedTransaction();
      Assert.assertEquals("Failed for batchSize: " + batchSize + ", changesCount: " + changesCount,
                          expectedNewTxnCount - 1, txnCommitCallsCount.get());
      Assert.assertEquals("Failed for batchSize: " + batchSize + ", changesCount: " + changesCount,
                          Long.valueOf(expectedTotalChanges), Long.valueOf(actualTotalChangesCount));
    }
  }
}
