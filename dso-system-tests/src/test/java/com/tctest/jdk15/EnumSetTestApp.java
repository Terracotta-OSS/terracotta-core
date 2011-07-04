/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class EnumSetTestApp extends AbstractErrorCatchingTransparentApp {

  enum Utensil {
    FORK("Fork"), SPOON("Spoon"), KNIFE("Knife");

    private final String name;

    Utensil(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

  }

  private static final EnumSet<Utensil> FULL_FLATWARE_SET  = EnumSet.allOf(Utensil.class);
  private static final EnumSet<Utensil> AVAILABLE_UTENSILS = EnumSet.allOf(Utensil.class);

  private final EnumSet<Utensil>        clusteredFlatwareSet;
  private final CyclicBarrier           barrier;

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    final String testClass = EnumSetTestApp.class.getName();
    config.addIncludePattern(testClass + "$*");
    config.addWriteAutolock("* " + testClass + "*.*(..)");

    final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("AVAILABLE_UTENSILS", "AVAILABLE_UTENSILS");
    spec.addRoot("clusteredFlatwareSet", "clusteredFlatwareSet");
    spec.addRoot("barrier", "barrier");
  }

  public EnumSetTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    clusteredFlatwareSet = EnumSet.noneOf(Utensil.class);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  protected void runTest() throws Throwable {
    barrier.await();
    addUtensilToFlatwareSet(getNextUtensil());
    waitForCompleteFlatwareSet();
    verifySetOperations();
  }

  private Utensil getNextUtensil() {
    final Utensil utensilToAddToFlatwareSet;
    synchronized (AVAILABLE_UTENSILS) {
      final Iterator<Utensil> iterator = AVAILABLE_UTENSILS.iterator();
      utensilToAddToFlatwareSet = iterator.next();
      Assert.assertNotNull(utensilToAddToFlatwareSet);
      iterator.remove();
      Assert.assertFalse(AVAILABLE_UTENSILS.contains(utensilToAddToFlatwareSet));
    }
    return utensilToAddToFlatwareSet;
  }

  private void addUtensilToFlatwareSet(final Utensil utensil) {
    synchronized (clusteredFlatwareSet) {
      clusteredFlatwareSet.add(utensil);
    }
  }

  private void waitForCompleteFlatwareSet() throws InterruptedException, BrokenBarrierException {
    barrier.await();
  }

  private void verifySetOperations() {
    synchronized (clusteredFlatwareSet) {
      // Read operations
      Assert.assertFalse(clusteredFlatwareSet.isEmpty());
      assertFlatwareSetCompleteness(true);
      for (Utensil utensil : Utensil.values()) {
        Assert.assertTrue(clusteredFlatwareSet.contains(utensil));
      }

      // Write operations
      for (Utensil utensil : Utensil.values()) {
        Assert.assertTrue(clusteredFlatwareSet.remove(utensil));
        assertFlatwareSetCompleteness(false);
        Assert.assertTrue(clusteredFlatwareSet.add(utensil));
        assertFlatwareSetCompleteness(true);
      }

      clusteredFlatwareSet.clear();
      assertEmpty();
      Assert.assertTrue(clusteredFlatwareSet.addAll(FULL_FLATWARE_SET));
      assertFlatwareSetCompleteness(true);

      Assert.assertTrue(clusteredFlatwareSet.removeAll(FULL_FLATWARE_SET));
      assertEmpty();
      Assert.assertTrue(clusteredFlatwareSet.addAll(FULL_FLATWARE_SET));
      assertFlatwareSetCompleteness(true);
    }
  }

  private void assertFlatwareSetCompleteness(final boolean shouldBeComplete) {
    synchronized (clusteredFlatwareSet) {
      Assert.assertEquals(shouldBeComplete, clusteredFlatwareSet.containsAll(FULL_FLATWARE_SET));
      Assert.assertEquals(shouldBeComplete, clusteredFlatwareSet.equals(FULL_FLATWARE_SET));
      if (shouldBeComplete) {
        Assert.assertEquals(clusteredFlatwareSet.size(), Utensil.values().length);
      } else {
        Assert.assertTrue(clusteredFlatwareSet.size() < Utensil.values().length);
      }
    }
  }

  private void assertEmpty() {
    synchronized (clusteredFlatwareSet) {
      Assert.assertTrue(clusteredFlatwareSet.isEmpty());
      Assert.assertEquals(0, clusteredFlatwareSet.size());
      assertFlatwareSetCompleteness(false);
    }
  }

}
