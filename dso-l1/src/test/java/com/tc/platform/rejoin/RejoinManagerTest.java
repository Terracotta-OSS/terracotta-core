/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.platform.rejoin;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.platform.rejoin.RejoinManagerImpl.RejoinWorker;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class RejoinManagerTest {
  private RejoinManagerImpl           rejoinManager;
  private final boolean               isRejoinEnabled = true;
  @Mock
  private Queue<ClientMessageChannel> rejoinRequestedChannels;
  private RejoinWorker                rejoinWorker;
  @Mock
  ClientMessageChannel                messageChannel;

  @Before
  public void setup() {
    rejoinManager = new RejoinManagerImpl(isRejoinEnabled);
    // rejoinManager.start();
    rejoinWorker = rejoinManager.getRejoinWorkerThread();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void test_when_doRejoin_called_rejoin_count_gets_updated() throws Exception {
    final int rejoinCount = rejoinManager.getRejoinCount();
    rejoinManager.doRejoin(messageChannel);
    Assert.assertTrue((rejoinCount + 1) == rejoinManager.getRejoinCount());
  }

  @Test
  public void test_when_rejoin_request_makes_reopen_in_progress() throws InterruptedException {
    Assert.assertFalse(rejoinManager.isReopenInProgress());
    Thread thread = new Thread(new Runnable() {

      @Override
      public void run() {
        rejoinManager.getRejoinWorkerThread().waitUntilRejoinRequestedOrShutdown();
      }
    });
    thread.start();
    rejoinManager.requestRejoin(messageChannel);
    thread.join(30 * 1000);// wait for max 30sec
    Assert.assertTrue(rejoinManager.isReopenInProgress());
  }

  @Test
  public void test_when_rejoin_request_makes_rejoin_in_progress() {
    Assert.assertFalse(rejoinManager.isRejoinInProgress());
    rejoinManager.doRejoin(messageChannel);
    Assert.assertTrue(rejoinManager.isRejoinInProgress());
  }

  @Test
  public void test_request_rejoin_notifies_waiting_rejoinWorker() throws InterruptedException {
    rejoinWorker = rejoinManager.getRejoinWorkerThread();
    final AtomicBoolean error = new AtomicBoolean(false);
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        ClientMessageChannel tmpChannel = rejoinWorker.waitUntilRejoinRequestedOrShutdown();
        error.set(!tmpChannel.equals(messageChannel));
      }
    });
    thread.start();
    rejoinManager.requestRejoin(messageChannel);
    thread.join();
    Assert.assertFalse(error.get());
  }

  @Test
  public void test_rejoin_request_added_to_queue_when_requested() {
    Queue rejoinRequestsQueue = Mockito.mock(LinkedList.class);
    rejoinWorker.setRejoinRequestedChannelListForTesting(rejoinRequestsQueue);
    requestRejoin();
    Mockito.verify(rejoinRequestsQueue, Mockito.atLeastOnce()).add(null);
  }

  @Test
  public void test_rejoin_request_ignored_when_reopen_in_Progress() {
    rejoinManager.setReopenInProgress(true);
    setUpfailureWhenRejoinRequestAddedToRequestQueue().requestRejoin();
  }

  @Test
  public void test_doReopen_reopens_channel() throws Exception {
    rejoinManager.doReopen(messageChannel);
    Mockito.verify(messageChannel, Mockito.times(1)).reopen();
  }

  @Test
  public void test_doRejoin_notifies_rejoin_start_and_reopens_channel() throws Exception {
    final AtomicBoolean rejoinStartNotified = new AtomicBoolean(false);
    rejoinManager.addListener(new RejoinLifecycleListener() {
      @Override
      public void onRejoinStart() {
        rejoinStartNotified.set(true);
      }

      @Override
      public void onRejoinComplete() {

      }
    });
    rejoinManager.doRejoin(messageChannel);
    Assert.assertTrue(rejoinStartNotified.get());
    Mockito.verify(messageChannel, Mockito.times(1)).reopen();
  }

  @Test
  public void test_doReopen_unsets_reopenInProgress() throws Exception {
    rejoinManager.setReopenInProgress(true);
    rejoinManager.doReopen(messageChannel);
    Assert.assertFalse(rejoinManager.isRejoinInProgress());
    Mockito.verify(messageChannel, Mockito.times(1)).reopen();
  }

  @Test
  public void test_thisNodeJoined_wont_notifyRejoinComplete_when_rejoin_disabled() {
    RejoinManagerImpl tmpRejoinManager = new RejoinManagerImpl(false);
    tmpRejoinManager.addListener(new RejoinLifecycleListener() {
      @Override
      public void onRejoinStart() {
        Assert.fail();
      }

      @Override
      public void onRejoinComplete() {
        Assert.fail("Rejoin ain't enabled, You can't notify rejoinComplete");
      }
    });
    tmpRejoinManager.thisNodeJoined(null);
  }

  @Test
  public void test_thisNodeJoined_will_notifyRejoinComplete() throws Exception {
    final AtomicBoolean rejoinCompleteNotified = new AtomicBoolean(false);
    // this call is to set rejoinInProgress, otherwise notifyRejoinComplete won't get called
    rejoinManager.requestRejoin(messageChannel);
    rejoinManager.addListener(new RejoinLifecycleListener() {
      @Override
      public void onRejoinStart() {
      }

      @Override
      public void onRejoinComplete() {
        rejoinCompleteNotified.set(true);
      }
    });

    rejoinManager.thisNodeJoined(null);
    rejoinCompleteNotified.get();
  }

  @Test
  public void test_thisNodeJoined_will_unset_rejoinInProgress() throws Exception {
    Assert.assertFalse(rejoinManager.isRejoinInProgress());
    // this call is to set rejoinInProgress, otherwise notifyRejoinComplete won't get called
    rejoinManager.doRejoin(messageChannel);
    Assert.assertTrue(rejoinManager.isRejoinInProgress());
    rejoinManager.thisNodeJoined(null);
    Assert.assertFalse(rejoinManager.isRejoinInProgress());
  }

  @Test
  public void test_rejoin_request_Ignored_when_shutdown_in_progress() {
    rejoinManager.shutdown();
    setUpfailureWhenRejoinRequestAddedToRequestQueue().requestRejoin();
  }

  private RejoinManagerTest requestRejoin() {
    rejoinManager.requestRejoin(null);
    return this;
  }

  private RejoinManagerTest setUpfailureWhenRejoinRequestAddedToRequestQueue() {
    Mockito.doThrow(new AssertionError("Rejoin request not ignored")).when(rejoinRequestedChannels)
        .add((ClientMessageChannel) Mockito.any());
    setCustomRejoinRequestQueue();
    return this;
  }

  private void setCustomRejoinRequestQueue() {
    rejoinWorker.setRejoinRequestedChannelListForTesting(rejoinRequestedChannels);
  }

  @Test
  public void test_rejoin_requested_thorws_assertionError_when_rejoin_not_enabled() {
    RejoinManagerImpl tmpRejoinManager = new RejoinManagerImpl(false);
    try {
      tmpRejoinManager.requestRejoin(null);
    } catch (AssertionError e) {
      // expected
    }
  }

  @Test
  public void test_doRejoin_notifies_rejoin_start_to_rejoin_listeners() throws Exception {
    final AtomicBoolean rejoinStarted = new AtomicBoolean(false);
    rejoinManager.addListener(getRejoinLifeCycleListener(rejoinStarted));
    rejoinManager.doRejoin(messageChannel);
    Assert.assertTrue(rejoinStarted.get());
  }

  private RejoinLifecycleListener getRejoinLifeCycleListener(final AtomicBoolean rejoinStarted) {
    return new RejoinLifecycleListener() {

      @Override
      public void onRejoinStart() {
        rejoinStarted.set(true);
      }

      @Override
      public void onRejoinComplete() {

      }
    };
  }

  @After
  public void tearDown() {
    rejoinManager.shutdown();
  }

}
