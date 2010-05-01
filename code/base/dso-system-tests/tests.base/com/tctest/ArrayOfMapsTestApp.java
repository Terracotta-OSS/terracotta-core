/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.stats.DSOMBean;
import com.tc.test.GroupData;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.management.remote.JMXConnector;

/**
 * DEV-4047: In the AA world, an Object Lookup can happen before the Object actually gets create at the server. Server
 * shouldn't crash on those scenarios
 */
public class ArrayOfMapsTestApp extends AbstractErrorCatchingTransparentApp {

  public static final String    GROUPS_DATA     = "groups-data";
  public static final short     CLIENT_UPDATERS = 2;
  private final GroupData[]     groupsData;
  private final Map[]           rootMap         = new HashMap[CLIENT_UPDATERS];
  private final SynchronizedInt uid             = new SynchronizedInt(0);
  private ObjectBrowser         objectBrowser;

  public ArrayOfMapsTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    groupsData = (GroupData[]) cfg.getAttributeObject(ArrayOfMapsTestApp.GROUPS_DATA);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ArrayOfMapsTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("rootMap", "rootMap");
    spec.addRoot("uid", "uid");
    spec.addRoot("barrier", "barrier");
    String methodExpression = "* " + testClass + ".runCreateRootMaps(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + testClass + ".runReadRootMaps(..)";
    config.addWriteAutolock(methodExpression);

    config.addIncludePattern(KeyObject.class.getName());
    config.addIncludePattern(ValueObject.class.getName());

    new SynchronizedIntSpec().visit(visitor, config);
    new CyclicBarrierSpec().visit(visitor, config);
  }

  @Override
  public void runTest() throws BrokenBarrierException, InterruptedException, IOException {
    int myid = uid.increment();
    objectBrowser = new ObjectBrowser(this.groupsData[0].getJmxPorts()[0]);
    if (myid == 1) {
      runCreateRootMaps();
      runReadRootMaps(myid);
    } else {
      runReadRootMaps(myid);
    }
  }

  private void runCreateRootMaps() throws BrokenBarrierException {
    for (int i = 0; i < CLIENT_UPDATERS; i++) {
      synchronized (rootMap) {
        rootMap[i] = new HashMap<KeyObject, ValueObject>(50);
        System.out.println("XXX CLient ID 0 : created Map " + i + ": "
                           + ((Manageable) rootMap[i]).__tc_managed().getObjectID());
        rootMap.notify();
      }
    }
  }

  private void runReadRootMaps(int myid) throws InterruptedException {
    Map myMap;
    synchronized (rootMap) {
      myMap = rootMap[myid - 1];
      while (myMap == null) {
        rootMap.wait();
        myMap = rootMap[myid - 1];
      }
    }

    System.out.println("XXX CLient ID " + myid + "; My Map " + ((Manageable) myMap).__tc_managed().getObjectID());

    int count = 1000;
    while (count-- > 0) {
      Object k = new KeyObject(getRandomID());
      Object v = new ValueObject(getRandomID());
      synchronized (myMap) {
        myMap.put(k, v);
      }

      ObjectID keyID = ((Manageable) k).__tc_managed().getObjectID();
      ObjectID valueID = ((Manageable) v).__tc_managed().getObjectID();
      System.out.println("XXX CLient ID " + myid + "; " + k + " ==> " + v + "; " + keyID + " ==> " + valueID);
      try {
        this.objectBrowser.lookup(keyID);
        this.objectBrowser.lookup(valueID);
      } catch (NoSuchObjectException nsoe) {
        System.out.println("XXX " + nsoe);
      }
    }

  }

  Random r = new Random();

  private long getRandomID() {
    r.setSeed(System.currentTimeMillis());
    return r.nextInt(50);
  }

  private static class KeyObject {
    private final long id;

    public KeyObject(final long id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof KeyObject)) return false;
      if (((KeyObject) obj).getId() == this.id) return true;
      return false;
    }

    public long getId() {
      return id;
    }

    @Override
    public String toString() {
      return "K:" + getId();
    }

  }

  private static class ValueObject {
    private final long id;

    public ValueObject(final long id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ValueObject)) return false;
      if (((ValueObject) obj).getId() == this.id) return true;
      return false;
    }

    public long getId() {
      return id;
    }

    @Override
    public String toString() {
      return "V:" + getId();
    }

  }

  private static final class ObjectBrowser {

    private ConnectionContext context;
    private JMXConnector      jmxc;
    private DSOMBean          dsoBean;

    public ObjectBrowser(int jmxPort) throws IOException {
      context = new ConnectionContext("localhost", jmxPort);
      jmxc = new JMXConnectorProxy(context.host, context.port);
      context.jmxc = jmxc;
      context.mbsc = jmxc.getMBeanServerConnection();
      dsoBean = MBeanServerInvocationProxy.newMBeanProxy(context.mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
    }

    public void lookup(ObjectID oID) throws NoSuchObjectException {
      ManagedObjectFacade facade = dsoBean.lookupFacade(oID, 10);

      String[] fieldNames = facade.getFields();
      String s = null;
      for (int j = 0; j < fieldNames.length; j++) {
        s = fieldNames[j] + facade.getFieldValue(fieldNames[j]) + "; ";
      }
      System.out.println("XXX Lookup ObjID " + oID + " : " + facade + "; Fileds: " + s);
    }

  }
}
