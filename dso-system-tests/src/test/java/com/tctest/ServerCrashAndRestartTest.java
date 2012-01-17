/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.test.schema.InstrumentedClassConfigBuilderImpl;
import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.LockConfigBuilderImpl;
import com.tc.config.test.schema.RootConfigBuilderImpl;
import com.tc.config.test.schema.TerracottaConfigBuilder;

public class ServerCrashAndRestartTest extends ServerCrashingTestBase {

  private static final int NODE_COUNT = 5;

  public ServerCrashAndRestartTest() {
    super(NODE_COUNT);
  }

  @Override
  protected Class getApplicationClass() {
    return ServerCrashAndRestartTestApp.class;
  }

  @Override
  protected void createConfig(TerracottaConfigBuilder cb) {
    // persistent mode
    cb.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);

    // locks
    LockConfigBuilder[] locks = new LockConfigBuilder[] {
        new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK, CyclicBarrier.class, LockConfigBuilder.LEVEL_WRITE),
        new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK, SynchronizedInt.class, LockConfigBuilder.LEVEL_WRITE),
        new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK, getApplicationClass(), LockConfigBuilder.LEVEL_WRITE) };

    cb.getApplication().getDSO().setLocks(locks);

    // include classes
    InstrumentedClassConfigBuilder[] instrClasses = new InstrumentedClassConfigBuilder[] {
        new InstrumentedClassConfigBuilderImpl(CyclicBarrier.class),
        new InstrumentedClassConfigBuilderImpl(SynchronizedInt.class),
        new InstrumentedClassConfigBuilderImpl(getApplicationClass()) };

    cb.getApplication().getDSO().setInstrumentedClasses(instrClasses);

    // roots
    RootConfigBuilder[] roots = new RootConfigBuilder[] { new RootConfigBuilderImpl(getApplicationClass(), "barrier") };
    cb.getApplication().getDSO().setRoots(roots);
  }

}
