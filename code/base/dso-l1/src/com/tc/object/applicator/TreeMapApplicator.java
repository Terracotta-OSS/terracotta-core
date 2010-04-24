/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.logging.TCLogger;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObjectExternal;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.Assert;
import com.tc.util.FieldUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class TreeMapApplicator extends BaseApplicator {

  static final String        COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";
  private static final Field COMPARATOR_FIELD;

  static {
    try {
      Field field = TreeMap.class.getDeclaredField("comparator");
      field.setAccessible(true);
      COMPARATOR_FIELD = field;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public TreeMapApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    TreeMap treemap = (TreeMap) pojo;
    filterPortableObjects(treemap.keySet(), addTo);
    filterPortableObjects(treemap.values(), addTo);
    filterPortableObject(treemap.comparator(), addTo);
    return addTo;
  }

  private void filterPortableObjects(Collection objects, TraversedReferences addTo) {
    for (Iterator i = objects.iterator(); i.hasNext();) {
      Object value = i.next();
      filterPortableObject(value, addTo);
    }
  }

  private void filterPortableObject(Object value, TraversedReferences addTo) {
    if (value != null && isPortableReference(value.getClass())) {
      addTo.addAnonymousReference(value);
    }
  }

  public void hydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNA dna, Object po)
      throws IOException, ClassNotFoundException {
    Map m = (Map) po;
    DNACursor cursor = dna.getCursor();

    while (cursor.next(encoding)) {
      Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        // This is done so that subclass of TreeMaps can work
        PhysicalAction pa = (PhysicalAction) action;
        Assert.assertEquals(COMPARATOR_FIELDNAME, pa.getFieldName());
        Comparator c = (Comparator) objectManager.lookupObject((ObjectID) pa.getObject());
        setComparator(po, c);
      } else {
        LogicalAction la = (LogicalAction) action;
        int method = la.getMethod();
        Object[] params = la.getParameters();
        switch (method) {
          case SerializationUtil.PUT:
            Object k = params[0];
            Object v = params[1];
            Object pkey = k instanceof ObjectID ? objectManager.lookupObject((ObjectID) k) : k;
            Object value = v instanceof ObjectID ? objectManager.lookupObject((ObjectID) v) : v;
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
    }
  }

  private void setComparator(Object target, Object value) {
    try {
      FieldUtils.tcSet(target, value, COMPARATOR_FIELD);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void dehydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer, Object pojo) {
    TreeMap map = (TreeMap) pojo;

    Comparator cmp = map.comparator();
    final Object cmpObj = getDehydratableObject(cmp, objectManager);
    if (cmpObj != null) {
      writer.addPhysicalAction(COMPARATOR_FIELDNAME, cmpObj);
    }

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

  public Object getNewInstance(ApplicatorObjectManager objectManager, DNA dna) throws IOException,
      ClassNotFoundException {
    DNACursor cursor = dna.getCursor();
    if (!cursor.next(encoding)) { throw new AssertionError("Cursor is empty in TreeMap.getNewInstance()"); }
    PhysicalAction physicalAction = cursor.getPhysicalAction();
    Assert.assertEquals(COMPARATOR_FIELDNAME, physicalAction.getFieldName());
    Comparator c = (Comparator) objectManager.lookupObject((ObjectID) physicalAction.getObject());
    return (c == null ? new TreeMap() : new TreeMap(c));
  }
}
