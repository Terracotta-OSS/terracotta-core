/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class JavaUtilConcurrentCloneTestApp extends AbstractTransparentApp {

  public static final int NODE_COUNT  = 2;

  private MyStuff         root;
  private CyclicBarrier   barrier     = new CyclicBarrier(NODE_COUNT);
  private int             nodeCount[] = new int[1];

  public JavaUtilConcurrentCloneTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    try {
      int myId = 0;
      synchronized (nodeCount) {
        myId = nodeCount[0]++;
      }
      MyStuff copy = null;
      if (myId == 0) {
        MyStuff s = new MyStuff(null);
        s.linkedBlockingQueue = new LinkedBlockingQueue();
        s.linkedBlockingQueue.put("first element");

        s.map = new ConcurrentHashMap();
        MapInnerObject innerObject = new MapInnerObject(true);
        MapObject mapObject = new MapObject(10, innerObject);
        //s.map.put(mapObject, mapObject);
        s.map.put(s, s);

        s.linkedBlockingQueue.put(s.map);
        s.one = 2;
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
          root.mapObject = mapObject;
        }
        ManagerUtil.optimisticBegin();
        copy = (MyStuff) ManagerUtil.deepCopy(root);

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
        }

        Assert.eval(size == 1);

        Assert.eval((copy.obj1.date != root.obj1.date));
        Assert.eval((copy.obj1.date.equals(root.obj1.date)));
        Assert.eval(copy.obj1.date == copy.date);

        Assert.eval(copy.obj1.linkedBlockingQueue.size() == root.obj1.linkedBlockingQueue.size());
        Assert.eval(copy.obj1.linkedBlockingQueue.poll().equals("first element"));
        Assert.eval(copy.obj1.linkedBlockingQueue.poll() == copy.obj1.map);

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
      }
      System.out.println("NODE:" + myId + " is about to enter");
      barrier();
      if (myId == 0) {
        ManagerUtil.optimisticBegin();
        synchronized ("test") {
          copy = (MyStuff) ManagerUtil.deepCopy(root);
        }
      }
      barrier();
      if (myId == 0) {
        copy.obj1.one = 250;
        copy.obj1.map.put("steve", "steve");
        copy.obj1.map.put("doh", copy);
        Assert.eval(copy != null);
        copy.obj1.myStuffArray[7] = new MyStuff(null);
        copy.obj1.myStuffArray[7].one = 7;
        copy.obj1.myStuffArray[3] = copy;
      }
      barrier();
      if (myId == 1) {
        synchronized ("test") {
          root.obj1.one = 150;
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
      synchronized ("test") {
        System.out.println("NODE:" + myId + " Value:" + root.obj1.one);
        Assert.eval(root.obj1.one == 250);
        Assert.eval(root.obj1.myStuffArray[7] != null);
        Assert.eval(root.obj1.myStuffArray[7].one == 7);
      }
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void barrier() {

    try {
      barrier.barrier();
    } catch (InterruptedException ie) {
      throw new AssertionError();
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = JavaUtilConcurrentCloneTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("root", "root");
    spec.addRoot("nodeCount", "nodeCount");
    spec = config.getOrCreateSpec(MyStuff.class.getName());
    spec = config.getOrCreateSpec(MyStuff.MyInner.class.getName());
    spec = config.getOrCreateSpec(MapObject.class.getName());
    spec = config.getOrCreateSpec(MapInnerObject.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    new CyclicBarrierSpec().visit(visitor, config);

  }

  private static class MyStuff {
    public MyStuff             obj1;
    public long                one   = 1;
    public LinkedBlockingQueue linkedBlockingQueue;
    public ConcurrentHashMap   map;
    public Date                date;
    public Timestamp           timestamp;
    public MyStuff[]           myStuffArray;
    public short[]             shortArray;
    public MyInner             inner = new MyInner();
    public MapObject           mapObject;

    public MyStuff(MyStuff stuff) {
      this.obj1 = stuff;
    }

    private class MyInner {
      public int two;
    }
  }
  
  private static class MapObject {
    private int i;
    private MapInnerObject innerObject;
    
    public MapObject(int i, MapInnerObject innerObject) {
      this.i = i;
      this.innerObject = innerObject;
    }

    public int getI() {
      return i;
    }

    public MapInnerObject getInnerObject() {
      return innerObject;
    }
  }
  
  private static class MapInnerObject {
    private boolean flag;
    
    public MapInnerObject(boolean flag) {
      this.flag = flag;
    }

    public boolean isFlag() {
      return flag;
    }
  }
}
