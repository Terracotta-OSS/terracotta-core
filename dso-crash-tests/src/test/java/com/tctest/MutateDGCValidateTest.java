/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.test.schema.InstrumentedClassConfigBuilderImpl;
import com.tc.config.test.schema.L2ConfigBuilder;
import com.tc.config.test.schema.LockConfigBuilderImpl;
import com.tc.config.test.schema.RootConfigBuilderImpl;
import com.tc.config.test.schema.TerracottaConfigBuilder;
import com.tctest.MutateDGCValidateTestApp.WorkOnList;

public class MutateDGCValidateTest extends ServerCrashingTestBase {
  private static final int NODE_COUNT = 1;

  public MutateDGCValidateTest() {
    super(NODE_COUNT);
    timebombTestForRewrite();
  }

  @Override
  protected Class getApplicationClass() {
    return MutateDGCValidateTestApp.class;
  }

  @Override
  protected void createConfig(TerracottaConfigBuilder out) {
    out.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);

    String testClassName = MutateDGCValidateTestApp.class.getName();
    String clientClassName = WorkOnList.class.getName();

    LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock1.setMethodExpression("* " + testClassName + "*.*(..)");
    lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock2.setMethodExpression("* " + clientClassName + "*.*(..)");
    lock2.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    out.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2 });

    RootConfigBuilder root = new RootConfigBuilderImpl();
    root.setFieldName(testClassName + ".rootList");
    root.setRootName("rootList");

    out.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root });

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(clientClassName + "*");

    out.getApplication().getDSO()
        .setInstrumentedClasses(new InstrumentedClassConfigBuilder[] { instrumented1, instrumented2 });
  }
}