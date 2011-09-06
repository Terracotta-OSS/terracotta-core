/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.util.ReadOnlyException;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class LogicalIteratorTestApp extends AbstractErrorCatchingTransparentApp {

  private final Set           set       = new HashSet();
  private final Map           map       = new HashMap();
  private final List          llist     = new LinkedList();
  private final List          alist     = new ArrayList();
  private final Vector        vector    = new Vector();
  private final Hashtable     hashtable = new Hashtable();
  private final CyclicBarrier barrier;

  public LogicalIteratorTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.barrier = new CyclicBarrier(getParticipantCount());
  }

  protected void runTest() throws Throwable {
    String me = getApplicationId();
    init(me);
    barrier.barrier();
    readOnly();
    barrier.barrier();
    remove(me);
    barrier.barrier();
    verifySize();
  }

  private void verifySize() {
    synchronized (set) {
      if (vector.size() != 0) { throw new RuntimeException("vector not empty: " + vector.size()); }
      if (map.size() != 0) { throw new RuntimeException("map not empty: " + map.size()); }
      if (alist.size() != 0) { throw new RuntimeException("alist not empty" + alist.size()); }
      if (set.size() != 0) { throw new RuntimeException("set not empty: " + set.size()); }
      if (llist.size() != 0) { throw new RuntimeException("llist not empty: " + llist.size()); }
      if (hashtable.size() != 0) { throw new RuntimeException("hashtable not empty: " + hashtable.size()); }
    }
  }

  private void init(String id) {
    synchronized (barrier) {
      vector.add(id);
      map.put("key" + id, id);
      map.put(id, "value" + id);
      alist.add(id);
      set.add(id);
      llist.add(id);
      hashtable.put("key" + id, id);
      hashtable.put(id, "value" + id);
    }
  }

  private void remove(String id) {
    synchronized (barrier) {
      remove(vector.iterator(), id);
      remove(map.keySet().iterator(), "key" + id);
      remove(map.values().iterator(), "value" + id);
      remove(alist.iterator(), id);
      remove(set.iterator(), id);
      remove(llist.iterator(), id);
      remove(hashtable.keySet().iterator(), "key" + id);
      remove(hashtable.values().iterator(), "value" + id);
    }
  }

  private void remove(Iterator iter, String id) {
    while (iter.hasNext()) {
      if (id.equals(iter.next())) {
        iter.remove();
        return;
      }
    }
  }

  private void readOnly() {
    synchronized (barrier) {
      attemptRemove(map.keySet());
      attemptRemove(map.values());
      attemptRemove(alist);
      attemptRemove(set);
      attemptRemove(llist);
    }
  }

  private void attemptRemove(Collection collection) {
    String type = collection.getClass().getName();
    final int startSize = collection.size();

    Iterator iter = collection.iterator();
    iter.next();

    try {
      iter.remove();
      throw new RuntimeException("Iterator remove() allowed in read-only transaction on type " + type);
    } catch (ReadOnlyException roe) {
      // expected
    }

    int endSize = collection.size();
    if (startSize != endSize) {
      // collection size changed
      throw new AssertionError("Collection of type " + type + " changed size during read-only TXN: (" + startSize
                               + " != " + endSize + ")");
    }

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LogicalIteratorTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    new CyclicBarrierSpec().visit(visitor, config);

    String method = "* " + testClass + ".init(..)";
    config.addWriteAutolock(method);

    method = "* " + testClass + ".readOnly(..)";
    config.addAutolock(method, ConfigLockLevel.READ);

    method = "* " + testClass + ".remove(..)";
    config.addWriteAutolock(method);

    method = "* " + testClass + ".runTest()";
    config.addWriteAutolock(method);

    method = "* " + testClass + ".verifySize(..)";
    config.addWriteAutolock(method);

    spec.addRoot("hashtable", "hashtableLock");
    spec.addRoot("set", "setLock");
    spec.addRoot("vector", "vectorLock");
    spec.addRoot("map", "mapLock");
    spec.addRoot("llist", "llistLock");
    spec.addRoot("alist", "alistLock");
    spec.addRoot("barrier", "barrierLock");
  }

}
