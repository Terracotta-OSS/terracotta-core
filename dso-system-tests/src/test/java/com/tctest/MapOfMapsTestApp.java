/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.StringUtil;
import com.tc.util.runtime.Os;
import com.tctest.builtin.ArrayList;
import com.tctest.builtin.AtomicInteger;
import com.tctest.builtin.CyclicBarrier;
import com.tctest.builtin.HashMap;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BrokenBarrierException;

public class MapOfMapsTestApp extends AbstractErrorCatchingTransparentApp {

  private static int         LOOP_COUNT   = 20;
  private static int         DEPTH_COUNT  = 5;
  private static int         BREATH_COUNT = 3;

  public static final String OFFHEAP      = "offheap";

  final Map                  root         = new HashMap();
  final AtomicInteger        uid          = new AtomicInteger(0);
  CyclicBarrier              barrier;

  public MapOfMapsTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    if (Os.isSolaris()) {
      LOOP_COUNT = 10;
    }

    if ("true".equals(cfg.getAttribute(OFFHEAP))) {
      LOOP_COUNT = 10;
      DEPTH_COUNT = 2;
      BREATH_COUNT = 1;
    }
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    final String testClass = MapOfMapsTestApp.class.getName();
    final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("root", "root");
    spec.addRoot("uid", "uid");
    spec.addRoot("barrier", "barrier");
    String methodExpression = "* " + testClass + ".read(..)";
    config.addReadAutolock(methodExpression);
    methodExpression = "* " + testClass + ".countMaps(..)";
    config.addReadAutolock(methodExpression);
    methodExpression = "* " + testClass + ".add2Root(..)";
    config.addWriteAutolock(methodExpression);
    methodExpression = "* " + testClass + ".populateMyRoot(..)";
    config.addWriteAutolock(methodExpression);
  }

  @Override
  public void runTest() throws InterruptedException, BrokenBarrierException {
    setCyclicBarrier();
    final int myid = this.uid.incrementAndGet();
    if (myid == 1) {
      // Writer
      runCreateMaps();
    } else {
      // readers
      runReadMaps();
    }
  }

  private void runReadMaps() throws InterruptedException, BrokenBarrierException {
    int count = 0;
    final int mapCountNo = calculateMapCount(DEPTH_COUNT, BREATH_COUNT);
    while (count++ < LOOP_COUNT) {
      this.barrier.await();
      log("Readers : Loop Count : " + count);
      final Map myRoot = read(String.valueOf(count), this.root);
      final int mapCount = countMaps(myRoot);
      log("Readers : No Of Maps = " + mapCount);
      Assert.assertEquals(mapCountNo, mapCount);
    }
  }

  private int calculateMapCount(final int depth, final int breath) {
    int pow = breath;
    int count = 0;
    for (int i = 0; i <= depth; i++) {
      count += pow;
      pow *= breath;
    }
    return count;
  }

  private int countMaps(final Map m) {
    int count = 0;
    synchronized (m) {
      for (final Iterator i = m.entrySet().iterator(); i.hasNext();) {
        final Map.Entry e = (Entry) i.next();
        if (e.getValue() instanceof Map) {
          count = count + 1 + countMaps((Map) e.getValue());
        }
      }
    }
    return count;
  }

  private void runCreateMaps() throws InterruptedException, BrokenBarrierException {
    int count = 0;
    while (count++ < LOOP_COUNT) {
      log("Writer : Loop Count : " + count);
      final Map myRoot = new HashMap();
      add2Root(String.valueOf(count), myRoot);
      populateMyRoot(myRoot, DEPTH_COUNT, BREATH_COUNT);
      this.barrier.await();
    }
  }

  private void populateMyRoot(final Map myRoot, int depth, final int breath) {
    final List childs = new ArrayList();
    int b = breath;
    synchronized (myRoot) {
      while (b-- > 0) {
        childs.add(addToMap(String.valueOf(b), myRoot));
      }
      myRoot.put(Integer.valueOf(10), "Testing");
    }
    synchronized (myRoot) {
      myRoot.remove(Integer.valueOf(10));
    }
    if (depth-- > 0) {
      for (final Iterator i = childs.iterator(); i.hasNext();) {
        final Map child = (Map) i.next();
        populateMyRoot(child, depth, breath);
      }
    }
  }

  private void setCyclicBarrier() {
    final int participationCount = getParticipantCount();
    log("Participation Count = " + participationCount);
    this.barrier = new CyclicBarrier(participationCount);
  }

  private void add2Root(final String id, final Map myRoot) {
    synchronized (this.root) {
      this.root.put(id, myRoot);
    }
  }

  static DateFormat formatter = new SimpleDateFormat("hh:mm:ss,S");

  private static void log(final String message) {
    System.err.println(Thread.currentThread().getName() + " :: "
                       + formatter.format(new Date(System.currentTimeMillis())) + " : " + message);
  }

  private Map addToMap(final String id, final Map m) {
    final Map child = getChild(m);
    m.put(id, child);
    return child;
  }

  private Map getChild(final Map m) {
    return getPopulatedMap(new HashMap());
  }

  private Map getPopulatedMap(final Map m) {
    for (int i = 0; i < 10; i++) {
      m.put(StringUtil.reduce("Hello - " + i),
            StringUtil.reduce("Hehehehehehehehehehehehehehehehehheheheheheheheheheheheheheheheheh-" + i));
    }
    return m;
  }

  private Map read(final String id, final Map m) {
    synchronized (m) {
      return (Map) m.get(id);
    }
  }

}
