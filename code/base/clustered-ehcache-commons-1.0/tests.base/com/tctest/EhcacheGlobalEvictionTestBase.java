/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.L1ConfigBuilder;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tctest.EhcacheGlobalEvictionTestApp.L1Client;


public abstract class EhcacheGlobalEvictionTestBase extends ServerCrashingTestBase {
  private final static int NODE_COUNT = 1;

  public EhcacheGlobalEvictionTestBase() {
    super(NODE_COUNT);
    // disableAllUntil("2007-08-31");
  }

  public void setUp() throws Exception {
    super.setUp();

    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  protected void createConfig(TerracottaConfigBuilder cb) {
    cb.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_TEMPORARY_SWAP_ONLY);

    String testClassName = EhcacheGlobalEvictionTestApp.class.getName();
    String clientClassName = L1Client.class.getName();
    String barrierClassName = CyclicBarrier.class.getName();

    L1ConfigBuilder l1Config = cb.getClient();
    l1Config.addModule("clustered-ehcache-1.2.4", "1.0.0");

    LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock1.setMethodExpression("* " + testClassName + "*.*(..)");
    setLockLevel(lock1);

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock2.setMethodExpression("* " + clientClassName + "*.*(..)");
    setLockLevel(lock2);

    LockConfigBuilder lock3 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock3.setMethodExpression("* " + barrierClassName + "*.*(..)");
    setLockLevel(lock3);

    LockConfigBuilder lock4 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock4.setMethodExpression("* " + getApplicationClass().getName() + "*.*(..)");
    setLockLevel(lock4);

    cb.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2, lock3, lock4 });
    
    RootConfigBuilder root = new RootConfigBuilderImpl();
    root.setFieldName(clientClassName + ".barrier");
    root.setRootName("barrier");
    RootConfigBuilder root2 = new RootConfigBuilderImpl();
    root2.setFieldName(clientClassName + ".cacheManager");
    root2.setRootName("cacheManager");
    cb.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root, root2 });

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(clientClassName + "*");

    InstrumentedClassConfigBuilder instrumented3 = new InstrumentedClassConfigBuilderImpl();
    instrumented3.setClassExpression(barrierClassName + "*");
    
    InstrumentedClassConfigBuilder instrumented4 = new InstrumentedClassConfigBuilderImpl();
    instrumented4.setClassExpression(getApplicationClass().getName() + "*");

    cb.getApplication().getDSO().setInstrumentedClasses(
                                                        new InstrumentedClassConfigBuilder[] { instrumented1,
                                                            instrumented2, instrumented3, instrumented4 });

  }

  protected void setLockLevel(LockConfigBuilder lock) {
    lock.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
  }

  protected void setReadLockLevel(LockConfigBuilder lock) {
    lock.setLockLevel(LockConfigBuilder.LEVEL_READ);
  }

}
