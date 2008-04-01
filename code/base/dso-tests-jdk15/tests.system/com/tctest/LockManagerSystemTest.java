/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.impl.MockStage;
import com.tc.async.impl.TestClientConfigurationContext;
import com.tc.config.lock.LockContextInfo;
import com.tc.exception.ImplementMe;
import com.tc.exception.TCLockUpgradeNotSupportedError;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.LockSpec;
import com.tc.management.lock.stats.TCStackTraceElement;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.gtx.TestClientGlobalTransactionManager;
import com.tc.object.handler.LockResponseHandler;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.impl.ClientLockManagerImpl;
import com.tc.object.lockmanager.impl.RemoteLockManagerImpl;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.MockChannelManager;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.TestServerConfigurationContext;
import com.tc.objectserver.handler.RequestLockUnLockHandler;
import com.tc.objectserver.handler.RespondToRequestLockHandler;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.lockmanager.api.NullChannelManager;
import com.tc.objectserver.lockmanager.impl.LockManagerImpl;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collection;

public class LockManagerSystemTest extends BaseDSOTestCase {

  // please keep this set to true so that tests on slow/loaded machines don't fail. When working on this test though, it
  // can be convenient to temporarily flip it to false
  private static final boolean  slow   = true;

  private static final TCLogger logger = CustomerLogging.getDSOGenericLogger();

  private ClientLockManagerImpl clientLockManager;

  public void setUp() throws Exception {
    BoundedLinkedQueue clientLockRequestQueue = new BoundedLinkedQueue();
    BoundedLinkedQueue serverLockRespondQueue = new BoundedLinkedQueue();
    
    TestRemoteLockManagerImpl rmtLockManager = new TestRemoteLockManagerImpl(new TestLockRequestMessageFactory(),
                                                                             new TestClientGlobalTransactionManager(),
                                                                             clientLockRequestQueue);
    clientLockManager = new ClientLockManagerImpl(logger, rmtLockManager, new NullSessionManager(),
                                                  ClientLockStatManager.NULL_CLIENT_LOCK_STAT_MANAGER);

    LockManager serverLockManager = new LockManagerImpl(new MockChannelManager(), new MockL2LockStatsManager());

    AbstractEventHandler serverLockUnlockHandler = new RequestLockUnLockHandler();

    TestServerConfigurationContext serverLockUnlockContext = new TestServerConfigurationContext();
    MockStage serverStage = new MockStage("LockManagerSystemTest");
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

  public void testUpgradeNotSupported() throws Exception {
    final LockID l1 = new LockID("1");

    final ThreadID tid1 = new ThreadID(1);
    final ThreadID tid2 = new ThreadID(2);
    final ThreadID tid3 = new ThreadID(3);

    final SetOnceFlag flag = new SetOnceFlag();
    clientLockManager.lock(l1, tid1, LockLevel.READ, String.class.getName(), LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    clientLockManager.lock(l1, tid2, LockLevel.READ, String.class.getName(), LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    clientLockManager.lock(l1, tid3, LockLevel.READ, String.class.getName(), LockContextInfo.NULL_LOCK_CONTEXT_INFO);

    Thread t = new Thread() {
      public void run() {
        try {
          LockManagerSystemTest.this.clientLockManager.lock(l1, tid1, LockLevel.WRITE, String.class.getName(),
                                                            LockContextInfo.NULL_LOCK_CONTEXT_INFO);
          throw new AssertionError("Should have thrown a TCLockUpgradeNotSupportedError.");
        } catch (TCLockUpgradeNotSupportedError e) {
          flag.set();
        }
      }
    };
    t.start();

    sleep(5);
    assertTrue(flag.isSet());

    clientLockManager.unlock(l1, tid2);
    clientLockManager.unlock(l1, tid3);

    t.join();

    Thread secondReader = new Thread() {
      public void run() {
        System.out.println("Read requested !");
        LockManagerSystemTest.this.clientLockManager.lock(l1, tid2, LockLevel.READ, String.class.getName(),
                                                          LockContextInfo.NULL_LOCK_CONTEXT_INFO);
        System.out.println("Got Read !");
      }
    };
    secondReader.start();
    
    sleep(5);

    Thread secondWriter = new Thread() {
      public void run() {
        System.out.println("Write requested !");
        LockManagerSystemTest.this.clientLockManager.lock(l1, tid3, LockLevel.WRITE, String.class.getName(),
                                                          LockContextInfo.NULL_LOCK_CONTEXT_INFO);
        System.out.println("Got Write !");
      }
    };
    secondWriter.start();

    sleep(5);
    secondReader.join(5000);
    assertFalse(secondReader.isAlive());

    clientLockManager.unlock(l1, tid1);
    assertTrue(secondWriter.isAlive());

    clientLockManager.unlock(l1, tid2);
    secondWriter.join(60000);
    assertFalse(secondWriter.isAlive());
  }

  public void testBasic() throws Exception {
    final LockID l1 = new LockID("1");
    final LockID l3 = new LockID("3");

    final ThreadID tid1 = new ThreadID(1);
    final ThreadID tid2 = new ThreadID(2);
    final ThreadID tid3 = new ThreadID(3);
    final ThreadID tid4 = new ThreadID(4);

    // Get the lock for threadID 1
    System.out.println("Asked for first lock");
    clientLockManager.lock(l1, tid1, LockLevel.WRITE, String.class.getName(), LockContextInfo.NULL_LOCK_CONTEXT_INFO);

    System.out.println("Got first lock");

    // Try to get it again, this should pretty much be a noop as we handle recursive lock calls
    clientLockManager.lock(l1, tid1, LockLevel.WRITE, String.class.getName(), LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    System.out.println("Got first lock again");

    final boolean[] done = new boolean[2];

    // try obtaining a write lock on l1 in a second thread. This should block initially since a write lock is already
    // held on l1
    Thread t = new Thread() {
      public void run() {
        System.out.println("Asked for second lock");
        clientLockManager.lock(l1, tid2, LockLevel.WRITE, String.class.getName(),
                               LockContextInfo.NULL_LOCK_CONTEXT_INFO);
        System.out.println("Got second lock");
        done[0] = true;
      }
    };

    t.start();
    sleep(5);
    assertFalse(done[0]);
    clientLockManager.unlock(l1, tid1);
    clientLockManager.unlock(l1, tid1); // should unblock thread above
    sleep(5);
    assertTrue(done[0]); // thread should have been unblocked and finished

    // Get a bunch of read locks on l3
    clientLockManager.lock(l3, tid1, LockLevel.READ, String.class.getName(), LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    clientLockManager.lock(l3, tid2, LockLevel.READ, String.class.getName(), LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    clientLockManager.lock(l3, tid3, LockLevel.READ, String.class.getName(), LockContextInfo.NULL_LOCK_CONTEXT_INFO);
    done[0] = false;
    t = new Thread() {
      public void run() {
        System.out.println("Asking for write lock");
        clientLockManager.lock(l3, tid4, LockLevel.WRITE, String.class.getName(),
                               LockContextInfo.NULL_LOCK_CONTEXT_INFO);
        System.out.println("Got write lock");
        done[0] = true;
      }
    };
    t.start();
    sleep(5);
    assertFalse(done[0]);

    clientLockManager.unlock(l3, tid1);
    sleep(5);
    assertFalse(done[0]);

    clientLockManager.unlock(l3, tid2);
    sleep(5);
    assertFalse(done[0]);

    clientLockManager.unlock(l3, tid3);
    sleep(5);
    assertTrue(done[0]);

    done[0] = false;
    t = new Thread() {
      public void run() {
        System.out.println("Asking for read lock");
        clientLockManager
            .lock(l3, tid1, LockLevel.READ, String.class.getName(), LockContextInfo.NULL_LOCK_CONTEXT_INFO);
        System.out.println("Got read lock");
        done[0] = true;
      }
    };
    t.start();

    done[1] = false;
    t = new Thread() {
      public void run() {
        System.out.println("Asking for read lock");
        clientLockManager
            .lock(l3, tid2, LockLevel.READ, String.class.getName(), LockContextInfo.NULL_LOCK_CONTEXT_INFO);
        System.out.println("Got read lock");
        done[1] = true;
      }
    };

    t.start();
    sleep(5);
    assertFalse(done[0]);
    assertFalse(done[1]);
    clientLockManager.unlock(l3, tid4);
    sleep(5);
    assertTrue(done[0]);
    assertTrue(done[1]);
    clientLockManager.unlock(l3, tid1);
    clientLockManager.unlock(l3, tid2);
  }

  private static class TestRemoteLockManagerImpl extends RemoteLockManagerImpl {
    private BoundedLinkedQueue clientLockRequestQueue = null;

    public TestRemoteLockManagerImpl(LockRequestMessageFactory lrmf, ClientGlobalTransactionManager gtxManager,
                                     BoundedLinkedQueue clientLockRequestQueue) {
      super(lrmf, gtxManager);
      this.clientLockRequestQueue = clientLockRequestQueue;
    }

    protected void send(LockRequestMessage req) {
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

    public void recordLockRequested(LockID lockID, NodeID nodeID, ThreadID threadID, String lockType,
                                    int numberOfPendingRequests) {
      //
    }

    public void setLockStatisticsConfig(int traceDepth, int gatherInterval) {
      throw new ImplementMe();
    }

    public void setLockStatisticsEnabled(boolean lockStatsEnabled) {
      throw new ImplementMe();
    }

    public void start(DSOChannelManager channelManager) {
      throw new ImplementMe();
    }

  }

  private static class TestRespondToRequestLockHandler extends RespondToRequestLockHandler {
    BoundedLinkedQueue serverLockRespondQueue;

    public TestRespondToRequestLockHandler(BoundedLinkedQueue serverLockRespondQueue) {
      this.serverLockRespondQueue = serverLockRespondQueue;
    }

    protected LockResponseMessage createMessage(EventContext context, TCMessageType messageType) {
      return new LockResponseMessage(new SessionID(100), new NullMessageMonitor(), new TCByteBufferOutputStream(),
                                     null, messageType);
    }

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
