/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

public class ConcurrentHashMapMultipleNodesTestApp extends AbstractTransparentApp {
  private static final int     CACHE_CONCURRENCY_LEVEL = 12;
  private static final int     NUM_OF_OBJECTS          = 800;
  private static final int     MAX_OBJECTS             = 400;
  private static final boolean OP_SUCCEEDED            = true;
  private static final boolean OP_FAILED               = false;
  private static final int     DURATION                = 300000;   

  private final CyclicBarrier  barrier;
  private final SessionCache   cache                   = new SessionCache();

  public ConcurrentHashMapMultipleNodesTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();

      if (index == 0) {
        loadData();
      }

      barrier.await();

      if (index != 0) {
        runTest();
      }

      barrier.await();

    } catch (Throwable t) {
      notifyError(t);
    }
  }
  
  private void runTest() throws Throwable {
    System.err.println("Start Running Test");
    long currentTime = System.currentTimeMillis();
    long spentTime = 0;
    int count = 0;
    while (spentTime < DURATION) {
      System.err.println("Running " + (++count));
      runReadTest();
      runInsertTest();
      long endTime = System.currentTimeMillis();
      spentTime = spentTime + (endTime - currentTime);
    }
    System.err.println("Test FINISHED");
  }

  private void runReadTest() throws Throwable {
    UUID uuid = UUID.randomUUID();
    Random random = new Random(uuid.getLeastSignificantBits());
    int id = (int)(random.nextGaussian() * MAX_OBJECTS);
    
    if (id < 0 || id >= MAX_OBJECTS) {
      System.err.println("Skipping non existent user id" + id);
      return;
    }
    
    UserIdCacheKey key = new UserIdCacheKey(id);
    System.err.println("Getting session for user id" + key.getUserId());
    TerracottaSession session = cache.getSession(key);
    if (session == null) {
      System.err.println("Skipping non existent user id" + id);
      return;
    }
    System.err.println("Got session user id" + key.getUserId());
    Assert.assertEquals(key.getUserId(), session.getI());
    System.err.flush();
  }
  
  private void runInsertTest() throws Throwable {
    UUID uuid = UUID.randomUUID();
    Random random = new Random(uuid.getLeastSignificantBits());
    int id = (int)(random.nextGaussian() * MAX_OBJECTS);
    
    if (id < 0 || id >= MAX_OBJECTS) {
      return;
    }
    
    UserIdCacheKey key = new UserIdCacheKey(id);
    TerracottaSession session = cache.getSession(key);
    if (session == null) {
      session = new TerracottaSession(key.getUserId());
      cache.insertSession(key, session);
    }
  }

  private void loadData() {
    for (int i = 0; i < NUM_OF_OBJECTS; i++) {
      System.err.println("Loading object " + i);
      UserIdCacheKey key = new UserIdCacheKey(i);
      TerracottaSession session = new TerracottaSession(key.getUserId());
      
      cache.insertSession(key, session);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ConcurrentHashMapMultipleNodesTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*", false, false, true);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("cache", "cache");
  }

  private static class SessionCache {
    private final ConcurrentHashMap cache = new ConcurrentHashMap(NUM_OF_OBJECTS, 0.75f, CACHE_CONCURRENCY_LEVEL);

    public TerracottaSession getSession(UserIdCacheKey key) {
      TerracottaSession session = null;
      session = (TerracottaSession) cache.get(key);
      return session;
    }

    public boolean insertSession(UserIdCacheKey key, TerracottaSession value) {
      TerracottaSession session = null;
      session = (TerracottaSession) cache.put(key, value);
      if (session != null) {
        System.err.println("Found TerracottaSession with user id: " + key.getUserId() + " already. Duplicate insert");
        return OP_FAILED;
      }
      return OP_SUCCEEDED;
    }
  }

  private static class TerracottaSession {
    private final int i;

    public TerracottaSession(int i) {
      this.i = i;
    }

    public int getI() {
      return i;
    }
  }

  private static class UserIdCacheKey {
    private int userId;

    public UserIdCacheKey(int u) {
      userId = u;
    }

    public boolean equals(Object other) {
      if (null == other) return false;
      if (!(other instanceof UserIdCacheKey)) return false;

      return userId == ((UserIdCacheKey) other).userId;
    }

    public int hashCode() {
      return userId;
    }

    public String toString() {
      return Integer.toString(userId);
    }

    public int getUserId() {
      return userId;
    }
  }
}
