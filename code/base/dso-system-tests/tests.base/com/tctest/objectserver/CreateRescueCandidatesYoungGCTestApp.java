/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.objectserver;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class CreateRescueCandidatesYoungGCTestApp extends AbstractErrorCatchingTransparentApp {

  private static final long TEST_DURATION = 10 * 60 * 1000;

  final Set                 root          = new HashSet();
  final Vector              unusedBytes   = new Vector();
  static long               maxDepth      = 2;

  public CreateRescueCandidatesYoungGCTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = CreateRescueCandidatesYoungGCTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("root", "root");
    String methodExpression = "";

    testClass = CreateRescueCandidatesYoungGCTestApp.Node.class.getName();
    spec = config.getOrCreateSpec(testClass);
    config.addTransient(testClass, "transientBytes");

    testClass = CreateRescueCandidatesYoungGCTestApp.PopulateThread.class.getName();
    spec = config.getOrCreateSpec(testClass);
    methodExpression = "* " + testClass + ".run(..)";
    config.addWriteAutolock(methodExpression);

    testClass = CreateRescueCandidatesYoungGCTestApp.RemoveThread.class.getName();
    spec = config.getOrCreateSpec(testClass);
    methodExpression = "* " + testClass + ".run(..)";
    config.addWriteAutolock(methodExpression);

    testClass = CreateRescueCandidatesYoungGCTestApp.DisplaceThread.class.getName();
    spec = config.getOrCreateSpec(testClass);
    methodExpression = "* " + testClass + ".run(..)";
    config.addWriteAutolock(methodExpression);

    testClass = CreateRescueCandidatesYoungGCTestApp.LocalGCThread.class.getName();
    spec = config.getOrCreateSpec(testClass);
    methodExpression = "* " + testClass + ".run(..)";
    config.addWriteAutolock(methodExpression);

  }

  public void runTest() {
    log("App Id = " + getApplicationId() + " participation count = " + getParticipantCount() + " intensity = "
        + getIntensity());

    PopulateThread populateThread = new PopulateThread(root);
    populateThread.start();

    RemoveThread removeThread = new RemoveThread(root);
    removeThread.start();

    DisplaceThread displaceThread = new DisplaceThread(root);
    displaceThread.start();

    LocalGCThread gcThread = new LocalGCThread();
    gcThread.start();

    try {
      Thread.sleep(TEST_DURATION);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

    for (Iterator iter = populateThread.getExceptions().iterator(); iter.hasNext();) {
      Throwable t = (Throwable) iter.next();
      throw new AssertionError(t);
    }

    for (Iterator iter = removeThread.getExceptions().iterator(); iter.hasNext();) {
      Throwable t = (Throwable) iter.next();
      throw new AssertionError(t);
    }

    for (Iterator iter = displaceThread.getExceptions().iterator(); iter.hasNext();) {
      Throwable t = (Throwable) iter.next();
      throw new AssertionError(t);
    }

    for (Iterator iter = gcThread.getExceptions().iterator(); iter.hasNext();) {
      Throwable t = (Throwable) iter.next();
      throw new AssertionError(t);
    }
  }

  static DateFormat formatter = new SimpleDateFormat("hh:mm:ss,S");

  private static void log(String message) {
    System.err.println(Thread.currentThread().getName() + " :: "
                       + formatter.format(new Date(System.currentTimeMillis())) + " : " + message);
  }

  private static final class PopulateThread extends Thread {

    private Set root;

    private Set exceptions = new HashSet();

    public PopulateThread(Set root) {
      this.root = root;
    }

    public synchronized Set getExceptions() {
      return exceptions;
    }

    public void run() {
      try {
        long objectCount = 0;
        while (true) {
          Node rootNode = new Node(objectCount * 100);
          createCicleOfNodes(rootNode, 20);
          synchronized (root) {
            root.add(rootNode);
          }
          if (objectCount % 100 == 0) {
            log("Object count id = " + objectCount);
          }
          objectCount++;
        }
      } catch (Exception e) {
        e.printStackTrace();
        synchronized (this) {
          exceptions.add(e);
        }
      }
    }

    private void createCicleOfNodes(Node parent, int count) {
      Node prev = parent;
      for (int i = 1; i <= count; i++) {
        Node node = new Node(parent.id + i);
        prev.add(node);
        prev = node;
      }
      prev.add(parent);
    }
  }

  private static final class RemoveThread extends Thread {

    private Set root;

    private Set exceptions = new HashSet();

    public RemoveThread(Set root) {
      this.root = root;
    }

    public synchronized Set getExceptions() {
      return exceptions;
    }

    public void run() {
      try {
        int removeCount = 0;
        while (true) {
          synchronized (root) {
            Iterator iter = root.iterator();
            if (iter.hasNext()) {
              iter.next();
              iter.remove();
              removeCount++;
            }
          }

          if (removeCount % 100 == 0) {
            log("remove count = " + removeCount);
          }

        }
      } catch (Exception e) {
        e.printStackTrace();
        synchronized (this) {
          exceptions.add(e);
        }
      }
    }
  }

  private static final class DisplaceThread extends Thread {

    private Set root;

    private Set exceptions   = new HashSet();

    private Set referenceSet = new HashSet();

    public DisplaceThread(Set root) {
      this.root = root;
    }

    public synchronized Set getExceptions() {
      return exceptions;
    }

    public void run() {
      try {
        int displacedCount = 0;
        while (true) {

          synchronized (root) {
            Iterator iter = root.iterator();
            if (iter.hasNext()) {
              Node currentRemove = (Node) iter.next();
              iter.remove();
              displacedCount++;
              referenceSet.add(currentRemove);
              if (displacedCount % 5 == 0) {
                System.out.println("displacing: " + referenceSet.size());
                for (Iterator rIter = referenceSet.iterator(); rIter.hasNext();) {
                  Node currentNode = (Node)rIter.next();
                  Node node = currentNode.next().next();
                  root.add(node);
                }
                referenceSet.clear();
               
              }
              
            }
          }

          if (displacedCount % 100 == 0) {
            log("displaced count = " + displacedCount);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        synchronized (this) {
          exceptions.add(e);
        }
      }
    }
  }

  private static final class LocalGCThread extends Thread {

    private Set exceptions = new HashSet();

    public synchronized Set getExceptions() {
      return exceptions;
    }

    public void run() {
      try {

        while (true) {
          System.gc();
          Thread.sleep(50);

        }
      } catch (Exception e) {
        e.printStackTrace();
        synchronized (this) {
          exceptions.add(e);
        }
      }
    }
  }

  private static final class Node {
    final long id;
    // the same id.
    long       lastAccess;
    long       level;

    byte[]     transientBytes = new byte[1000];

    Node       next;

    Node(long id) {
      this.id = id;
      this.level = 0;
      this.lastAccess = System.currentTimeMillis();
    }

    public Node next() {
      return next;
    }

    void add(Node c) {
      this.lastAccess = System.currentTimeMillis();
      next = c;
    }
  }

}
