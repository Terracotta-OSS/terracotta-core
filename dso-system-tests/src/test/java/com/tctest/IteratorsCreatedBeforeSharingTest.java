/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.runtime.Vm;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

public class IteratorsCreatedBeforeSharingTest extends TransparentTestBase {

  private final static int NODE_COUNT = 1;

  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final List roots = new ArrayList();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    protected void runTest() throws Throwable {
      List iterators = new ArrayList();

      List unsharedCollections = new ArrayList();

      // lists
      unsharedCollections.add(create(ArrayList.class));
      unsharedCollections.add(create(LinkedList.class));
      unsharedCollections.add(create(Vector.class));
      if (Vm.isJDK15Compliant()) {
        unsharedCollections.add(create(Class.forName("java.util.concurrent.LinkedBlockingQueue")));
      }

      // maps
      unsharedCollections.add(create(LinkedHashMap.class));
      unsharedCollections.add(create(IdentityHashMap.class));
      unsharedCollections.add(create(TreeMap.class));
      unsharedCollections.add(create(HashMap.class));
      unsharedCollections.add(create(Hashtable.class));
      unsharedCollections.add(create(Properties.class));
      if (Vm.isJDK15Compliant()) {
        unsharedCollections.add(create(Class.forName("java.util.concurrent.ConcurrentHashMap")));
      }

      // sets
      unsharedCollections.add(create(TreeSet.class));
      unsharedCollections.add(create(HashSet.class));
      unsharedCollections.add(create(LinkedHashSet.class));

      // get iterators on all the unshared collections
      iterators.addAll(getIterators(unsharedCollections));

      // share em
      synchronized (roots) {
        roots.addAll(unsharedCollections);
      }

      // sanity check
      for (Iterator iterator = roots.iterator(); iterator.hasNext();) {
        Manageable m = (Manageable) iterator.next();
        if (!m.__tc_isManaged()) { throw new AssertionError(m + " is not shared"); }
      }

      // clear refs for types that support it. This will catch problems with iterators that fail to resolve references
      for (Iterator iter = roots.iterator(); iter.hasNext();) {
        Object o = iter.next();
        clearAccessedOnElements(o);

        ((Manageable)o).__tc_managed().clearAccessed();
        if (o instanceof Clearable) {
          ((Clearable) o).__tc_clearReferences(Integer.MAX_VALUE);
        }
      }

      for (Iterator iter = iterators.iterator(); iter.hasNext();) {
        Iterator sharedObjectIterator = (Iterator) iter.next();

        Object o = sharedObjectIterator.next();
        if (o instanceof Map.Entry) {
          Map.Entry entry = (Entry) o;
          assertDummyType(entry.getKey(), sharedObjectIterator);
          assertDummyType(entry.getValue(), sharedObjectIterator);
        } else {
          assertDummyType(o, sharedObjectIterator);
        }
      }
    }

    private void clearAccessedOnElements(Object o) {
      if (o instanceof Collection) {
        for (Iterator i = ((Collection)o).iterator(); i.hasNext(); ) {
          ((Manageable)i.next()).__tc_managed().clearAccessed();
        }
      } else if (o instanceof Map) {
        for (Iterator i = ((Map)o).entrySet().iterator(); i.hasNext(); ) {
          Map.Entry entry = (Entry) i.next();
          ((Manageable)entry.getKey()).__tc_managed().clearAccessed();
          ((Manageable)entry.getValue()).__tc_managed().clearAccessed();
        }
      } else {
        throw new AssertionError(o.getClass());
      }
    }

    void assertDummyType(Object o, Iterator context) {
      if (!(o instanceof DummyType)) {
        //
        throw new AssertionError("invalid object (" + o.getClass() + ") returned from iterator of type "
                                 + context.getClass());
      }
    }

    private Collection getIterators(List unsharedCollections) {
      List rv = new ArrayList();
      for (Iterator iter = unsharedCollections.iterator(); iter.hasNext();) {
        Object o = iter.next();
        if (o instanceof List) {
          rv.add(((List) o).iterator());
          rv.add(((List) o).listIterator());
        } else if (o instanceof Map) {
          rv.add(((Map) o).entrySet().iterator());
          rv.add(((Map) o).values().iterator());
          rv.add(((Map) o).keySet().iterator());
        } else if (o instanceof Collection) {
          rv.add(((Collection) o).iterator());
        } else {
          throw new AssertionError(o.getClass());
        }
      }

      return rv;
    }

    private Object create(Class type) throws Exception {
      Object o = type.newInstance();
      if (o instanceof Collection) {
        ((Collection) o).add(new DummyType());
      } else if (o instanceof Map) {
        ((Map) o).put(new DummyType(), new DummyType());
      } else {
        throw new AssertionError(type.getName());
      }

      return o;
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      spec.addRoot("barrier", "barrier");
      spec.addRoot("roots", "roots");

      String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(writeAllowedMethodExpression);
      config.addIncludePattern(testClass + "$*");
    }

    // This type is just used as the item in the shared collections. I'm not using a regular jdk type since
    // those might get special cased at some point
    private static class DummyType implements Comparable {

      public int compareTo(Object o) {
        int thisVal = System.identityHashCode(this);
        int otherVal = System.identityHashCode(o);
        return (thisVal < otherVal ? -1 : (thisVal == otherVal ? 0 : 1));
      }

    }
  }
}
