/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

public class FastReadSlowWriteTestApp extends AbstractTransparentApp {

  public static final int NODE_COUNT  = 10;
  SynchronizedInt         idGenerator = new SynchronizedInt(0);

  public FastReadSlowWriteTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

    TransparencyClassSpec thisSpec = config.getOrCreateSpec(FastReadSlowWriteTestApp.class.getName());
    thisSpec.addRoot("idGenerator", "idGenerator");
    new SynchronizedIntSpec().visit(visitor, config);

    TransparencyClassSpec readerSpec = config.getOrCreateSpec("com.tctest.TestReader");
    TransparencyClassSpec writerSpec = config.getOrCreateSpec("com.tctest.TestWriter");

    readerSpec.addRoot("stuff", "rootBabyRoot");
    writerSpec.addRoot("stuff", "rootBabyRoot");
    config.addReadAutolock("* com.tctest.TestReader.*(..)");
    config.addWriteAutolock("* com.tctest.TestWriter.*(..)");
  }

  public void run() {
    int myId = idGenerator.increment();
    if (myId % 5 == 1) {
      new TestWriter().write();
    } else {
      new TestReader("" + myId).read();
    }

  }

}
