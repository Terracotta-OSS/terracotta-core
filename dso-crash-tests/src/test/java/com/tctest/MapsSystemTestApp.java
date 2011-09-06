/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CyclicBarrier;

public class MapsSystemTestApp extends AbstractTransparentApp {
  private ServerControl                         serverControl;
  private CyclicBarrier                         barrier;
  private TreeMap<PartialSetNode, String>       myTreeMap;
  private LinkedHashMap<PartialSetNode, String> myLinkedHashMapInsertionOrder;
  private LinkedHashMap<PartialSetNode, String> myLinkedHashMapAccessOrder;
  private HashMap<PartialSetNode, String>       myHashMap;

  private final static int                      NUMBERS_ADDED = 5000;

  public MapsSystemTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    serverControl = cfg.getServerControl();
    barrier = new CyclicBarrier(getParticipantCount());
    myTreeMap = new TreeMap<PartialSetNode, String>();
    myLinkedHashMapInsertionOrder = new LinkedHashMap<PartialSetNode, String>();
    myLinkedHashMapAccessOrder = new LinkedHashMap<PartialSetNode, String>(1000, (float) 0.75, true);
    myHashMap = new HashMap<PartialSetNode, String>();
  }

  public void run() {
    int index = waitOnBarrier();
    if (index != 0) {
      addElementsToMaps();
    }

    index = waitOnBarrier();
    if (index == 0) restartServer();
    waitOnBarrier();

    validate();
  }

  private void validate() {
    validateAllEntries(myTreeMap);
    validateAllEntries(myLinkedHashMapInsertionOrder);
    validateAllEntries(myLinkedHashMapAccessOrder);
    validateAllEntries(myHashMap);

    validateTreeMap();
    validateLinkedHashMapInsertionOrder();
    validateLinkedHashMapAccessOrder();
  }

  private void validateLinkedHashMapAccessOrder() {
    synchronized (myLinkedHashMapAccessOrder) {
      for (int i = NUMBERS_ADDED - 1; i >= 0; i--) {
        String str = myLinkedHashMapAccessOrder.get(new PartialSetNode(i));
        Assert.assertEquals(String.valueOf(i), str);
      }
    }
    validateInOrder(myLinkedHashMapAccessOrder, false);
  }

  private void validateLinkedHashMapInsertionOrder() {
    validateInOrder(myLinkedHashMapInsertionOrder, true);
  }

  private void validateTreeMap() {
    validateInOrder(myTreeMap, true);
  }

  private void validateInOrder(Map<PartialSetNode, String> map, boolean inOrder) {
    Iterator<PartialSetNode> iter = map.keySet().iterator();
    int count = 0;
    while(iter.hasNext()) {
      PartialSetNode node = iter.next();
      if(inOrder)
        Assert.assertEquals(count, node.getNumber());
      else
        Assert.assertEquals(5000 - count -1, node.getNumber());
      
      count++;
    }
  }

  private void validateAllEntries(Map<PartialSetNode, String> map) {
    synchronized (map) {
      Assert.assertEquals(NUMBERS_ADDED, map.size());
      for (int i = 0; i < NUMBERS_ADDED; i++) {
        String str = map.get(new PartialSetNode(i));
        Assert.assertEquals(String.valueOf(i), str);
      }
    }
  }

  private void addElementsToMaps() {
    addElementsToMap(myTreeMap);
    addElementsToMap(myLinkedHashMapInsertionOrder);
    addElementsToMap(myLinkedHashMapAccessOrder);
    addElementsToMap(myHashMap);
  }

  private void addElementsToMap(Map<PartialSetNode, String> map) {
    synchronized (map) {
      for (int i = 0; i < NUMBERS_ADDED; i++) {
        map.put(new PartialSetNode(i), String.valueOf(i));
      }
    }
  }

  private void restartServer() {
    try {
      System.out.println("Crashing the Server ...");
      serverControl.crash();
      ThreadUtil.reallySleep(5000);
      System.out.println("Re-starting the Server ...");
      serverControl.start();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private int waitOnBarrier() {
    int index = -1;
    try {
      index = barrier.await();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return index;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MapsSystemTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(MapsSystemTestApp.class.getName());
    config.addIncludePattern(PartialSetNode.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";

    spec.addRoot("myTreeMap", "myTreeMap");
    spec.addRoot("myLinkedHashMapInsertionOrder", "myLinkedHashMapInsertionOrder");
    spec.addRoot("myLinkedHashMapAccessOrder", "myLinkedHashMapAccessOrder");
    spec.addRoot("myHashMap", "myHashMap");
    spec.addRoot("barrier", "barrier");
    config.addWriteAutolock(methodExpression);
  }
}