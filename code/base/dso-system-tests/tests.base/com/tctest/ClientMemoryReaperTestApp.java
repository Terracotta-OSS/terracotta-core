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
import com.tc.util.runtime.Os;
import com.tctest.restart.system.ObjectDataTestApp;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

/**
 * All this test does is create many many objects so that if the Client side Memory Reaper doesnt run properly for some
 * reason then it forces an OOME This test needs some tuning
 */
public class ClientMemoryReaperTestApp extends AbstractErrorCatchingTransparentApp {
  public static final String SYNCHRONOUS_WRITE         = "synch-write";

  private static final long  OBJECT_COUNT              = 1500;
  private static final long  MINIMUM_MEM_NEEDED        = 60 * 1024 * 1024;
  private static final int   MEMORY_BLOCKS             = 1024 * 1024;

  final Map                  root                      = new HashMap();
  final Vector               unusedBytes               = new Vector();
  static long                maxDepth                  = 1;
  static int                 transient_mem_blocks_size = 0;

  public ClientMemoryReaperTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    visitL1DSOConfig(visitor, config, new HashMap());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config, Map optionalAttributes) {
    boolean isSynchronousWrite = false;
    if (optionalAttributes.size() > 0) {
      isSynchronousWrite = Boolean.valueOf((String) optionalAttributes.get(ObjectDataTestApp.SYNCHRONOUS_WRITE))
          .booleanValue();
    }

    String testClass = ClientMemoryReaperTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("root", "root");
    String methodExpression = "* " + testClass + ".getNode(..)";
    config.addReadAutolock(methodExpression);
    methodExpression = "* " + testClass + ".putNode(..)";
    addWriteAutolock(config, isSynchronousWrite, methodExpression);
    methodExpression = "* " + testClass + ".addNode(..)";
    addWriteAutolock(config, isSynchronousWrite, methodExpression);

    testClass = ClientMemoryReaperTestApp.Node.class.getName();
    spec = config.getOrCreateSpec(testClass);
    config.addTransient(testClass, "transientBytes");
  }

  private static void addWriteAutolock(DSOClientConfigHelper config, boolean isSynchronousWrite, String methodPattern) {
    if (isSynchronousWrite) {
      config.addSynchronousWriteAutolock(methodPattern);
    } else {
      config.addWriteAutolock(methodPattern);
    }
  }

  @Override
  public void runTest() {
    log("App Id = " + getApplicationId() + " participation count = " + getParticipantCount() + " intensity = "
        + getIntensity());

    // This is not used as the same effect is attained by using DSO transient
    if (false) initHeap();
    initTransientMemBlockSize();

    long objectCount = 0;
    log("Objects to Create = " + OBJECT_COUNT);
    final SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    log(" Seed for Random = " + seed);
    Random r = new Random(seed);

    int topLevelObjectCount = (int) (OBJECT_COUNT / 50);
    while (objectCount++ <= OBJECT_COUNT) {
      Object myKey = new Integer(r.nextInt(topLevelObjectCount));
      Node n = getNode(myKey);
      if (n == null) {
        putNode(myKey, new Node(objectCount));
      } else {
        addNode(n, new Node(objectCount));
      }
      if (objectCount % 100 == 0) {
        log("Objects created = " + objectCount);
      }
    }
    log("Done !!");
  }

  private static synchronized void initTransientMemBlockSize() {
    if (transient_mem_blocks_size > 0) {
      log("Transient memory block size is already initialized to " + transient_mem_blocks_size);
      return;
    }
    Runtime runtime = Runtime.getRuntime();
    long max_memory = runtime.maxMemory();
    if (max_memory == Long.MAX_VALUE) {
      // With no upperbound it is possible that this test wont fail even when client memory reaper is broken.
      throw new AssertionError("This test is memory sensitive. Please specify the max memory using -Xmx option. "
                               + "Currently Max Memory is " + max_memory);
    }
    log("Max memory is " + max_memory);
    if(Os.isSolaris() || Os.isWindows()) {
      transient_mem_blocks_size = (int) ((max_memory * 25) / (512 * 1024)); // 25KB for 512MB, so for max_memory ?
    } else {
      transient_mem_blocks_size = (int) ((max_memory * 50) / (512 * 1024)); // 50KB for 512MB, so for max_memory ?
    }
    log("Transient memory block size is " + transient_mem_blocks_size);
  }

  private void initHeap() {
    Runtime runtime = Runtime.getRuntime();
    long max_memory = runtime.maxMemory();
    if (max_memory == Long.MAX_VALUE || max_memory < MINIMUM_MEM_NEEDED) {
      // With no upperbound it is possible that this test wont fail even when client memory reaper is broken.
      throw new AssertionError("This test is memory sensitive. Please specify the max memory using -Xmx option. "
                               + " Ideal value for this test is >= " + MINIMUM_MEM_NEEDED
                               + ". Currently Max Memory is " + max_memory);
    }
    log("Max memory is " + max_memory);
    long totalAllocations = 0;
    // This is not fail proof, but worst case is a few extra allocations, (no of nodes * 1 MB)
    synchronized (unusedBytes) {
      while (max_memory > (runtime.totalMemory() + MINIMUM_MEM_NEEDED / 2) || runtime.freeMemory() > MINIMUM_MEM_NEEDED) {
        final byte[] unused = new byte[MEMORY_BLOCKS];
        unusedBytes.add(unused);
        totalAllocations += MEMORY_BLOCKS;
        log("Allocated " + unused.length + " bytes. Free memory = " + runtime.freeMemory() + ". Total memory = "
            + runtime.totalMemory());
      }
    }
    log("Total bytes allocated = " + totalAllocations);
  }

  static DateFormat formatter = new SimpleDateFormat("hh:mm:ss,S");

  private static void log(String message) {
    System.err.println(Thread.currentThread().getName() + " :: "
                       + formatter.format(new Date(System.currentTimeMillis())) + " : " + message);
  }

  private void addNode(Node rootNode, Node node) {
    synchronized (rootNode) {
      rootNode.add(node);
      // System.err.println("Added " + node + " to " + rootNode);
    }
  }

  private synchronized static void setMaxDepth(long depth) {
    if (maxDepth < depth) {
      maxDepth = depth;
      if (maxDepth % 10 == 0) {
        log("Max Depth reached : " + maxDepth);
      }
    }
  }

  private void putNode(Object myKey, Node n) {
    synchronized (root) {
      root.put(myKey, n);
    }
  }

  private Node getNode(Object myKey) {
    synchronized (root) {
      return (Node) root.get(myKey);
    }
  }

  private static final class Node {
    final long id;                                                  // Not necessarily unique as each node might create
    // with
    // the same id.
    @SuppressWarnings("unused")
    long       lastAccess;
    long       level;

    /* Just to make the object big */
    byte[]     transientBytes = new byte[transient_mem_blocks_size];

    Node       odd;
    Node       even;

    Node(long id) {
      this.id = id;
      this.level = 0;
      this.lastAccess = System.currentTimeMillis();
    }

    void add(Node c) {
      this.lastAccess = System.currentTimeMillis();
      if (this.transientBytes == null) {
        // TODO:: Comeback :: If it is toooo deep, this can take quite some memory
        this.transientBytes = new byte[transient_mem_blocks_size];
      }
      Assert.assertFalse(this == c);
      if (c.id % 2 == 1) {
        if (odd == null) {
          odd = c;
          c.level = this.level + 1;
          setMaxDepth(c.level);
        } else {
          odd.add(c);
        }
      } else {
        if (even == null) {
          even = c;
          c.level = this.level + 1;
          setMaxDepth(c.level);
        } else {
          even.add(c);
        }
      }
    }

    @Override
    public String toString() {
      return "Node(" + id + ") : level = " + level + " : odd = " + (odd == null) + " : even = " + (even == null);
    }

  }

}
