/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.Manageable;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.object.tx.optimistic.TCObjectClone;

import java.io.IOException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Apply a logical action to an object
 */
public class HashMapApplicator extends BaseApplicator {

  public HashMapApplicator(DNAEncoding encoding) {
    super(encoding);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    Map m = (Map) pojo;
    filterPortableObjects(m.keySet(), addTo);
    filterPortableObjects(m.values(), addTo);
    return addTo;
  }

  private void filterPortableObjects(Collection objects, TraversedReferences addTo) {
    for (Iterator i = objects.iterator(); i.hasNext();) {
      Object o = i.next();
      if (o != null && isPortableReference(o.getClass())) {
        addTo.addAnonymousReference(o);
      }
    }
  }

  protected boolean isPortableReference(Class c) {
    return TCFieldFactory.isReferenceClass(c);
  }

  /**
   * return the key value pairs of field names to shared objects for this source. We already updated the literals and
   * set the new TCObjectClone
   */
  public Map connectedCopy(Object source, Object dest, Map visited, ClientObjectManager objectManager,
                           OptimisticTransactionManager txManager) {
    Map cloned = new IdentityHashMap();

    Manageable sourceManageable = (Manageable) source;
    Manageable destManaged = (Manageable) dest;

    Map sourceMap = (Map) source;
    Map destMap = (Map) dest;

    for (Iterator i = sourceMap.entrySet().iterator(); i.hasNext();) {
      Entry e = (Entry) i.next();

      Object copyKey = createCopyIfNecessary(objectManager, visited, cloned, e.getKey());
      Object copyValue = createCopyIfNecessary(objectManager, visited, cloned, e.getValue());
      destMap.put(copyKey, copyValue);
    }

    destManaged.__tc_managed(new TCObjectClone(sourceManageable.__tc_managed(), txManager));
    return cloned;
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      ClassNotFoundException {
    DNACursor cursor = dna.getCursor();

    while (cursor.next(encoding)) {
      LogicalAction action = cursor.getLogicalAction();
      int method = action.getMethod();
      Object[] params = action.getParameters();
      apply(objectManager, po, method, params);
    }
  }

  protected void apply(ClientObjectManager objectManager, Object po, int method, Object[] params) {
    Map m = (Map) po;
    switch (method) {
      case SerializationUtil.PUT:
        Object k = getKey(params);
        Object v = getValue(params);
        Object pkey = getObjectForKey(objectManager, k);

        Object value = getObjectForValue(objectManager, v);

        m.put(pkey, value);
        break;
      case SerializationUtil.REMOVE:
        Object rkey = params[0] instanceof ObjectID ? objectManager.lookupObject((ObjectID) params[0]) : params[0];
        m.remove(rkey);
        break;
      case SerializationUtil.CLEAR:
        m.clear();
        break;
      default:
        throw new AssertionError("invalid action:" + method);
    }
  }

  // This can be overridden by subclass if you want different behavior.
  protected Object getObjectForValue(ClientObjectManager objectManager, Object v) {
    return (v instanceof ObjectID ? objectManager.lookupObject((ObjectID) v) : v);
  }

  // This can be overridden by subclass if you want different behavior.
  protected Object getObjectForKey(ClientObjectManager objectManager, Object k) {
    return (k instanceof ObjectID ? objectManager.lookupObject((ObjectID) k) : k);
  }

  private Object getValue(Object[] params) {
    // Hack hack big hack for trove maps which replace the key on set as opposed to HashMaps which do not.
    return params.length == 3 ? params[2] : params[1];
  }

  private Object getKey(Object[] params) {
    return params.length == 3 ? params[1] : params[0];
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {

    Map map = (Map) pojo;
    for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Object key = entry.getKey();
      Object value = entry.getValue();

      if (!objectManager.isPortableInstance(key)) {
        continue;
      }
      if (!objectManager.isPortableInstance(value)) {
        continue;
      }

      final Object addKey = getDehydratableObject(key, objectManager);
      final Object addValue = getDehydratableObject(value, objectManager);

      if (addKey == null || addValue == null) {
        continue;
      }

      writer.addLogicalAction(SerializationUtil.PUT, new Object[] { addKey, addValue });
    }
  }

  public Object getNewInstance(ClientObjectManager objectManager, DNA dna) throws IOException, ClassNotFoundException {
    if (false) { throw new IOException(); } // silence compiler warning
    if (false) { throw new ClassNotFoundException(); } // silence compiler warning
    throw new UnsupportedOperationException();
  }
}
