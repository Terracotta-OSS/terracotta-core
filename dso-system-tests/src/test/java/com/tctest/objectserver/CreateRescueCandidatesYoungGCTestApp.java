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
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class CreateRescueCandidatesYoungGCTestApp extends AbstractErrorCatchingTransparentApp {

  private static long       TEST_DURATION;

  private static long       THREAD_DURATION_TOLERANCE;

  private static long       THREAD_DURATION;

  private static final long MINUTE      = 60 * 1000;

  final Set                 root        = new HashSet();
  final Vector              unusedBytes = new Vector();
  static long               maxDepth    = 2;

  public CreateRescueCandidatesYoungGCTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);

    TEST_DURATION = Os.isSolaris() ? (1 * MINUTE) : (5 * MINUTE);
    THREAD_DURATION_TOLERANCE = Os.isSolaris() ? (15 * 1000) : (30 * 1000);
    THREAD_DURATION = TEST_DURATION - THREAD_DURATION_TOLERANCE;
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

  @Override
  public void runTest() {
    log("App Id = " + getApplicationId() + " participation count = " + getParticipantCount() + " intensity = "
        + getIntensity());

    PopulateThread populateThread = new PopulateThread(root, THREAD_DURATION);
    populateThread.start();

    RemoveThread removeThread = new RemoveThread(root, THREAD_DURATION);
    removeThread.start();

    DisplaceThread displaceThread = new DisplaceThread(root, THREAD_DURATION);
    displaceThread.start();

    LocalGCThread gcThread = new LocalGCThread(THREAD_DURATION);
    gcThread.start();

    try {
      populateThread.join();
      removeThread.join();
      displaceThread.join();
      gcThread.join();
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

    private final Set  root;

    private final Set  exceptions = new HashSet();

    private final long duration;

    public PopulateThread(Set root, long duration) {
      this.root = root;
      this.duration = duration;
    }

    public synchronized Set getExceptions() {
      return exceptions;
    }

    @Override
    public void run() {
      try {
        long objectCount = 0;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < (startTime + duration)) {
          Node rootNode = new Node(objectCount * 100);
          createCicleOfNodes(rootNode, 20);
          synchronized (root) {
            root.add(rootNode);
          }
          if (objectCount % 10 == 0) {
            log("Object count id = " + objectCount);
          }
          objectCount++;
          ThreadUtil.reallySleep(5);
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

    private final Set  root;

    private final Set  exceptions = new HashSet();

    private final long duration;

    public RemoveThread(Set root, long duration) {
      this.root = root;
      this.duration = duration;
    }

    public synchronized Set getExceptions() {
      return exceptions;
    }

    @Override
    public void run() {
      try {
        int removeCount = 0;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < (startTime + duration)) {
          synchronized (root) {
            Iterator iter = root.iterator();
            if (iter.hasNext()) {
              iter.next();
              iter.remove();
              removeCount++;
            }
          }
          ThreadUtil.reallySleep(5);

          if (removeCount % 10 == 0) {
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

    private final Set  root;

    private final Set  exceptions   = new HashSet();

    private final Set  referenceSet = new HashSet();

    private final long duration;

    public DisplaceThread(Set root, long duration) {
      this.root = root;
      this.duration = duration;
    }

    public synchronized Set getExceptions() {
      return exceptions;
    }

    @Override
    public void run() {
      try {
        int displacedCount = 0;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < (startTime + duration)) {

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
                  Node currentNode = (Node) rIter.next();
                  Node node = currentNode.next().next();
                  root.add(node);
                }
                referenceSet.clear();

              }

            }
          }
          ThreadUtil.reallySleep(5);

          if (displacedCount % 10 == 0) {
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

    private final Set  exceptions = new HashSet();

    private final long duration;

    public LocalGCThread(long duration) {
      this.duration = duration;
    }

    public synchronized Set getExceptions() {
      return exceptions;
    }

    @Override
    public void run() {
      try {

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < (startTime + duration)) {
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

  @SuppressWarnings("unused")
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
