/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class CloneTestApp extends AbstractTransparentApp {

  public static final int NODE_COUNT = 2;

  private MyStuff       root;
  private CyclicBarrier barrier     = new CyclicBarrier(NODE_COUNT);
  private int           nodeCount[] = new int[1];

  public CloneTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    int myId = 0;
    synchronized (nodeCount) {
      myId = nodeCount[0]++;
    }

    MyStuff copy = null;
    if (myId == 0) {
      MyStuff s = new MyStuff(null);
      s.map = new HashMap();
      s.map.put(s, s);
      s.one = 2;
      s.list = new ArrayList();
      s.list.add(s.map);
      s.set = new HashSet();
      s.set.add(s.list);
      s.date = new Date();
      s.myStuffArray = new MyStuff[10];
      s.shortArray = new short[200];
      s.myStuffArray[3] = s;
      s.myStuffArray[9] = new MyStuff(null);
      s.shortArray[5] = 4;

      root = new MyStuff(s);

      synchronized (root) {
        s.obj1 = root;
        root.one = 1;
        root.date = s.date;
      }

      test1();
    }
    System.out.println("NODE:" + myId + " is about to enter");
    barrier();

    if (myId == 0) {
      ManagerUtil.optimisticBegin();
      synchronized (root) {
        copy = (MyStuff) ManagerUtil.deepCopy(root);
      }
    }
    barrier();
    if (myId == 0) {
      copy.obj1.one = 250;
      copy.obj1.map.put("steve", "steve");
      copy.obj1.map.put("doh", copy);
      copy.obj1.myStuffArray[7] = new MyStuff(null);
      copy.obj1.myStuffArray[7].one = 7;
      copy.obj1.myStuffArray[3] = copy;

      // checking primitive array access
      short[] tmpShortArray = new short[20];
      Arrays.fill(tmpShortArray, (short) 99);
      copy.shortArray = new short[20];
      System.arraycopy(tmpShortArray, 0, copy.shortArray, 0, 20);
      copy.shortArray[1] = 5;
      copy.shortArray[2] = 6;

      // checking object array access
      MyStuff[] tmpMyStuffArray = new MyStuff[3];
      for (int i = 0; i < tmpMyStuffArray.length; i++) {
        tmpMyStuffArray[i] = new MyStuff(null);
        tmpMyStuffArray[i].one = 100 + i;
      }
      copy.myStuffArray = new MyStuff[10];
      System.arraycopy(tmpMyStuffArray, 0, copy.myStuffArray, 4, 3);
      copy.myStuffArray[5] = new MyStuff(null);
      copy.myStuffArray[5].one = 5;
      copy.myStuffArray[6] = new MyStuff(null);
      copy.myStuffArray[6].one = 6;
    }
    barrier();
    if (myId == 1) {
      synchronized (root) {
        root.obj1.one = 150;  // shouldn't commit fail because of this change?
      }
    }
    barrier();
    if (myId == 0) {
      ManagerUtil.beginLock("test", LockLevel.WRITE);
      ManagerUtil.optimisticCommit();
      ManagerUtil.commitLock("test");
    }
    barrier();
    System.out.println("NODE:" + myId + " is about to exit");
    synchronized (root) {
      System.out.println("NODE:" + myId + " Value:" + root.obj1.one);
      Assert.eval(root.obj1.one == 250);
      Assert.eval(root.obj1.myStuffArray[7] != null);
      Assert.eval(root.obj1.myStuffArray[7].one == 7);
      Assert.eval(root.obj1.map.get("steve").equals("steve"));
      Assert.eval(root.obj1.map.get("doh") == root);

      // verify primitive array content
      Assert.eval(root.shortArray[0] == 99);
      Assert.eval(root.shortArray[1] == 5);
      Assert.eval(root.shortArray[2] == 6);
      Assert.eval(root.shortArray[3] == 99);
      Assert.eval(root.shortArray[4] == 99);

      // verify object array content
      Assert.eval(root.myStuffArray[4] != null);
      Assert.eval(root.myStuffArray[4].one == 100);
      Assert.eval(root.myStuffArray[5] != null);
      Assert.eval(root.myStuffArray[5].one == 5);
      Assert.eval(root.myStuffArray[6] != null);
      Assert.eval(root.myStuffArray[6].one == 6);
    }
  }

  private void test1() {
    ManagerUtil.optimisticBegin();

    MyStuff copy = (MyStuff) ManagerUtil.deepCopy(root);
    Assert.eval(copy != root);
    Assert.eval(copy.obj1 != root.obj1);

    System.out.println("Root:" + root);
    System.out.println("Root obj:" + root.obj1 + " one:" + root.one);
    System.out.println("sub Root obj:" + root.obj1.obj1 + " one:" + root.obj1.one);

    System.out.println("Copy:" + copy);
    System.out.println("Copy obj:" + copy.obj1 + " one:" + copy.one);
    System.out.println("sub Copy obj:" + copy.obj1.obj1 + " one:" + copy.obj1.one);

    Assert.eval(copy == copy.obj1.obj1);
    Assert.eval(copy.inner != root.inner);
    Assert.eval(copy.inner != null);
    Assert.eval(copy.one == 1);
    Assert.eval(copy.obj1.one == 2);
    Assert.eval(copy.obj1.map != root.obj1.map);
    Assert.eval(copy.obj1.myStuffArray[3] == copy.obj1);
    Assert.eval(copy.obj1.myStuffArray[4] == null);
    Assert.eval(copy.obj1.myStuffArray[2] == null);
    Assert.eval(copy.obj1.myStuffArray != root.obj1.myStuffArray);

    Assert.eval(copy.obj1.shortArray[4] != 4);
    Assert.eval(copy.obj1.shortArray[5] == 4);
    Assert.eval(copy.obj1.shortArray[6] != 4);
    Assert.eval(copy.obj1.shortArray != root.obj1.shortArray);

    int size = 0;
    for (Iterator i = copy.obj1.map.entrySet().iterator(); i.hasNext();) {
      size++;
      Entry e = (Entry) i.next();
      Assert.eval(e.getValue() == copy.obj1);
      Assert.eval(e.getKey() == copy.obj1);

      Assert.eval(e.getValue() != root.obj1.map);
      Assert.eval(e.getKey() != root.obj1);
    }

    Assert.eval(size == 1);

    Assert.eval(copy.obj1.list != root.obj1.list);
    Assert.eval(copy.obj1.set != root.obj1.set);
    Assert.eval(copy.obj1.set.size() == root.obj1.set.size());
    Assert.eval(copy.obj1.set.iterator().next() == copy.obj1.list);

    Assert.eval(copy.obj1.list.size() == root.obj1.list.size());
    Assert.eval(copy.obj1.list.iterator().next() == copy.obj1.map);
    Assert.eval((copy.obj1.date != root.obj1.date));
    Assert.eval((copy.obj1.date.equals(root.obj1.date)));
    Assert.eval(copy.obj1.date == copy.date);

    // make changes on copy
    Date d = new Date();
    copy.obj1.map.put("date", d);
    copy.obj1.map.put(copy, d);
    copy.date = d;
    copy.one = 500;

    Assert.eval(root.date != d);
    Assert.eval(!root.obj1.map.containsKey("date"));
    Assert.eval(root.one != 500);

    ManagerUtil.beginLock("test", LockLevel.WRITE);
    ManagerUtil.optimisticCommit();
    ManagerUtil.commitLock("test");

    Assert.eval(root.date == d);
    Assert.eval(root.one == 500);
    Assert.eval(root.obj1.map.get("date") == d);
  }

  private void barrier() {
    try {
      barrier.barrier();
    } catch (InterruptedException ie) {
      throw new AssertionError();
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = CloneTestApp.class.getName();

    config.getOrCreateSpec(testClass) //
        .addRoot("barrier", "barrier") //
        .addRoot("root", "root") //
        .addRoot("nodeCount", "nodeCount");

    config.getOrCreateSpec(MyStuff.class.getName());
    config.getOrCreateSpec(MyStuff.MyInner.class.getName());

    config.addWriteAutolock("* " + testClass + "*.*(..)");
    new CyclicBarrierSpec().visit(visitor, config);

  }

  private static class MyStuff {
    public MyStuff   obj1;
    public long      one   = 1;
    public Map       map;
    public List      list;
    public Set       set;
    public Date      date;
    public Timestamp timestamp;
    public MyStuff[] myStuffArray;
    public short[]   shortArray;
    public MyInner   inner = new MyInner();

    public MyStuff(MyStuff stuff) {
      this.obj1 = stuff;
    }

    private class MyInner {
      public int two;
    }
  }
}
