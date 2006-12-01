/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.perf.collections;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.simulator.listener.StatsListener;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Generic timing for {@link Collections} classes, including insertion, iteration, sorting and removal. Subclasses
 * provide the actual {@link Collection} implemention along with the type of elements to use inside it.
 */
public abstract class CollectionsPerfTestAppBase extends AbstractTransparentApp {

  private Map rootMap = new HashMap();
  private CollectionType sharedCollection;
  private Integer        transactionSize;
  private StatsListener  txnLogger;
  boolean                staging   = true;
  boolean                described = false;

  public CollectionsPerfTestAppBase() {
    super();
  }

  public CollectionsPerfTestAppBase(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    transactionSize = new Integer(System.getProperty("dso.txn.size", "5"));
    Properties p = new Properties();
    txnLogger = listenerProvider.newStatsListener(p);
  }

  protected void setCollection(CollectionType collect) {
    synchronized (rootMap){
      sharedCollection = (CollectionType)rootMap.get("collection");
      if((sharedCollection == null) || !sharedCollection.getClass().getName().equals(collect.getClass().getName())){
        sharedCollection = collect;
        rootMap.put("collection", sharedCollection);
      }
    }
  }

  public void runPerType(ElementType.Factory elementFactory, int stageStart) {

    final int totalElementCount = getIntensity();
    final int txnSize = transactionSize.intValue();
    StringBuffer sbuf = new StringBuffer("intensity: ").append(getIntensity());
    sbuf.append(" participant count: ").append(getParticipantCount());
    sbuf.append(" totalElementCount: ").append(totalElementCount);
    sbuf.append(" transactionSize: ").append(txnSize);
    if (!described) {
      described = true;
      System.out.println(sbuf.toString());
    }

    StringBuffer buf = new StringBuffer("collection type: " + sharedCollection.describeType());
    buf.append(", element type: " + elementFactory.describeType());
    buf.append(" staging:" + (staging ? "On" : "Off"));
    buf.append(", clientId=" + getApplicationId() + " ");
    // buf.append(", threadId=" + Thread.currentThread().getName() + " ");
    String testBase = buf.toString();
    String testDesc = testBase;

    try {
      moveToStageAndWait(stageStart + 1);
      testDesc = "stage: " + (stageStart + 1) + " " + testBase;
      addElements(elementFactory, totalElementCount, txnSize, testDesc);

      if (staging) moveToStageAndWait(stageStart + 2);
      testDesc = "stage: " + (stageStart + 2) + " " + testBase;
      sortCollection(testDesc);

      if (staging) moveToStageAndWait(stageStart + 3);
      testDesc = "stage: " + (stageStart + 3) + " " + testBase;
      iterateCollection(testDesc);

      if (staging) moveToStageAndWait(stageStart + 4);
      testDesc = "stage: " + (stageStart + 4) + " " + testBase;
      removeElements(totalElementCount, txnSize, testDesc);

      moveToStageAndWait(stageStart + 5);
      testDesc = "stage: " + (stageStart + 5) + " " + testBase;
      clearCollection(testDesc);
    } catch (Exception e) {
      notifyError(e);
      return;
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    // add root
    config.addRoot("rootMap", CollectionsPerfTestAppBase.class.getName() + ".rootMap");
    // add all perf classes
    config.addIncludePattern("com.tctest.perf.collections.*");
    // add all methods
    String methodExpression = "* " + CollectionsPerfTestAppBase.class.getName() + "*.*(..)";
    config.addWriteAutolock(methodExpression);
  }

  public final void run() {
    int stageStart = 0;
    int stageInc = 5;
    runPerType(new ElementType.LongFactory(), stageStart);
    stageStart += stageInc;
    runPerType(new ElementType.StringFactory(), stageStart);
    stageStart += stageInc;
    runPerType(new ElementType.GraphFactory(20, new ElementType.LongFactory()), stageStart);
    stageStart += stageInc;
    runPerType(new ElementType.GraphFactory(20, new ElementType.StringFactory()), stageStart);
    staging = false;
    stageStart += stageInc;
    runPerType(new ElementType.LongFactory(), stageStart);
    stageStart += stageInc;
    runPerType(new ElementType.StringFactory(), stageStart);
    stageStart += stageInc;
    runPerType(new ElementType.GraphFactory(20, new ElementType.LongFactory()), stageStart);
    stageStart += stageInc;
    runPerType(new ElementType.GraphFactory(20, new ElementType.StringFactory()), stageStart);

  }

  private void removeElements(final int totalElementCount, final int txnSize, String testDesc) {
    final Timer txnTimer = new Timer();
    final Timer lockTimer = new Timer();
    final Timer unlockTimer = new Timer();
    final Timer opTimer = new Timer();

    for (int elementCount = 0; elementCount < totalElementCount; elementCount += txnSize) {
      int count = Math.min(txnSize, totalElementCount - elementCount);
      txnTimer.start();
      lockTimer.start();
      synchronized (sharedCollection) {
        lockTimer.stop();

        opTimer.start();
        sharedCollection.remove(count);
        opTimer.stop();

        unlockTimer.start();
      }
      unlockTimer.stop();
      txnTimer.stop();
      txnLogger.sample(-1, testDesc + "removed " + count + " elements Txn stats: lock=" + lockTimer.getElapsedMillis() + " op="
                           + opTimer.getElapsedMillis() + " unlock=" + unlockTimer.getElapsedMillis() + " txn="
                           + txnTimer.getElapsedMillis());
      Thread.yield();
    }
    txnLogger.sample(-1, testDesc + "remove stage avg stats: lock=" + lockTimer.getAvgElapsed() + " op="
                         + opTimer.getAvgElapsed() + " unlock=" + unlockTimer.getAvgElapsed() + " txn="
                         + txnTimer.getAvgElapsed());
  }

  private void iterateCollection(String testDesc) {
    final Timer txnTimer = new Timer();
    final Timer lockTimer = new Timer();
    final Timer unlockTimer = new Timer();
    final Timer opTimer = new Timer();

    txnTimer.start();
    lockTimer.start();
    synchronized (sharedCollection) {
      lockTimer.stop();

      opTimer.start();
      sharedCollection.iterate();
      opTimer.stop();

      unlockTimer.start();
    }
    unlockTimer.stop();
    txnTimer.stop();
    txnLogger.sample(-1, testDesc + "iterate Txn stats: lock=" + lockTimer.getElapsedMillis() + " op="
                         + opTimer.getElapsedMillis() + " unlock=" + unlockTimer.getElapsedMillis() + " txn="
                         + txnTimer.getElapsedMillis());
    Thread.yield();
  }

  private void sortCollection(String testDesc) {
    final Timer txnTimer = new Timer();
    final Timer lockTimer = new Timer();
    final Timer unlockTimer = new Timer();
    final Timer opTimer = new Timer();

    txnTimer.start();
    lockTimer.start();
    synchronized (sharedCollection) {
      lockTimer.stop();

      if (!sharedCollection.isSorted()) {
        opTimer.start();
        sharedCollection.sort();
        sharedCollection.setSorted(true);
        opTimer.stop();

      }
      unlockTimer.start();
    }
    unlockTimer.stop();
    txnTimer.stop();
    txnLogger.sample(-1, testDesc + "sort Txn stats: lock=" + lockTimer.getElapsedMillis() + " op="
                         + opTimer.getElapsedMillis() + " unlock=" + unlockTimer.getElapsedMillis() + " txn="
                         + txnTimer.getElapsedMillis());
    Thread.yield();
  }

  private void addElements(ElementType.Factory elementFactory, final int totalElementCount, final int txnSize,
                           String testDesc) {
    final Timer txnTimer = new Timer();
    final Timer lockTimer = new Timer();
    final Timer unlockTimer = new Timer();
    final Timer opTimer = new Timer();

    for (int elementCount = 0; elementCount < totalElementCount; elementCount += txnSize) {
      int count = Math.min(txnSize, totalElementCount - elementCount);
      txnTimer.start();
      lockTimer.start();
      synchronized (sharedCollection) {
        lockTimer.stop();

        opTimer.start();
        sharedCollection.add(count, elementFactory);
        opTimer.stop();

        unlockTimer.start();
      }
      unlockTimer.stop();
      txnTimer.stop();
      txnLogger.sample(-1, testDesc + "added " + count + " elements Txn stats: lock=" + lockTimer.getElapsedMillis() + " op="
                           + opTimer.getElapsedMillis() + " unlock=" + unlockTimer.getElapsedMillis() + " txn="
                           + txnTimer.getElapsedMillis());
      Thread.yield();
    }
    txnLogger.sample(-1, testDesc + "addElements stage avg stats: lock=" + lockTimer.getAvgElapsed() + " op="
                         + opTimer.getAvgElapsed() + " unlock=" + unlockTimer.getAvgElapsed() + " txn="
                         + txnTimer.getAvgElapsed());
  }

  private void clearCollection(String testDesc) {
    synchronized (sharedCollection) {
      if (!(sharedCollection.size() == 0)) {
        txnLogger.sample(-1, testDesc + "clearing collection");
        sharedCollection.clear();
      }
    }
  }

}
