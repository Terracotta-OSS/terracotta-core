/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.sequence;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;

public class GroupBatchSequenceProviderTest extends TCTestCase {
  public static final int LOOP_COUNT             = 5;
  public static final int REQUEST_SIZE_PER_BATCH = 10;
  public static final int SKIP_CONSTANT          = 100;

  public void testBasic() {
    DummyReceiverBasic receiver = new DummyReceiverBasic();
    TestBatchSequenceProviderBasic remoteProvider = new TestBatchSequenceProviderBasic(LOOP_COUNT
                                                                                       * REQUEST_SIZE_PER_BATCH);

    GroupBatchSequenceProvider provider = new GroupBatchSequenceProvider(remoteProvider, LOOP_COUNT
                                                                                         * REQUEST_SIZE_PER_BATCH);
    provider.setNextBatch(0, LOOP_COUNT * REQUEST_SIZE_PER_BATCH);

    int current = 0;
    makeRequests(receiver, provider, current);
    Assert.assertEquals(0, remoteProvider.getRequestCount());

    current += LOOP_COUNT * REQUEST_SIZE_PER_BATCH;
    makeRequests(receiver, provider, current);
    Assert.assertEquals(1, remoteProvider.getRequestCount());

    current += (LOOP_COUNT * REQUEST_SIZE_PER_BATCH + SKIP_CONSTANT);
    makeRequests(receiver, provider, current);
    Assert.assertEquals(2, remoteProvider.getRequestCount());
  }

  public void testOddGroups() {
    TestBatchSequenceProviderOddGroup remoteProvider = new TestBatchSequenceProviderOddGroup();

    GroupBatchSequenceProvider provider = new GroupBatchSequenceProvider(remoteProvider, SKIP_CONSTANT);
    DummyReceiverOddGroup receiver = new DummyReceiverOddGroup();
    provider.requestBatch(receiver, 33);
    provider.requestBatch(receiver, 33);
    provider.requestBatch(receiver, 33);

    // this should finish the objects in GroupBatchSequenceProvider. Now when u call provider.requestBatch, it shud to
    // server again. At this point if server crashes, then is batch request pending should be false
    provider.requestBatch(receiver, 33);

    Assert.assertTrue(provider.isBatchRequestPending());
    Assert.assertEquals(66, receiver.getLastStart());
    Assert.assertEquals(99, receiver.getLastEnd());
    Assert.assertEquals(2, remoteProvider.getRequestCount());
  }

  private void makeRequests(DummyReceiverBasic receiver, GroupBatchSequenceProvider provider, int current) {
    for (int i = 0; i < LOOP_COUNT; i++) {
      int expectedStart = current + REQUEST_SIZE_PER_BATCH * i;
      int expectedEnd = current + REQUEST_SIZE_PER_BATCH * i + REQUEST_SIZE_PER_BATCH;
      receiver.setExpectedNextBatch(expectedStart, expectedEnd);
      provider.requestBatch(receiver, 10);
      Assert.assertTrue(receiver.getAndReset());
    }
  }

  private class DummyReceiverBasic implements BatchSequenceReceiver {

    private long    expectedStart        = 0;
    private long    expectedEnd          = 0;
    private boolean isSetNextBatchCalled = false;

    public void setExpectedNextBatch(long start, long end) {
      expectedStart = start;
      expectedEnd = end;
    }

    public boolean getAndReset() {
      boolean prev = isSetNextBatchCalled;
      isSetNextBatchCalled = false;
      return prev;
    }

    @Override
    public boolean isBatchRequestPending() {
      return false;
    }

    @Override
    public void setNextBatch(long start, long end) {
      System.out.println("Next Batch Called with: start=" + start + " end=" + end);
      Assert.assertFalse(isSetNextBatchCalled);
      isSetNextBatchCalled = true;
      Assert.assertEquals(expectedStart, start);
      Assert.assertEquals(expectedEnd, end);
    }

  }

  private class DummyReceiverOddGroup implements BatchSequenceReceiver {
    private long lastStart;
    private long lastEnd;

    @Override
    public boolean isBatchRequestPending() {
      return false;
    }

    @Override
    public void setNextBatch(long start, long end) {
      lastStart = start;
      lastEnd = end;
    }

    public long getLastStart() {
      return lastStart;
    }

    public long getLastEnd() {
      return lastEnd;
    }

  }

  private static class TestBatchSequenceProviderBasic implements BatchSequenceProvider {
    private int current      = 0;

    private int requestCount = 0;

    public TestBatchSequenceProviderBasic(int start) {
      current = start;
    }

    @Override
    public void requestBatch(BatchSequenceReceiver receiver, int size) {
      requestCount++;
      int start = current;
      int end = current + size;
      current = end + SKIP_CONSTANT;
      receiver.setNextBatch(start, end);
    }

    public int getRequestCount() {
      return requestCount;
    }
  }

  private static class TestBatchSequenceProviderOddGroup implements BatchSequenceProvider {
    private int current      = 0;
    private int requestCount = 0;

    @Override
    public void requestBatch(BatchSequenceReceiver receiver, int size) {
      requestCount++;
      if (requestCount > 1) {
        // this is to simulate a server crash
        return;
      }

      int start = current;
      int end = current + size;
      current = end + SKIP_CONSTANT;
      receiver.setNextBatch(start, end);
    }

    public int getRequestCount() {
      return requestCount;
    }
  }
}
