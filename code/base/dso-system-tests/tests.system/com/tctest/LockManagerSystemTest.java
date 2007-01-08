/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.NewSystemConfig;
import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.ConfigItemListener;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.schema.NewDSOApplicationConfig;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.impl.ClientLockManagerImpl;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.lockmanager.api.DeadlockChain;
import com.tc.objectserver.lockmanager.api.DeadlockResults;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.server.NullTCServerInfo;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LockManagerSystemTest extends BaseDSOTestCase {

  // please keep this set to true so that tests on slow/loaded machines don't fail. When working on this test though, it
  // can be convenient to temporarily flip it to false
  private static final boolean    slow = true;

  private DistributedObjectServer server;
  private DistributedObjectClient client;
  private ClientLockManagerImpl   lockManager;

  public void setUp() throws Exception {
    TestTVSConfigurationSetupManagerFactory factory = createDistributedConfigFactory();

    ManagedObjectStateFactory.disableSingleton(true);
    L2TVSConfigurationSetupManager l2Manager = factory.createL2TVSConfigurationSetupManager(null);
    server = new DistributedObjectServer(new ConfigOverride(l2Manager),
                                         new TCThreadGroup(new ThrowableHandler(TCLogging
                                             .getLogger(DistributedObjectServer.class))), new NullConnectionPolicy(),
                                         new NullTCServerInfo());
    server.start();

    makeClientUsePort(server.getListenPort());

    L1TVSConfigurationSetupManager manager = super.createL1ConfigManager();
    DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelper(manager);

    PreparedComponentsFromL2Connection components = new PreparedComponentsFromL2Connection(manager);

    client = new DistributedObjectClient(configHelper, new TCThreadGroup(new ThrowableHandler(TCLogging
        .getLogger(DistributedObjectClient.class))), new MockClassProvider(), components, NullManager.getInstance());
    client.start();

    lockManager = (ClientLockManagerImpl) client.getLockManager();
  }

  protected void tearDown() {
    if (client != null) {
      client.stop();
    }

    if (server != null) {
      server.stop();
    }
  }

  public void testUpgradeDeadlock() throws Exception {
    final LockID l1 = new LockID("1");

    final ThreadID tid1 = new ThreadID(1);
    final ThreadID tid2 = new ThreadID(2);

    lockManager.lock(l1, tid1, LockLevel.READ);
    lockManager.lock(l1, tid2, LockLevel.READ);

    Thread t1 = new Thread() {
      public void run() {
        LockManagerSystemTest.this.lockManager.lock(l1, tid1, LockLevel.WRITE);
      }
    };

    Thread t2 = new Thread() {
      public void run() {
        LockManagerSystemTest.this.lockManager.lock(l1, tid2, LockLevel.WRITE);
      }
    };

    t1.start();
    t2.start();

    t1.join(2000);
    t2.join(2000);

    assertTrue(t1.isAlive());
    assertTrue(t2.isAlive());

    // make sure we "see" the deadlock on the server side
    final List deadlocks = new ArrayList();
    DeadlockResults results = new DeadlockResults() {
      public void foundDeadlock(DeadlockChain chain) {
        deadlocks.add(chain);
      }
    };

    server.getContext().getLockManager().scanForDeadlocks(results);

    assertEquals(1, deadlocks.size());
    DeadlockChain chain = (DeadlockChain) deadlocks.remove(0);

    ThreadID id1 = chain.getWaiter().getClientThreadID();
    LockID lid1 = chain.getWaitingOn();
    chain = chain.getNextLink();
    ThreadID id2 = chain.getWaiter().getClientThreadID();
    LockID lid2 = chain.getWaitingOn();

    assertEquals(id1, chain.getNextLink().getWaiter().getClientThreadID());

    assertEquals(lid1, l1);
    assertEquals(lid2, l1);
    assertEquals(lid1, lid2);

    if (id1.equals(tid1)) {
      assertEquals(id2, tid2);
    } else {
      assertEquals(id1, tid2);
    }

  }

  private static void sleep(long amount) {
    amount *= (slow ? 300 : 50);
    ThreadUtil.reallySleep(amount);
  }

  public void testUpgrade() throws Exception {
    final LockID l1 = new LockID("1");

    final ThreadID tid1 = new ThreadID(1);
    final ThreadID tid2 = new ThreadID(2);
    final ThreadID tid3 = new ThreadID(3);

    final SetOnceFlag flag = new SetOnceFlag();
    lockManager.lock(l1, tid1, LockLevel.READ);
    lockManager.lock(l1, tid2, LockLevel.READ);
    lockManager.lock(l1, tid3, LockLevel.READ);

    Thread t = new Thread() {
      public void run() {
        LockManagerSystemTest.this.lockManager.lock(l1, tid1, LockLevel.WRITE);
        flag.set();
      }
    };
    t.start();

    sleep(5);
    assertFalse(flag.isSet());

    lockManager.unlock(l1, tid2);
    sleep(5);
    assertFalse(flag.isSet());

    lockManager.unlock(l1, tid3);
    sleep(5);
    assertTrue(flag.isSet());

    t.join();

    Thread secondReader = new Thread() {
      public void run() {
        System.out.println("Read requested !");
        LockManagerSystemTest.this.lockManager.lock(l1, tid2, LockLevel.READ);
        System.out.println("Got Read !");
      }
    };
    secondReader.start();

    Thread secondWriter = new Thread() {
      public void run() {
        System.out.println("Write requested !");
        LockManagerSystemTest.this.lockManager.lock(l1, tid3, LockLevel.WRITE);
        System.out.println("Got Write !");
      }
    };
    secondWriter.start();

    sleep(5);
    lockManager.unlock(l1, tid1);
    sleep(5);
    secondReader.join(5000);
    assertFalse(secondReader.isAlive());
    assertTrue(secondWriter.isAlive());

    lockManager.unlock(l1, tid1);
    sleep(5);
    assertTrue(secondWriter.isAlive());
    lockManager.unlock(l1, tid2);
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
    lockManager.lock(l1, tid1, LockLevel.WRITE);

    System.out.println("Got first lock");

    // Try to get it again, this should pretty much be a noop as we handle recursive lock calls
    lockManager.lock(l1, tid1, LockLevel.WRITE);
    System.out.println("Got first lock again");

    final boolean[] done = new boolean[2];

    // try obtaining a write lock on l1 in a second thread. This should block initially since a write lock is already
    // held on l1
    Thread t = new Thread() {
      public void run() {
        System.out.println("Asked for second lock");
        lockManager.lock(l1, tid2, LockLevel.WRITE);
        System.out.println("Got second lock");
        done[0] = true;
      }
    };

    t.start();
    sleep(5);
    assertFalse(done[0]);
    lockManager.unlock(l1, tid1);
    lockManager.unlock(l1, tid1); // should unblock thread above
    sleep(5);
    assertTrue(done[0]); // thread should have been unblocked and finished

    // Get a bunch of read locks on l3
    lockManager.lock(l3, tid1, LockLevel.READ);
    lockManager.lock(l3, tid2, LockLevel.READ);
    lockManager.lock(l3, tid3, LockLevel.READ);
    done[0] = false;
    t = new Thread() {
      public void run() {
        System.out.println("Asking for write lock");
        lockManager.lock(l3, tid4, LockLevel.WRITE);
        System.out.println("Got write lock");
        done[0] = true;
      }
    };
    t.start();
    sleep(5);
    assertFalse(done[0]);

    lockManager.unlock(l3, tid1);
    sleep(5);
    assertFalse(done[0]);

    lockManager.unlock(l3, tid2);
    sleep(5);
    assertFalse(done[0]);

    lockManager.unlock(l3, tid3);
    sleep(5);
    assertTrue(done[0]);

    done[0] = false;
    t = new Thread() {
      public void run() {
        System.out.println("Asking for read lock");
        lockManager.lock(l3, tid1, LockLevel.READ);
        System.out.println("Got read lock");
        done[0] = true;
      }
    };
    t.start();

    done[1] = false;
    t = new Thread() {
      public void run() {
        System.out.println("Asking for read lock");
        lockManager.lock(l3, tid2, LockLevel.READ);
        System.out.println("Got read lock");
        done[1] = true;
      }
    };

    t.start();
    sleep(5);
    assertFalse(done[0]);
    assertFalse(done[1]);
    lockManager.unlock(l3, tid4);
    sleep(5);
    assertTrue(done[0]);
    assertTrue(done[1]);
    lockManager.unlock(l3, tid1);
    lockManager.unlock(l3, tid2);
  }

  private static class ConfigOverride implements L2TVSConfigurationSetupManager {

    private final L2TVSConfigurationSetupManager realConfig;

    ConfigOverride(L2TVSConfigurationSetupManager realConfig) {
      this.realConfig = realConfig;
    }

    public String[] allCurrentlyKnownServers() {
      return realConfig.allCurrentlyKnownServers();
    }

    public String[] applicationNames() {
      return realConfig.applicationNames();
    }

    public NewCommonL2Config commonl2Config() {
      return realConfig.commonl2Config();
    }

    public NewCommonL2Config commonL2ConfigFor(String name) throws ConfigurationSetupException {
      return realConfig.commonL2ConfigFor(name);
    }

    public String describeSources() {
      return realConfig.describeSources();
    }

    public NewDSOApplicationConfig dsoApplicationConfigFor(String applicationName) {
      return realConfig.dsoApplicationConfigFor(applicationName);
    }

    public NewL2DSOConfig dsoL2Config() {
      return new L2ConfigOverride(realConfig.dsoL2Config());
    }

    public NewL2DSOConfig dsoL2ConfigFor(String name) throws ConfigurationSetupException {
      return realConfig.dsoL2ConfigFor(name);
    }

    public InputStream rawConfigFile() {
      return realConfig.rawConfigFile();
    }

    public NewSystemConfig systemConfig() {
      return realConfig.systemConfig();
    }

    private static class L2ConfigOverride implements NewL2DSOConfig {

      private final NewL2DSOConfig config;

      public L2ConfigOverride(NewL2DSOConfig config) {
        this.config = config;
      }

      public void changesInItemForbidden(ConfigItem item) {
        config.changesInItemForbidden(item);
      }

      public void changesInItemIgnored(ConfigItem item) {
        config.changesInItemIgnored(item);
      }

      public IntConfigItem clientReconnectWindow() {
        return config.clientReconnectWindow();
      }

      public BooleanConfigItem garbageCollectionEnabled() {
        return config.garbageCollectionEnabled();
      }

      public IntConfigItem garbageCollectionInterval() {
        return config.garbageCollectionInterval();
      }

      public BooleanConfigItem garbageCollectionVerbose() {
        return config.garbageCollectionVerbose();
      }

      public IntConfigItem listenPort() {
        return new IntConfigItem() {
          public int getInt() {
            return 0;
          }

          public void addListener(ConfigItemListener changeListener) {
            //
          }

          public Object getObject() {
            return new Integer(0);
          }

          public void removeListener(ConfigItemListener changeListener) {
            //
          }
        };
      }

      public ConfigItem persistenceMode() {
        return config.persistenceMode();
      }

    }

  }

}
