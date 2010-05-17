/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.impl.MockStage;
import com.tc.async.impl.TestClientConfigurationContext;
import com.tc.exception.ImplementMe;
import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.LockSpec;
import com.tc.management.lock.stats.TCStackTraceElement;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.ClientIDProviderImpl;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.gtx.TestClientGlobalTransactionManager;
import com.tc.object.handler.LockResponseHandler;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.ClientLockManagerImpl;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.ManualThreadIDManager;
import com.tc.object.locks.NullClientLockManagerConfig;
import com.tc.object.locks.RemoteLockManagerImpl;
import com.tc.object.locks.StringLockID;
import com.tc.object.locks.ThreadID;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.MockChannelManager;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionID;
import com.tc.objectserver.api.ObjectStatsManager;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.handler.RequestLockUnLockHandler;
import com.tc.objectserver.handler.RespondToRequestLockHandler;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.locks.LockManagerImpl;
import com.tc.objectserver.locks.NullChannelManager;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LockManagerSystemTest extends BaseDSOTestCase {

  // please keep this set to true so that tests on slow/loaded machines don't fail. When working on this test though, it
  // can be convenient to temporarily flip it to false
  private static final boolean  slow   = true;

  private static final TCLogger logger = CustomerLogging.getDSOGenericLogger();

  private ClientLockManager     clientLockManager;
  private ManualThreadIDManager threadManager;

  @Override
  public void setUp() throws Exception {
    BoundedLinkedQueue clientLockRequestQueue = new BoundedLinkedQueue();
    BoundedLinkedQueue serverLockRespondQueue = new BoundedLinkedQueue();

    TestRemoteLockManagerImpl rmtLockManager = new TestRemoteLockManagerImpl(new TestLockRequestMessageFactory(),
                                                                             new TestClientGlobalTransactionManager(),
                                                                             clientLockRequestQueue);

    threadManager = new ManualThreadIDManager();
    clientLockManager = new ClientLockManagerImpl(logger, new NullSessionManager(), rmtLockManager, threadManager,
                                                  new NullClientLockManagerConfig(),
                                                  ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);

    AbstractEventHandler serverLockUnlockHandler = new RequestLockUnLockHandler();

    TestServerConfigurationContext serverLockUnlockContext = new TestServerConfigurationContext();
    MockStage serverStage = new MockStage("LockManagerSystemTest");
    LockManager serverLockManager = new LockManagerImpl(serverStage.sink, new MockChannelManager());
    ((LockManagerImpl) serverLockManager).setLockStatisticsEnabled(true, new MockL2LockStatsManager());

    serverLockUnlockContext.addStage(ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE, serverStage);
    serverLockUnlockContext.lockManager = serverLockManager;

    serverLockUnlockHandler.initializeContext(serverLockUnlockContext);

    AbstractEventHandler serverRespondToRequestLcokHandler = new TestRespondToRequestLockHandler(serverLockRespondQueue);
    TestServerConfigurationContext serverRespondToRequestContext = new TestServerConfigurationContext();
    serverRespondToRequestContext.channelManager = new NullChannelManager();

    serverRespondToRequestLcokHandler.initializeContext(serverRespondToRequestContext);

    TestClientConfigurationContext clientLockResponseContext = new TestClientConfigurationContext();
    clientLockResponseContext.clientLockManager = this.clientLockManager;
    AbstractEventHandler clientLockResponseHandler = new LockResponseHandler(new NullSessionManager());

    clientLockResponseHandler.initializeContext(clientLockResponseContext);

    serverLockManager.start();

    // start the client side lock handler thread
    Thread clientLockRequestHandlerThread = new StageThread("Client Lock Handler", clientLockRequestQueue,
                                                            serverLockUnlockHandler);
    clientLockRequestHandlerThread.start();

    // start the server side lock respond thread
    Thread serverLockRespondHandlerThread = new StageThread("Server Lock Respond Handler", serverStage.sink.queue,
                                                            serverRespondToRequestLcokHandler);
    serverLockRespondHandlerThread.start();

    // start the client lock response handler
    Thread clientLockRespondHandlerThread = new StageThread("Client Lock Respond Thread", serverLockRespondQueue,
                                                            clientLockResponseHandler);
    clientLockRespondHandlerThread.start();
  }

  private static void sleep(long amount) {
    amount *= (slow ? 300 : 50);
    ThreadUtil.reallySleep(amount);
  }

  public void testLockAwardOrder() throws Exception {
    final LockID l1 = new StringLockID("1");

    final ThreadID tid1 = new ThreadID(1);
    final ThreadID tid2 = new ThreadID(2);
    final ThreadID tid3 = new ThreadID(3);

    final List lockRequestOrder = new ArrayList(2);
    final List lockAwardOrder = new ArrayList(2);

    threadManager.setThreadID(tid1);
    clientLockManager.lock(l1, LockLevel.READ);
    Thread t1 = new Thread() {
      @Override
      public void run() {
        String threadName = "t1";
        System.err.println("Thread " + threadName + " request lock");
        lockRequestOrder.add(threadName);
        LockManagerSystemTest.this.threadManager.setThreadID(tid3);
        LockManagerSystemTest.this.clientLockManager.lock(l1, LockLevel.WRITE);
        System.err.println("Thread " + threadName + " obtain lock");
        lockAwardOrder.add(threadName);
      }
    };

    Thread t2 = new Thread() {
      @Override
      public void run() {
        String threadName = "t2";
        System.err.println("Thread " + threadName + " request lock");
        lockRequestOrder.add(threadName);
        // to make sure that lock request order is correct i.e. t2 request the lock after t1
        ThreadUtil.reallySleep(30000);
        LockManagerSystemTest.this.threadManager.setThreadID(tid2);
        LockManagerSystemTest.this.clientLockManager.lock(l1, LockLevel.READ);
        System.err.println("Thread " + threadName + " obtain lock");
        lockAwardOrder.add(threadName);
      }
    };

    t1.start();
    sleep(5);
    t2.start();

    sleep(50);
    threadManager.setThreadID(tid1);
    clientLockManager.unlock(l1, LockLevel.READ);
    t1.join();
    threadManager.setThreadID(tid3);
    clientLockManager.unlock(l1, LockLevel.WRITE);
    t2.join();

    System.err.println(lockRequestOrder);
    System.err.println(lockAwardOrder);

    assertEquals(lockRequestOrder.get(0), lockAwardOrder.get(0));
    assertEquals(lockRequestOrder.get(1), lockAwardOrder.get(1));
  }

  public void testUpgradeNotSupported() throws Exception {
    final LockID l1 = new StringLockID("1");

    final ThreadID tid1 = new ThreadID(1);
    final ThreadID tid2 = new ThreadID(2);
    final ThreadID tid3 = new ThreadID(3);

    final SetOnceFlag flag = new SetOnceFlag();
    threadManager.setThreadID(tid1);
    clientLockManager.lock(l1, LockLevel.READ);
    threadManager.setThreadID(tid2);
    clientLockManager.lock(l1, LockLevel.READ);
    threadManager.setThreadID(tid3);
    clientLockManager.lock(l1, LockLevel.READ);

    Thread t = new Thread() {
      @Override
      public void run() {
        try {
          LockManagerSystemTest.this.threadManager.setThreadID(tid1);
          LockManagerSystemTest.this.clientLockManager.lock(l1, LockLevel.WRITE);
          throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
        } catch (TCLockUpgradeNotSupportedError e) {
          flag.set();
        }
      }
    };
    t.start();

    sleep(5);
    assertTrue(flag.isSet());

    threadManager.setThreadID(tid2);
    clientLockManager.unlock(l1, LockLevel.READ);
    threadManager.setThreadID(tid3);
    clientLockManager.unlock(l1, LockLevel.READ);

    t.join();

    Thread secondReader = new Thread() {
      @Override
      public void run() {
        System.out.println("Read requested !");
        LockManagerSystemTest.this.threadManager.setThreadID(tid2);
        LockManagerSystemTest.this.clientLockManager.lock(l1, LockLevel.READ);
        System.out.println("Got Read !");
      }
    };
    secondReader.start();

    sleep(5);

    Thread secondWriter = new Thread() {
      @Override
      public void run() {
        System.out.println("Write requested !");
        LockManagerSystemTest.this.threadManager.setThreadID(tid3);
        LockManagerSystemTest.this.clientLockManager.lock(l1, LockLevel.WRITE);
        System.out.println("Got Write !");
      }
    };
    secondWriter.start();

    sleep(5);
    secondReader.join(5000);
    assertFalse(secondReader.isAlive());

    threadManager.setThreadID(tid1);
    clientLockManager.unlock(l1, LockLevel.READ);
    assertTrue(secondWriter.isAlive());

    threadManager.setThreadID(tid2);
    clientLockManager.unlock(l1, LockLevel.READ);
    secondWriter.join(60000);
    assertFalse(secondWriter.isAlive());
  }

  public void testBasic() throws Exception {
    final LockID l1 = new StringLockID("1");
    final LockID l3 = new StringLockID("3");

    final ThreadID tid1 = new ThreadID(1);
    final ThreadID tid2 = new ThreadID(2);
    final ThreadID tid3 = new ThreadID(3);
    final ThreadID tid4 = new ThreadID(4);

    // Get the lock for threadID 1
    System.out.println("Asked for first lock");
    threadManager.setThreadID(tid1);
    clientLockManager.lock(l1, LockLevel.WRITE);

    System.out.println("Got first lock");

    // Try to get it again, this should pretty much be a noop as we handle recursive lock calls
    threadManager.setThreadID(tid1);
    clientLockManager.lock(l1, LockLevel.WRITE);
    System.out.println("Got first lock again");

    final boolean[] done = new boolean[2];

    // try obtaining a write lock on l1 in a second thread. This should block initially since a write lock is already
    // held on l1
    Thread t = new Thread() {
      @Override
      public void run() {
        System.out.println("Asked for second lock");
        threadManager.setThreadID(tid2);
        clientLockManager.lock(l1, LockLevel.WRITE);
        System.out.println("Got second lock");
        done[0] = true;
      }
    };

    t.start();
    sleep(5);
    assertFalse(done[0]);
    threadManager.setThreadID(tid1);
    clientLockManager.unlock(l1, LockLevel.WRITE);
    clientLockManager.unlock(l1, LockLevel.WRITE); // should unblock thread above
    sleep(5);
    assertTrue(done[0]); // thread should have been unblocked and finished

    // Get a bunch of read locks on l3
    threadManager.setThreadID(tid1);
    clientLockManager.lock(l3, LockLevel.READ);
    threadManager.setThreadID(tid2);
    clientLockManager.lock(l3, LockLevel.READ);
    threadManager.setThreadID(tid3);
    clientLockManager.lock(l3, LockLevel.READ);
    done[0] = false;
    t = new Thread() {
      @Override
      public void run() {
        System.out.println("Asking for write lock");
        threadManager.setThreadID(tid4);
        clientLockManager.lock(l3, LockLevel.WRITE);
        System.out.println("Got write lock");
        done[0] = true;
      }
    };
    t.start();
    sleep(5);
    assertFalse(done[0]);

    threadManager.setThreadID(tid1);
    clientLockManager.unlock(l3, LockLevel.READ);
    sleep(5);
    assertFalse(done[0]);

    threadManager.setThreadID(tid2);
    clientLockManager.unlock(l3, LockLevel.READ);
    sleep(5);
    assertFalse(done[0]);

    threadManager.setThreadID(tid3);
    clientLockManager.unlock(l3, LockLevel.READ);
    sleep(5);
    assertTrue(done[0]);

    done[0] = false;
    t = new Thread() {
      @Override
      public void run() {
        System.out.println("Asking for read lock");
        threadManager.setThreadID(tid1);
        clientLockManager.lock(l3, LockLevel.READ);
        System.out.println("Got read lock");
        done[0] = true;
      }
    };
    t.start();

    done[1] = false;
    t = new Thread() {
      @Override
      public void run() {
        System.out.println("Asking for read lock");
        threadManager.setThreadID(tid2);
        clientLockManager.lock(l3, LockLevel.READ);
        System.out.println("Got read lock");
        done[1] = true;
      }
    };

    t.start();
    sleep(5);
    assertFalse(done[0]);
    assertFalse(done[1]);
    threadManager.setThreadID(tid4);
    clientLockManager.unlock(l3, LockLevel.WRITE);
    sleep(5);
    assertTrue(done[0]);
    assertTrue(done[1]);
    threadManager.setThreadID(tid1);
    clientLockManager.unlock(l3, LockLevel.READ);
    threadManager.setThreadID(tid2);
    clientLockManager.unlock(l3, LockLevel.READ);
  }

  public void testTryLock() throws Throwable {
    final LockID l1 = new StringLockID("1");

    final ThreadID tid1 = new ThreadID(1);
    final ThreadID tid2 = new ThreadID(2);

    // Get the first lock
    System.out.println("Asked for first lock");
    threadManager.setThreadID(tid1);
    clientLockManager.lock(l1, LockLevel.WRITE);
    System.out.println("Got first lock");

    final int[] count1 = new int[1];

    // Try the lock 100 times while it's being locked by the first thread, this will
    // thus fail 100 times
    final Thread t1 = new Thread() {
      @Override
      public void run() {
        System.out.println("Trying second lock 100 times");
        for (int i = 0; i < 100; i++) {
          threadManager.setThreadID(tid2);
          try {
            if (!clientLockManager.tryLock(l1, LockLevel.WRITE, 0)) {
              count1[0]++;
            }
          } catch (InterruptedException e) {
            System.out.println("XXX INTERRUPTED XXX");
          }
        }
      }
    };

    t1.start();
    System.out.println("Waiting for 2nd thread to finish");
    t1.join();

    assertEquals(100, count1[0]);

    System.out.println("Releasing first lock");
    threadManager.setThreadID(tid1);
    clientLockManager.unlock(l1, LockLevel.WRITE);

    final int[] count2 = new int[1];

    // Try the lock 100 times while it's not being locked by the first thread, this will
    // thus never fail
    final Thread t2 = new Thread() {
      @Override
      public void run() {
        System.out.println("Trying second lock once more 100 times");
        for (int i = 0; i < 100; i++) {
          threadManager.setThreadID(tid2);
          try {
            if (!clientLockManager.tryLock(l1, LockLevel.WRITE, 0L)) {
              count2[0]++;
            }
          } catch (InterruptedException e) {
            System.out.println("XXX INTERRUPTED XXX");
          }
        }
      }
    };

    t2.start();
    System.out.println("Waiting for 2nd thread to finish");
    t2.join();

    assertEquals(0, count2[0]);
  }

  private static class TestRemoteLockManagerImpl extends RemoteLockManagerImpl {
    private BoundedLinkedQueue clientLockRequestQueue = null;

    public TestRemoteLockManagerImpl(LockRequestMessageFactory lrmf, ClientGlobalTransactionManager gtxManager,
                                     BoundedLinkedQueue clientLockRequestQueue) {
      super(new ClientIDProviderImpl(new TestChannelIDProvider()), GroupID.NULL_ID, lrmf, gtxManager,
            ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);
      this.clientLockRequestQueue = clientLockRequestQueue;
    }

    @Override
    protected void sendMessage(LockRequestMessage req) {
      try {
        clientLockRequestQueue.put(req);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  private static class MockL2LockStatsManager implements L2LockStatsManager {

    public void clearAllStatsFor(NodeID nodeID) {
      throw new ImplementMe();
    }

    public void enableStatsForNodeIfNeeded(NodeID nodeID) {
      throw new ImplementMe();
    }

    public int getGatherInterval() {
      throw new ImplementMe();
    }

    public Collection<LockSpec> getLockSpecs() {
      throw new ImplementMe();
    }

    public long getNumberOfLockHopRequests(LockID lockID) {
      throw new ImplementMe();
    }

    public long getNumberOfLockReleased(LockID lockID) {
      throw new ImplementMe();
    }

    public long getNumberOfLockRequested(LockID lockID) {
      throw new ImplementMe();
    }

    public long getNumberOfPendingRequests(LockID lockID) {
      throw new ImplementMe();
    }

    public int getTraceDepth() {
      throw new ImplementMe();
    }

    public boolean isLockStatisticsEnabled() {
      throw new ImplementMe();
    }

    public void recordClientStat(NodeID nodeID, Collection<TCStackTraceElement> lockStatElements) {
      throw new ImplementMe();
    }

    public void recordLockAwarded(LockID lockID, NodeID nodeID, ThreadID threadID, boolean isGreedy,
                                  long lockAwardTimestamp) {
      //
    }

    public void recordLockHopRequested(LockID lockID) {
      //
    }

    public void recordLockRejected(LockID lockID, NodeID nodeID, ThreadID threadID) {
      throw new ImplementMe();
    }

    public void recordLockReleased(LockID lockID, NodeID nodeID, ThreadID threadID) {
      //
    }

    public void recordLockRequested(LockID lockID, NodeID nodeID, ThreadID threadID, int numberOfPendingRequests) {
      //
    }

    public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
      throw new ImplementMe();
    }

    public void setLockStatisticsEnabled(boolean lockStatsEnabled) {
      throw new ImplementMe();
    }

    public void start(DSOChannelManager channelManager, DSOGlobalServerStats serverStats,
                      ObjectStatsManager objectManager) {
      throw new ImplementMe();
    }

    public synchronized TimeStampedCounterValue getLockRecallMostRecentSample() {
      return null;
    }
  }

  private static class TestRespondToRequestLockHandler extends RespondToRequestLockHandler {
    BoundedLinkedQueue serverLockRespondQueue;

    public TestRespondToRequestLockHandler(BoundedLinkedQueue serverLockRespondQueue) {
      this.serverLockRespondQueue = serverLockRespondQueue;
    }

    @Override
    protected LockResponseMessage createMessage(EventContext context, TCMessageType messageType) {
      return new LockResponseMessage(new SessionID(100), new NullMessageMonitor(), new TCByteBufferOutputStream(),
                                     new TestMessageChannel(), messageType);
    }

    @Override
    protected void send(LockResponseMessage responseMessage) {
      try {
        serverLockRespondQueue.put(responseMessage);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  private static class StageThread extends Thread {

    private final AbstractEventHandler handler;
    private final BoundedLinkedQueue   queue;

    StageThread(String name, BoundedLinkedQueue queue, AbstractEventHandler handler) {
      this.setName(name);
      this.queue = queue;
      this.handler = handler;
      this.setDaemon(true);
    }

    @Override
    public void run() {
      while (true) {
        EventContext ec;
        try {
          ec = (EventContext) queue.take();
          handler.handleEvent(ec);
        } catch (Exception e) {
          throw new AssertionError(e);
        }
      }
    }

  }
}
