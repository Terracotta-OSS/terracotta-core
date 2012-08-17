/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.junit.Assert;
import org.terracotta.express.tests.CallableWaiter;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

public abstract class AbstractClusteredMapBasicTestClient extends ClientBase {

  public static final int   NUM_ELEMENTS = 100;
  public static final int   NODE_COUNT   = 2;
  private ToolkitBarrier    barrier;
  private int               nodeId;
  private Toolkit           toolkit;
  private final Consistency consistency;

  public AbstractClusteredMapBasicTestClient(ToolkitStoreConfigFields.Consistency consistnecy, String[] args) {
    super(args);
    this.consistency = consistnecy;
  }

  @Override
  protected void test(Toolkit myToolkit) throws Throwable {
    toolkit = myToolkit;
    barrier = getBarrierForAllClients();
    nodeId = this.barrier.await();
    testBasic();
    barrier.await();
    testEntrySet();
    barrier.await();
    testKeySet();
    barrier.await();
    testValues();
  }

  private void testEntrySet() throws Exception {
    debug("Testing entry set");
    final ToolkitStore<String, String> testMap = getMap("entrySetMap");
    populateMap(testMap);

    final Set<Entry<String, String>> entrySet = testMap.entrySet();
    assertWithCallable(new Callable<Boolean>() {
      public Boolean call() {
        return checkInts(testMap.size(), entrySet.size());
      }
    });

    barrier.await();

    if (nodeId == 0) {
      int removed = 0;
      Iterator<Entry<String, String>> iterator = entrySet.iterator();
      while (iterator.hasNext() && removed < NUM_ELEMENTS) {
        iterator.next();
        removed++;
        iterator.remove();
      }
      testMap.size();
      assertWithCallable(new Callable<Boolean>() {
        public Boolean call() {
          return checkInts(testMap.size(), entrySet.size());
        }
      });
    }

    barrier.await();

    assertWithCallable(new Callable<Boolean>() {
      public Boolean call() {
        return checkInts(NUM_ELEMENTS, testMap.size());
      }
    });
  }

  private Boolean checkInts(int expected, int actual) {
    debug("Expected: " + expected + ", Actual: " + actual);
    return expected == actual;
  }

  private Boolean checkStrings(String expected, String actual) {
    debug("Expected: " + expected + ", Actual: " + actual);
    return expected.equals(actual);
  }

  private void assertWithCallable(Callable<Boolean> callable) throws Exception {
    if (consistency == Consistency.STRONG) {
      // no wait for strong
      Assert.assertTrue("Callable returned false", callable.call());
    } else {
      // wait until true for eventual
      CallableWaiter.waitOnCallable(callable);
    }
  }

  private ToolkitStore<String, String> getMap(String name) {
    Configuration config = new ToolkitStoreConfigBuilder().consistency(consistency).build();
    ToolkitStore<String, String> testMap = toolkit.getStore(name, config, null);
    return testMap;
  }

  private void testKeySet() throws Exception {
    debug("Testing keySet()");
    final ToolkitStore<String, String> testMap = getMap("keySetMap");
    populateMap(testMap);

    final Set<String> keySet = testMap.keySet();
    assertWithCallable(new Callable<Boolean>() {
      public Boolean call() {
        return checkInts(testMap.size(), keySet.size());
      }
    });
    for (int i = 0; i < NODE_COUNT; i++) {
      for (int j = 0; j < NUM_ELEMENTS; j++) {
        Assert.assertTrue(keySet.contains(getKey(i, j)));
      }
    }

    barrier.await();

    if (nodeId == 1) {
      Set<String> removeAllSet = new HashSet<String>();
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        removeAllSet.add(getKey(nodeId, i));
      }
      keySet.removeAll(removeAllSet);
      testMap.size();
      assertWithCallable(new Callable<Boolean>() {
        public Boolean call() {
          System.err.println("Key set size " + keySet.size());
          return checkInts(NUM_ELEMENTS, keySet.size());
        }
      });
    }
    barrier.await();

    assertWithCallable(new Callable<Boolean>() {
      public Boolean call() {
        return checkInts(NUM_ELEMENTS, testMap.size());
      }
    });

    barrier.await();

    if (nodeId == 0) {
      Set<String> retainAllSet = Collections.singleton(getKey(nodeId, NUM_ELEMENTS - 1));
      keySet.retainAll(retainAllSet);
      testMap.size();
      assertWithCallable(new Callable<Boolean>() {
        public Boolean call() {
          return checkInts(1, keySet.size());
        }
      });
    }
    barrier.await();
    assertWithCallable(new Callable<Boolean>() {
      public Boolean call() {
        return checkInts(1, testMap.size());
      }
    });
    barrier.await();

    if (nodeId == 1) {
      final Set<String> keySet2 = testMap.keySet();
      Iterator<String> i = keySet2.iterator();
      while (i.hasNext()) {
        i.next();
        i.remove();
      }
      assertWithCallable(new Callable<Boolean>() {
        public Boolean call() {
          return checkInts(0, keySet2.size());
        }
      });
    }

    barrier.await();

    assertWithCallable(new Callable<Boolean>() {
      public Boolean call() {
        return checkInts(0, testMap.size());
      }
    });
  }

  private void testValues() throws Exception {
    debug("Testing values()");
    final ToolkitStore<String, String> testMap = getMap("valuesMap");

    populateMap(testMap);

    if (nodeId == 0) {
      final Collection<String> values = testMap.values();
      assertWithCallable(new Callable<Boolean>() {
        public Boolean call() {
          return checkInts(testMap.size(), values.size());
        }
      });

      Set<String> removedValues = new HashSet<String>();
      for (int i = 0; i < NUM_ELEMENTS / 2; i++) {
        removedValues.add("" + i);
      }

      values.removeAll(removedValues);
    }

    testMap.size();
    barrier.await();
    assertWithCallable(new Callable<Boolean>() {
      public Boolean call() {
        return checkInts(NUM_ELEMENTS, testMap.size());
      }
    });

    barrier.await();

    if (nodeId == 0) {
      final Collection<String> values = testMap.values();
      assertWithCallable(new Callable<Boolean>() {
        public Boolean call() {
          return checkInts(testMap.size(), values.size());
        }
      });

      Set<String> retainedValues = new HashSet<String>();
      for (int i = NUM_ELEMENTS / 2; i < NUM_ELEMENTS - 1; i++) {
        retainedValues.add("" + i);
      }

      values.retainAll(retainedValues);
    }
    testMap.size();

    barrier.await();

    assertWithCallable(new Callable<Boolean>() {
      public Boolean call() {
        return checkInts(NUM_ELEMENTS - NODE_COUNT, testMap.size());
      }
    });

    barrier.await();

    if (nodeId == 0) {
      final Collection<String> values = testMap.values();
      assertWithCallable(new Callable<Boolean>() {
        public Boolean call() {
          return checkInts(testMap.size(), values.size());
        }
      });

      Iterator<String> i = values.iterator();
      while (i.hasNext()) {
        i.next();
        i.remove();
      }
      testMap.size();
    }
    barrier.await();

    assertWithCallable(new Callable<Boolean>() {
      public Boolean call() {
        return checkInts(0, testMap.size());
      }
    });
  }

  private void testBasic() throws Exception {
    debug("Testing basics...");
    final ToolkitStore<String, String> testMap = getMap("basicMap");
    populateMap(testMap);

    for (int i = 0; i < NODE_COUNT; i++) {
      for (int j = 0; j < NUM_ELEMENTS; j++) {
        final String key = getKey(i, j);
        final int J = j;
        assertWithCallable(new Callable<Boolean>() {
          public Boolean call() {
            return checkStrings("" + J, testMap.get(key));
          }
        });
        Assert.assertTrue(testMap.containsKey(key));
      }
    }

    barrier.await();

    for (int i = 0; i < NUM_ELEMENTS; i++) {
      Assert.assertTrue(testMap.replace(getKey(nodeId, i), "" + i, "" + (i + 1000)));
    }

    barrier.await();

    for (int i = 0; i < NODE_COUNT; i++) {
      for (int j = 0; j < NUM_ELEMENTS; j++) {
        final String key = getKey(i, j);
        final int J = j;
        assertWithCallable(new Callable<Boolean>() {
          public Boolean call() {
            return checkStrings("" + (J + 1000), testMap.get(key));
          }
        });
        Assert.assertTrue(testMap.containsKey(key));
      }
    }

    barrier.await();

    for (int i = 0; i < NUM_ELEMENTS; i++) {
      Assert.assertTrue(testMap.remove(getKey(nodeId, i), "" + (i + 1000)));
    }

    barrier.await();

    assertWithCallable(new Callable<Boolean>() {
      public Boolean call() {
        return checkInts(0, testMap.size());
      }
    });

    barrier.await();

    testMap.put(getKey(nodeId, 0), "test");

    barrier.await();

    Assert.assertNull(((ToolkitCacheInternal) testMap).unsafeLocalGet(getKey((nodeId + 1) % NODE_COUNT, 0)));
    testMap.remove(getKey(nodeId, 0));

    barrier.await();

    Map<String, String> putAllMap = new HashMap<String, String>();
    for (int i = 0; i < NUM_ELEMENTS; i++) {
      putAllMap.put(getKey(nodeId, i), "" + (i + 2000));
    }
    testMap.putAll(putAllMap);

    barrier.await();

    for (int i = 0; i < NODE_COUNT; i++) {
      for (int j = 0; j < NUM_ELEMENTS; j++) {
        final String key = getKey(i, j);
        final int J = j;
        assertWithCallable(new Callable<Boolean>() {
          public Boolean call() {
            return checkStrings("" + (J + 2000), testMap.get(key));
          }
        });
        Assert.assertTrue(testMap.containsKey(key));
      }
    }
    barrier.await();
  }

  private void populateMap(final Map<String, String> map) throws Exception {
    barrier.await();
    for (int i = 0; i < NUM_ELEMENTS; i++) {
      map.put(getKey(nodeId, i), "" + i);
    }

    barrier.await();
    assertWithCallable(new Callable<Boolean>() {
      public Boolean call() {
        return checkInts(NODE_COUNT * NUM_ELEMENTS, map.size());
      }
    });

    barrier.await();
  }

  private String getKey(int i, int j) {
    return "" + i + "-" + j;
  }
}
