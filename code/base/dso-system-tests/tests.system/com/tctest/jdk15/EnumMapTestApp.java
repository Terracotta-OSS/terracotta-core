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

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public final class EnumMapTestApp extends AbstractErrorCatchingTransparentApp {

  enum Fruit {
    APPLE("Apple"), ORANGE("Orange"), BANANA("Banana"), PEAR("Pear");

    private final String name;

    Fruit(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

  }

  private static final LinkedList<String> AVAILABLE_FRUIT = new LinkedList<String>();

  private static void createAvailableFruit() {
    synchronized (AVAILABLE_FRUIT) {
      if (AVAILABLE_FRUIT.isEmpty()) {
        for (Fruit fruit : Fruit.values()) {
          AVAILABLE_FRUIT.add(fruit.getName());
        }
      }
    }
  }

  static {
    createAvailableFruit();
  }

  private final EnumMap<Fruit, String> clusteredFruitBasket;
  private final CyclicBarrier          barrier;

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    final String testClass = EnumMapTestApp.class.getName();
    config.addIncludePattern(testClass + "$*");
    config.addWriteAutolock("* " + testClass + "*.*(..)");

    final TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("AVAILABLE_FRUIT", "AVAILABLE_FRUIT");
    spec.addRoot("clusteredFruitBasket", "clusteredFruitBasket");
    spec.addRoot("barrier", "barrier");
  }

  public EnumMapTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    clusteredFruitBasket = new EnumMap<Fruit, String>(Fruit.class);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  protected void runTest() throws Throwable {
    barrier.await();
    addFruitToBasket(getNextPieceOfFruit());
    waitForBasketToFill();
    verifyBasketOperations();
    
//    barrier.await();
//    verifyNullValueBehavior();
  }

  private Fruit getNextPieceOfFruit() {
    final Fruit fruitToAddToBasket;
    synchronized (AVAILABLE_FRUIT) {
      final String fruitName = AVAILABLE_FRUIT.remove();
      Fruit foundFruit = null;
      for (Fruit f : Fruit.values()) {
        if (f.getName().equals(fruitName)) {
          foundFruit = f;
          break;
        }
      }
      Assert.assertNotNull(foundFruit);
      fruitToAddToBasket = foundFruit;
    }
    return fruitToAddToBasket;
  }

  private void addFruitToBasket(final Fruit fruit) {
    synchronized (clusteredFruitBasket) {
      clusteredFruitBasket.put(fruit, fruit.getName());
    }
  }

  private void waitForBasketToFill() throws InterruptedException, BrokenBarrierException {
    barrier.await();
  }

  private void verifyBasketOperations() {
    synchronized (clusteredFruitBasket) {
      Assert.assertEquals(Fruit.values().length, clusteredFruitBasket.size());
      final EnumMap<Fruit, String> equivalentBasket = new EnumMap<Fruit, String>(Fruit.class);
      for (Fruit fruit : Fruit.values()) {
        // Write
        Assert.assertEquals(fruit.getName(), clusteredFruitBasket.remove(fruit));
        Assert.assertFalse(clusteredFruitBasket.containsKey(fruit));
        Assert.assertFalse(clusteredFruitBasket.keySet().contains(fruit));
        Assert.assertFalse(clusteredFruitBasket.containsValue(fruit.getName()));
        Assert.assertFalse(clusteredFruitBasket.values().contains(fruit.getName()));
        clusteredFruitBasket.put(fruit, fruit.getName());

        // Read
        Assert.assertTrue(clusteredFruitBasket.containsKey(fruit));
        Assert.assertTrue(clusteredFruitBasket.keySet().contains(fruit));
        Assert.assertTrue(clusteredFruitBasket.containsValue(fruit.getName()));
        Assert.assertTrue(clusteredFruitBasket.values().contains(fruit.getName()));
        Assert.assertEquals(fruit.getName(), clusteredFruitBasket.get(fruit));

        // For our later .equals() comparison
        equivalentBasket.put(fruit, fruit.getName());
      }
      Assert.assertTrue(clusteredFruitBasket.equals(equivalentBasket));
      clusteredFruitBasket.clear();
      Assert.assertTrue(clusteredFruitBasket.isEmpty());
      clusteredFruitBasket.putAll(equivalentBasket);
      Assert.assertTrue(clusteredFruitBasket.equals(equivalentBasket));
    }
  }

//  private void verifyNullValueBehavior() throws InterruptedException, BrokenBarrierException {
//    int nodeId = barrier.await();
//    
//    if (nodeId == 0) {
//      synchronized (clusteredFruitBasket) {
//        clusteredFruitBasket.clear();
//        for (Fruit f : Fruit.values()) {
//          clusteredFruitBasket.put(f, null);
//        }
//      }
//    }
//    
//    barrier.await();
//
//    try {
//      synchronized (clusteredFruitBasket) {
//        for (Fruit f : Fruit.values()) {
//          Assert.assertNull("Null value mapped to " + f, clusteredFruitBasket.get(f));
//        }
//      }
//    } finally {
//      barrier.await();
//    }
//  }
}
