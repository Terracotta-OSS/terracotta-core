/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tc.config.schema.builder.InstrumentedClassConfigBuilder;
import com.tc.config.schema.builder.LockConfigBuilder;
import com.tc.config.schema.builder.RootConfigBuilder;
import com.tc.config.schema.test.InstrumentedClassConfigBuilderImpl;
import com.tc.config.schema.test.LockConfigBuilderImpl;
import com.tc.config.schema.test.RootConfigBuilderImpl;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tctest.ServerCrashingTestBase;
import com.tctest.restart.system.ClientTerminatingTestApp.Client;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;

public class ClientTerminatingTest extends ServerCrashingTestBase {

  private static final int NODE_COUNT = 2;

  private File             configFile;
  private int              port;

  private int              adminPort;

  public ClientTerminatingTest() {
    super(NODE_COUNT);
  }

  protected Class getApplicationClass() {
    return ClientTerminatingTestApp.class;
  }

  protected void createConfig(TerracottaConfigBuilder cb) {
    String testClassName = ClientTerminatingTestApp.class.getName();
    String testClassSuperName = AbstractTransparentApp.class.getName();
    String clientClassName = Client.class.getName();

    LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock1.setMethodExpression("* " + testClassName + ".run(..)");
    lock1.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock2.setMethodExpression("* " + clientClassName + ".execute(..)");
    lock2.setLockLevel(LockConfigBuilder.LEVEL_WRITE);

    cb.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2 });

    RootConfigBuilder root = new RootConfigBuilderImpl();
    root.setFieldName(testClassName + ".queue");
    // root.setRootName("queue");
    cb.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root });

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(testClassSuperName + "*");

    cb.getApplication().getDSO().setInstrumentedClasses(
                                                        new InstrumentedClassConfigBuilder[] { instrumented1,
                                                            instrumented2 });

  }

}
