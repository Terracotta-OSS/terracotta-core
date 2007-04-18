/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MockMessageChannel;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.lockmanager.api.LockAwardContext;
import com.tc.util.TCAssertionError;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class LockTimerTest extends TestCase {

  private LockTimer          timer;
  private LockAwardContext   lockAwardContext;
  private MockChannelManager channelManager;
  private MockMessageChannel channel;
  private ChannelID          channelId;
  private int                timeout;

  protected void setUp() throws Exception {
    super.setUp();
    this.channelId = new ChannelID(101);
    this.channel = new MockMessageChannel(channelId);
    this.channelManager = new MockChannelManager();
    this.channelManager.addChannel(this.channel);

    this.timer = new LockTimer(this.channelManager);
    this.lockAwardContext = new LockAwardContext() {

      public LockID getLockID() {
        throw new ImplementMe();
      }

      public ChannelID getChannelID() {
        return channel.getChannelID();
      }

      public long getTimeout() {
        return timeout;
      }

    };
  }

  public void testNotifyAddPending() throws Exception {
    this.timeout = 1000;

    // check that pending count is greater than zero.
    try {
      this.timer.notifyAddPending(0, this.lockAwardContext);
      fail("Should have thrown an assertion error.");
    } catch (TCAssertionError e) {
      // expected
    }

    // adding a pending with a pending count greater than one
    this.timer.notifyAddPending(2, this.lockAwardContext);
    checkTimerDoesNotFire();

    // adding a pending with a pending count of one should schedule the timeout.
    this.timer.notifyAddPending(1, this.lockAwardContext);
    checkTimerFires();
  }

  public void testNotifyAward() throws Exception {
    this.timeout = 1000;

    // the pending count must be >= 0;
    try {
      this.timer.notifyAward(-1, this.lockAwardContext);
      fail("Should have thrown an assertion error.");
    } catch (TCAssertionError e) {
      // expected
    }

    // if the pending count is 0, then the timeout should not get scheduled.
    this.timer.notifyAward(0, this.lockAwardContext);
    checkTimerDoesNotFire();

    // if the pending count is > 0, then the timeout SHOULD get scheduled.
    this.timer.notifyAward(1, this.lockAwardContext);
    checkTimerFires();

    this.timer.notifyAward(100, this.lockAwardContext);
    checkTimerFires();
  }

  public void testNotifyRevoke() throws Exception {
    this.timeout = 1000;

    // if the award context is revoked without the timer having been scheduled, we should throw an assertion
    // error
    try {
      this.timer.notifyRevoke(this.lockAwardContext);
      fail("Expected an assertion error.");
    } catch (TCAssertionError e) {
      // expected
    }

    // if the award context is added without any pending and then revoked, there should be no assertion fired.
    this.timer.notifyAward(0, this.lockAwardContext);
    this.timer.notifyRevoke(this.lockAwardContext);

    // if the award context is revoked after being awarded, the timer should not fire
    this.timer.notifyAward(1, this.lockAwardContext);
    checkTimerFires();
    this.timer.notifyAward(1, this.lockAwardContext);
    Thread.sleep(timeout / 4);
    this.timer.notifyRevoke(this.lockAwardContext);
    checkTimerDoesNotFire();

    // if a different award context is revoked, the original one should still fire.
    this.timer.notifyAward(1, this.lockAwardContext);

    LockAwardContext newContext = new LockAwardContext() {

      public LockID getLockID() {
        throw new ImplementMe();
      }

      public ChannelID getChannelID() {
        return new ChannelID(234709381274908237L);
      }

      public long getTimeout() {
        return 1001;
      }
    };
    this.timer.notifyAward(1, newContext);
    this.timer.notifyRevoke(newContext);

    checkTimerFires();
  }

  public void testStartTimerForLock() throws Exception {
    this.timeout = 2000;
    // max time over the timeout to allow for the test to pass.
    long excessThreshold = 800;
    long start = System.currentTimeMillis();
    this.timer.startTimerForLock(this.lockAwardContext);

    checkTimerFires();

    long elapsed = this.channel.getLastClosedCallTimestamp() - start;

    // make sure that it didn't happen in less time than the lock timeout.
    assertTrue("elapsed time (" + elapsed + " ms.) not greater than or equal to the timeout (" + timeout + " ms.)",
               elapsed >= timeout);
    // make sure that it didn't happen in greater time than the lock timeout + the excess threshold.
    assertTrue("elapsed time (" + elapsed + " ms.) not less than or equal to the timeout plus excess threshold ("
               + (timeout + excessThreshold) + " ms.)", elapsed <= (timeout + excessThreshold));

  }

  public void testCancel() throws Exception {
    timeout = 2000;

    this.timer.startTimerForLock(this.lockAwardContext);
    // make sure the timer actually fires.
    checkTimerFires();

    // reschedule
    this.timer.startTimerForLock(this.lockAwardContext);

    // wait a bit...
    Thread.sleep(timeout / 4);
    // cancel the timer...
    LockAwardContext cancelled = this.timer.cancel(this.lockAwardContext);

    // we should never see the close call.
    checkTimerDoesNotFire();

    assertEquals("Unexpected return value from cancel.", this.lockAwardContext, cancelled);

    cancelled = this.timer.cancel(this.lockAwardContext);
    assertTrue("Return value from a cancel that doesn't cancel anything should be null but is: " + cancelled,
               cancelled == null);
  }

  private void checkTimerDoesNotFire() throws InterruptedException {
    assertTrue(timeout > 0);
    assertFalse(this.channel.waitForCloseCall(timeout + 1000));
  }

  private void checkTimerFires() throws InterruptedException {
    assertTrue(timeout > 0);
    assertTrue(this.channel.waitForCloseCall(timeout + 1000));
  }

  public static class MockChannelManager implements DSOChannelManager {

    private Map channels = new HashMap();

    public void addChannel(MessageChannel channel) {
      synchronized (channels) {
        this.channels.put(channel.getChannelID(), channel);
      }
    }

    public MessageChannel getActiveChannel(ChannelID id) {
      synchronized (channels) {
        return (MessageChannel) this.channels.get(id);
      }
    }

    public MessageChannel[] getActiveChannels() {
      throw new ImplementMe();
    }

    public boolean isActiveID(ChannelID channelID) {
      throw new ImplementMe();
    }

    public void closeAll(Collection channelIDs) {
      throw new ImplementMe();
    }

    public String getChannelAddress(ChannelID channelID) {
      return null;
    }

    public BatchTransactionAcknowledgeMessage newBatchTransactionAcknowledgeMessage(ChannelID channelID) {
      throw new ImplementMe();
    }

    public Set getAllActiveChannelIDs() {
      throw new ImplementMe();
    }

    public void addEventListener(DSOChannelManagerEventListener listener) {
      throw new ImplementMe();
    }

    public void makeChannelActive(ChannelID channelID, long startIDs, long endIDs, boolean persistent) {
      throw new ImplementMe();
    }

    public Set getRawChannelIDs() {
      throw new ImplementMe();
    }

    public void makeChannelActiveNoAck(MessageChannel channel) {
      throw new ImplementMe();
    }

  }

}