/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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

public class ClientTerminatingTest extends ServerCrashingTestBase {

  private static final int NODE_COUNT         = 2;

  private boolean          isSynchronousWrite = false;

  public ClientTerminatingTest() {
    super(NODE_COUNT);
  }

  protected void setSynchronousWrite() {
    isSynchronousWrite = true;
  }

  protected Class getApplicationClass() {
    ClientTerminatingTestApp.setSynchronousWrite(isSynchronousWrite);
    return ClientTerminatingTestApp.class;
  }

  protected void createConfig(TerracottaConfigBuilder cb) {
    String testClassName = ClientTerminatingTestApp.class.getName();
    String clientClassName = Client.class.getName();

    LockConfigBuilder lock1 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock1.setMethodExpression("* " + testClassName + "*.*(..)");
    setLockLevel(lock1);

    LockConfigBuilder lock2 = new LockConfigBuilderImpl(LockConfigBuilder.TAG_AUTO_LOCK);
    lock2.setMethodExpression("* " + clientClassName + "*.*(..)");
    setLockLevel(lock2);

    cb.getApplication().getDSO().setLocks(new LockConfigBuilder[] { lock1, lock2 });

    RootConfigBuilder root = new RootConfigBuilderImpl();
    root.setFieldName(testClassName + ".queue");
    root.setRootName("queue");
    RootConfigBuilder root2 = new RootConfigBuilderImpl();
    root2.setFieldName(clientClassName + ".clientQueue");
    root2.setRootName("queue");
    cb.getApplication().getDSO().setRoots(new RootConfigBuilder[] { root, root2 });

    InstrumentedClassConfigBuilder instrumented1 = new InstrumentedClassConfigBuilderImpl();
    instrumented1.setClassExpression(testClassName + "*");

    InstrumentedClassConfigBuilder instrumented2 = new InstrumentedClassConfigBuilderImpl();
    instrumented2.setClassExpression(clientClassName + "*");

    cb.getApplication().getDSO().setInstrumentedClasses(
                                                        new InstrumentedClassConfigBuilder[] { instrumented1,
                                                            instrumented2 });
  }

  private void setLockLevel(LockConfigBuilder lock) {
    if (isSynchronousWrite) {
      lock.setLockLevel(LockConfigBuilder.LEVEL_SYNCHRONOUS_WRITE);
    } else {
      lock.setLockLevel(LockConfigBuilder.LEVEL_WRITE);
    }
  }

}
