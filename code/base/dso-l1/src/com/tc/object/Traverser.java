/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.TCRuntimeException;
import com.tc.object.appevent.NonPortableEventContext;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Generic Object traverser. Initial use for this is to add any unmanaged objects to the managed object tree
 *
 * @author steve
 */
public class Traverser {
  private static final TraverseTest    NULL_TEST = new NullTraverseTest();
  private final TraversalAction        action;
  private final PortableObjectProvider portableObjectProvider;
  private NonPortableEventContext      context;

  public Traverser(TraversalAction action, PortableObjectProvider portableObjectProvider) {
    this.action = action;
    this.portableObjectProvider = portableObjectProvider;
  }

  private void addReferencedObjects(Map toBeVisited, Object start, Map visited, TraverseTest traverseTest) {
    Class clazz = start.getClass();

    while (clazz != null) {
      TraversedReferences portableObjects = new TraversedReferencesImpl();
      portableObjects = portableObjectProvider.getPortableObjects(clazz, start, portableObjects);

      for (Iterator i = portableObjects.iterator(); i.hasNext();) {
        try {
          TraversedReference currentReference = (TraversedReference) i.next();
          Object currentObject = currentReference.getValue();

          if (doNotTraverse(traverseTest, visited, currentObject)) {
            continue;
          }

          traverseTest.checkPortability(currentReference, start.getClass(), context);

          toBeVisited.put(currentObject, null);
        } catch (IllegalArgumentException e) {
          throw new TCRuntimeException(e);
        }
      }
      clazz = clazz.getSuperclass();
    }

  }

  private boolean doNotTraverse(TraverseTest traverseTest, Map visited, Object current) {
    if (current == null) { return true; }

    if (visited.containsKey(current)) { return true; }

    if (!traverseTest.shouldTraverse(current)) { return true; }

    return false;
  }

  public void traverse(Object object) {
    traverse(object, NULL_TEST, null);
  }

  public void traverse(Object object, TraverseTest traverseTest, NonPortableEventContext ctx) {
    this.context = ctx;
    Map visited = new IdentityHashMap();
    List toAdd = new ArrayList();

    visited.put(object, null);

    IdentityHashMap toBeVisited = new IdentityHashMap();
    addReferencedObjects(toBeVisited, object, visited, traverseTest);
    toAdd.add(object);

    while (!toBeVisited.isEmpty()) {
      for (Iterator i = new IdentityHashMap(toBeVisited).keySet().iterator(); i.hasNext();) {
        Object obj = i.next();
        visited.put(obj, null);
        toBeVisited.remove(obj);
        addReferencedObjects(toBeVisited, obj, visited, traverseTest);
        toAdd.add(obj); // action.visit() to be taken place after addReferencedObjects() so that
        // the manager of the referenced objects will only be set after the referenced
        // objects are obtained.
      }
    }
    action.visit(toAdd);
  }
}
