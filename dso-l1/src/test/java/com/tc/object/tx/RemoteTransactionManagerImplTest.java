/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.terracotta.test.categories.CheckShorts;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.tx.RemoteTransactionManagerImpl.BatchManager;

@Category(CheckShorts.class)
public class RemoteTransactionManagerImplTest {
  private RemoteTransactionManager manager;
  @Mock
  private BatchManager             batchManager;
  private final TCLogger           logger             = TCLogging.getLogger(RemoteTransactionManagerImplTest.class);
  private final long               ackOnExitTimeoutMs = 1 * 1000;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(batchManager.sendNextBatch(false)).thenReturn(null);
    when(batchManager.sendNextBatch(true)).thenReturn(null);
    when(batchManager.isEmpty()).thenReturn(false);
    when(batchManager.size()).thenReturn(Integer.MAX_VALUE);
    manager = new RemoteTransactionManagerImpl(batchManager, null, null, logger, ackOnExitTimeoutMs, null, null, null,
                                               null, null, false, null, null, null);
  }

  @Test
  public void test_Stop_Waits_For_Timeout_When_Immediate_Shutdown_Not_Requested() {
    long timeTaken = stopManagerAndReturnTimeTaken();
    Assert.assertTrue(timeTaken > ackOnExitTimeoutMs);
    System.out.println("Test Finished, Time taken : " + (timeTaken) + " ms");
  }

  @Test
  public void test_Stop_Method_Doesnot_Wait_For_Timeout_When_NodeError() {
    this.whenNodeError().assertStopExitsImmediately();
  }

  private long stopManagerAndReturnTimeTaken() {
    long startTime = System.currentTimeMillis();
    manager.stop();
    long timeTaken = System.currentTimeMillis() - startTime;
    return timeTaken;
  }

  private void assertStopExitsImmediately() {
    long timeTaken = stopManagerAndReturnTimeTaken();
    Assert.assertTrue(timeTaken < ackOnExitTimeoutMs);
    System.out.println("Test Finished, Time taken : " + (timeTaken) + " ms");
  }

  private RemoteTransactionManagerImplTest whenNodeError() {
    ClusterEventListener listener = new ClusterEventListener(manager);
    listener.nodeError(null);
    return this;
  }
}
