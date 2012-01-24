/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCRuntimeException;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.JMXUtils;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.builtin.ConcurrentHashMap;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.builtin.HashMap;
import com.tctest.builtin.Lock;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

import junit.framework.Assert;

public class RemoveObjectsTestApp extends AbstractTransparentApp {

  private final long                   TEST_DURATION = 5 * 60 * 1000;
  private final CyclicBarrier          barrier;
  private static final Lock            myLock        = new Lock();
  private final ConcurrentHashMap      sharedRoot    = new ConcurrentHashMap();

  private MBeanServerConnection        mbsc          = null;
  private JMXConnector                 jmxc;
  private ObjectManagementMonitorMBean objectMBean;
  private final ApplicationConfig      config;

  public RemoveObjectsTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    Assert.assertEquals(2, cfg.getGlobalParticipantCount());
    this.barrier = new CyclicBarrier(cfg.getGlobalParticipantCount());
    this.config = cfg;
  }

  public void run() {
    int myID = -1;
    try {
      myID = barrier.await();
    } catch (Exception e1) {
      throw new RuntimeException(e1);
    }
    try {
      if (myID == 0) {
        ThreadUtil.reallySleep(10000);
        connect();
        Assert.assertTrue(objectMBean != null);
        int startLiveCount = objectMBean.getAllObjectIds().size();
        long starttime = System.currentTimeMillis();
        while ((starttime + TEST_DURATION) > System.currentTimeMillis()) {
          int liveObjs = objectMBean.getAllObjectIds().size();
          System.out.println("XXX StartLiveObjs:" + startLiveCount + "; NowLiveObjs:" + liveObjs);
          Assert.assertTrue(liveObjs < 2 * startLiveCount);
          ThreadUtil.reallySleep(5000);
        }
      } else {
        long starttime = System.currentTimeMillis();
        while ((starttime + TEST_DURATION) > System.currentTimeMillis()) {
          Map streamData = new HashMap();
          for (int n = 0; n < 10000; n++) {
            String key = String.valueOf(n);
            String value = new String("Value for key " + n);
            streamData.put(key, value);
          }
          myLock.writeLock();
          sharedRoot.put("Data-DEV3462", streamData);
          myLock.writeUnlock();
          System.out.println("Sleeping ");
          try {
            System.gc();
            ThreadUtil.reallySleep(5000);
          } catch (Exception e) {
            System.out.println("XXX Exception Caught " + e);
          }
        }
      }
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  private void connect() throws Exception {
    System.out.println("connecting to jmx server....");
    int jmxPort = Integer.parseInt(config.getAttribute(ApplicationConfig.JMXPORT_KEY));
    jmxc = JMXUtils.getJMXConnector("localhost", jmxPort);
    mbsc = jmxc.getMBeanServerConnection();
    System.out.println("obtained mbeanserver connection");
    objectMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.OBJECT_MANAGEMENT,
                                                                ObjectManagementMonitorMBean.class, false);

  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = RemoveObjectsTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*", false, false, true);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("sharedRoot", "sharedRoot");
    spec.addRoot("myLock", "myLock");
    spec.addRoot("barrier", "barrier");
  }
}
