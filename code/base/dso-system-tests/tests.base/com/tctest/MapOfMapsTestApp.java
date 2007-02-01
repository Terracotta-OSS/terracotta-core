/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class MapOfMapsTestApp extends AbstractErrorCatchingTransparentApp {

  private static final int LOOP_COUNT   = 10;
  private static final int DEPTH_COUNT  = 5;
  private static final int BREATH_COUNT = 3;

  final Map                root         = new HashMap();
  final SynchronizedInt    uid          = new SynchronizedInt(0);
  CyclicBarrier            barrier;

  public MapOfMapsTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MapOfMapsTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("root", "root");
    spec.addRoot("uid", "uid");
    spec.addRoot("barrier", "barrier");
    String methodExpression = "* " + testClass + ".read(..)";
    config.addReadAutolock(methodExpression);
    methodExpression = "* " + testClass + ".add2Root(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + testClass + ".populateMyRoot(..)";
    config.addWriteAutolock(methodExpression);
    new SynchronizedIntSpec().visit(visitor, config);
    new CyclicBarrierSpec().visit(visitor, config);
  }

  public void runTest() throws BrokenBarrierException, InterruptedException {
    setCyclicBarrier();
    int myid = uid.increment();
    if (myid == 1) {
      // Writer
      runCreateMaps();
    } else {
      // readers
      runReadMaps();
    }
  }

  private void runReadMaps() throws BrokenBarrierException, InterruptedException {
    int count = 0;
    int mapCountNo = calculateMapCount(DEPTH_COUNT, BREATH_COUNT);
    while (count++ < LOOP_COUNT) {
      barrier.barrier();
      log("Readers : Loop Count : " + count);
      Map myRoot = read(String.valueOf(count), root);
      int mapCount = countMaps(myRoot);
      log("Readers : No Of Maps = " + mapCount);
      Assert.assertEquals(mapCountNo, mapCount);
    }
  }

  private int calculateMapCount(int depth, int breath) {
    int pow = breath;
    int count = 0;
    for (int i = 0; i <= depth; i++) {
      count += pow;
      pow *= breath;
    }
    return count;
  }

  private int countMaps(Map m) {

    int count = 0;
    for (Iterator i = m.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Entry) i.next();
      if (e.getValue() instanceof Map) {
        count = count + 1 + countMaps((Map) e.getValue());
      }
    }
    return count;
  }

  private void runCreateMaps() throws BrokenBarrierException, InterruptedException {
    int count = 0;
    while (count++ < LOOP_COUNT) {
      log("Writer : Loop Count : " + count);
      Map myRoot = new HashMap();
      add2Root(String.valueOf(count), myRoot);
      populateMyRoot(myRoot, DEPTH_COUNT, BREATH_COUNT);
      barrier.barrier();
    }
  }

  private void populateMyRoot(Map myRoot, int depth, int breath) {
    List childs = new ArrayList();
    int b = breath;
    synchronized (myRoot) {
      while (b-- > 0) {
        childs.add(addToMap(String.valueOf(b), myRoot));
      }
    }
    if (depth-- > 0) {
      for (Iterator i = childs.iterator(); i.hasNext();) {
        Map child = (Map) i.next();
        populateMyRoot(child, depth, breath);
      }
    }
  }

  private void setCyclicBarrier() {
    int participationCount = getParticipantCount();
    log("Participation Count = " + participationCount);
    barrier = new CyclicBarrier(participationCount);
  }

  private void add2Root(String id, Map myRoot) {
    synchronized (root) {
      root.put(id, myRoot);
    }
  }

  static DateFormat formatter = new SimpleDateFormat("hh:mm:ss,S");

  private static void log(String message) {
    System.err.println(Thread.currentThread().getName() + " :: "
                       + formatter.format(new Date(System.currentTimeMillis())) + " : " + message);
  }

  private Map addToMap(String id, Map m) {
    Map child = getChild(m);
    m.put(id, child);
    return child;
  }

  /**
   * HashMap, Hashtable and LinkedHashMap supports partial collection and IdentityHashMap and TreeMap doesnt support it.
   * So both should be tested
   */
  private Map getChild(Map m) {
    if (m instanceof HashMap) {
      return getPopulatedMap(new Hashtable());
    } else if (m instanceof Hashtable) {
      return getPopulatedMap(new LinkedHashMap());
    } else if (m instanceof LinkedHashMap) {
      return getPopulatedMap(new IdentityHashMap());
    } else if (m instanceof IdentityHashMap) {
      return getPopulatedMap(new TreeMap());
    } else if (m instanceof TreeMap) {
      return getPopulatedMap(new HashMap());
    } else {
      throw new AssertionError("Should never get here");
    }
  }

  private Map getPopulatedMap(Map m) {
    for (int i = 0; i < 10; i++) {
      m.put("Hello - " + i, new String("Hehehehehehehehehehehehehehehehehheheheheheheheheheheheheheheheheh-" + i));
    }
    return m;
  }

  private Map read(String id, Map m) {
    synchronized (m) {
      return (Map) m.get(id);
    }
  }

}
