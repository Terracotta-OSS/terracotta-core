/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

public class CollectionsWrappersTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return CollectionsWrappersTestApp.class;
  }

  @SuppressWarnings("unchecked")
  public static class CollectionsWrappersTestApp extends AbstractErrorCatchingTransparentApp {

    private static final Map<String, Object> root = new HashMap<String, Object>();
    private final CyclicBarrier              barrier;

    private final LinkedList                 c    = new LinkedList(Arrays.asList(new Object[] { "item" }));
    private final ArrayList                  l    = new ArrayList(Arrays.asList(new Object[] { "timmy" }));
    private final HashMap                    m    = new HashMap();
    private final HashSet                    s    = new HashSet();
    private final TreeMap                    sm   = new TreeMap();
    private final TreeSet                    ss   = new TreeSet();
    {
      m.put("yer", "mom");
      s.add("it");
      sm.put("swing", "low");
      ss.add("sweet chariot");
    }

    public CollectionsWrappersTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      barrier = new CyclicBarrier(getParticipantCount());
    }

    @Override
    protected void runTest() throws Throwable {
      int n = barrier.barrier();

      if (n == 0) {
        synchronized (root) {
          // It's important here to use non-synch collection implelentation underneath the wrapper
          // This test is making sure that the wrapper acquires the proper write lock before
          // calling down to the underlying collection instance. If that instance is itself autolocked, then this test
          // isn't very effective (ie. don't use Hashtable, or Vector, etc)
          root.put("synch collection", Collections.synchronizedCollection(new LinkedList()));
          root.put("synch list", Collections.synchronizedList(new ArrayList()));
          root.put("synch map", Collections.synchronizedMap(new HashMap()));
          root.put("synch set", Collections.synchronizedSet(new HashSet()));
          root.put("synch sorted map", Collections.synchronizedSortedMap(new TreeMap()));
          root.put("synch sorted set", Collections.synchronizedSortedSet(new TreeSet()));

          root.put("empty list", Collections.EMPTY_LIST);
          root.put("empty set", Collections.EMPTY_SET);
          root.put("empty map", Collections.EMPTY_MAP);

          root.put("singleton", Collections.singleton("item"));
          root.put("singleton list", Collections.singletonList("item"));
          root.put("singleton map", Collections.singletonMap("key", "value"));

          root.put("unmod collection", Collections.unmodifiableCollection((Collection) c.clone()));
          root.put("unmod list", Collections.unmodifiableList((List) l.clone()));
          root.put("unmod map", Collections.unmodifiableMap((Map) m.clone()));
          root.put("unmod set", Collections.unmodifiableSet((Set) s.clone()));
          root.put("unmod sorted map", Collections.unmodifiableSortedMap((SortedMap) sm.clone()));
          root.put("unmod sorted set", Collections.unmodifiableSortedSet((SortedSet) ss.clone()));
        }

        // XXX: This test should really excercise all methods to test autolocking, not just
        // add(Object) and put(Object,Object)
        for (Map.Entry<String, Object> entry : root.entrySet()) {
          String key = entry.getKey();
          if (key.startsWith("synch ")) {
            Object o = root.get(key);
            if (o instanceof Collection) {
              Collection c1 = (Collection) o;
              // attempt modify it
              c1.add("value");
            } else if (o instanceof Map) {
              Map m1 = (Map) o;
              m1.put("key", "value");
            } else {
              throw new AssertionError("unknown type: " + o.getClass());
            }
          }
        }
      }

      barrier.barrier();

      verify();
    }

    private static void testIterators(Object o) {
      if (o instanceof Collection) {
        testIterator(((Collection) o).iterator());
      } else if (o instanceof Map) {
        Map m1 = (Map) o;
        testIterator(m1.keySet().iterator());
        testIterator(m1.entrySet().iterator());
        testIterator(m1.values().iterator());
      }
    }

    private static void testIterator(Iterator iter) {
      while (iter.hasNext()) {
        if (iter.next() == null) { throw new AssertionError(iter.getClass()); }
      }
    }

    private void verify() {
      for (Map.Entry<String, Object> entry : root.entrySet()) {
        testIterators(entry.getValue());

        String key = entry.getKey();
        if (key.startsWith("synch ")) {
          Object o = root.get(key);
          if (o instanceof Collection) {
            Collection c1 = (Collection) o;
            Assert.assertEquals(1, c1.size());
            Assert.assertEquals("value", c1.iterator().next());
          } else if (o instanceof Map) {
            Map m1 = (Map) o;
            Assert.assertEquals(1, m1.size());
            Assert.assertEquals("value", m1.values().iterator().next());
            Assert.assertEquals("key", m1.keySet().iterator().next());
          } else {
            throw new AssertionError("unknown type: " + o.getClass());
          }
        }
      }

      assertEquals(Collections.EMPTY_LIST, root.get("empty list"));
      assertEquals(Collections.EMPTY_SET, root.get("empty set"));
      assertEquals(Collections.EMPTY_MAP, root.get("empty map"));

      assertEquals(Collections.singleton("item"), root.get("singleton"));
      assertEquals(Collections.singletonList("item"), root.get("singleton list"));
      assertEquals(Collections.singletonMap("key", "value"), root.get("singleton map"));

      assertTrue(Arrays.equals(Collections.unmodifiableCollection(c).toArray(), ((Collection) root
          .get("unmod collection")).toArray()));
      assertEquals(Collections.unmodifiableList(l), root.get("unmod list"));
      assertEquals(Collections.unmodifiableMap(m), root.get("unmod map"));
      assertEquals(Collections.unmodifiableSet(s), root.get("unmod set"));
      assertEquals(Collections.unmodifiableSortedMap(sm), root.get("unmod sorted map"));
      assertEquals(Collections.unmodifiableSortedSet(ss), root.get("unmod sorted set"));

      for (Object element : root.values()) {
        exercise(element);
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      visitor.visit(config, new CyclicBarrierSpec());

      TransparencyClassSpec spec;
      String testClass;

      testClass = CollectionsWrappersTestApp.class.getName();
      spec = config.getOrCreateSpec(testClass);

      String methodExpression = "* " + testClass + ".*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("root", "root");
      spec.addRoot("barrier", "barrier");
    }

    private static void exercise(Object o) {
      // exercise all non-mutator methods (this is to shake out problems with uninstrumented classes, or improper
      // transients

      if (o instanceof SortedMap) {
        exerciseSortedMap((SortedMap) o);
      } else if (o instanceof SortedSet) {
        exerciseSortedSet((SortedSet) o);
      } else if (o instanceof Map) {
        exerciseMap((Map) o);
      } else if (o instanceof Set) {
        exerciseSet((Set) o);
      } else if (o instanceof List) {
        exerciseList((List) o);
      } else if (o instanceof Collection) {
        exerciseCollection((Collection) o);
      } else {
        throw new AssertionError("unknown type: " + o.getClass().getName());
      }
    }

    private static void exerciseCollection(Collection coll) {
      coll.contains(coll);
      coll.containsAll(coll);
      coll.equals(coll);
      coll.isEmpty();
      for (Iterator i = coll.iterator(); i.hasNext();) {
        i.next();
      }
      coll.hashCode();
      coll.size();
      coll.toArray();
      coll.toArray(new Object[] {});
      coll.toString();
    }

    private static void exerciseList(List list) {
      list.contains(list);
      list.containsAll(list);
      list.equals(list);
      if (list.size() > 0) {
        list.get(0);
      }
      list.indexOf(list);
      list.isEmpty();
      list.hashCode();
      for (Iterator i = list.iterator(); i.hasNext();) {
        i.next();
      }
      list.lastIndexOf(list);
      for (ListIterator i = list.listIterator(); i.hasNext();) {
        i.hasPrevious();
        i.hasNext();
        i.nextIndex();
        i.next();
        i.previousIndex();
        i.previous();
        i.next();
      }
      list.listIterator(0);
      list.size();
      list.subList(0, 0);
      list.toArray();
      list.toArray(new Object[] {});
      list.toString();
    }

    private static void exerciseSet(Set set) {
      set.contains("darth");
      set.containsAll(set);
      set.equals(set);
      set.hashCode();
      set.isEmpty();
      for (Iterator i = set.iterator(); i.hasNext();) {
        i.next();
      }
      set.size();
      set.toArray();
      set.toArray(new Object[] {});
      set.toString();
    }

    private static void exerciseSortedMap(SortedMap map) {
      exerciseMap(map);
      map.comparator();
      map.firstKey();
      map.headMap(map.firstKey());
      map.lastKey();
      map.subMap(map.firstKey(), map.lastKey());
      map.tailMap(map.firstKey());
    }

    private static void exerciseSortedSet(SortedSet set) {
      //
    }

    private static void exerciseMap(Map map) {
      map.containsKey("bob");
      map.containsValue(map);
      exerciseSet(map.entrySet());
      for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
        Map.Entry entry = (Entry) i.next();
        entry.getKey();
        entry.getValue();
      }
      map.equals(map);
      map.get("fanny");
      map.hashCode();
      map.isEmpty();
      exerciseSet(map.keySet());
      map.size();
      map.toString();
      exerciseCollection(map.values());
    }

  }
}
