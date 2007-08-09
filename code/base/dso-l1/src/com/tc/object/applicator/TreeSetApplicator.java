/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.IDNAEncoding;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.Assert;
import com.tc.util.FieldUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * ChangeApplicator for HashSets.
 */
public class TreeSetApplicator extends HashSetApplicator {
  private static final String COMPARATOR_FIELDNAME = TreeMapApplicator.COMPARATOR_FIELDNAME;
  private static final Field  COMPARATOR_FIELD;
  private static final Field  TREE_MAP_FIELD;

  static {
    try {
      COMPARATOR_FIELD = TreeMap.class.getDeclaredField("comparator");
      COMPARATOR_FIELD.setAccessible(true);

      TREE_MAP_FIELD = TreeSet.class.getDeclaredField("m");
      TREE_MAP_FIELD.setAccessible(true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public TreeSetApplicator(IDNAEncoding encoding) {
    super(encoding);
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object pojo) throws IOException,
      ClassNotFoundException {
    TreeSet set = (TreeSet) pojo;
    DNACursor cursor = dna.getCursor();

    if (!cursor.next(encoding)) { return; }

    Object action = cursor.getAction();
    if (action instanceof PhysicalAction) {
      PhysicalAction pa = (PhysicalAction) action;
      Assert.assertEquals(COMPARATOR_FIELDNAME, pa.getFieldName());
      setComparator(set, objectManager.lookupObject((ObjectID) pa.getObject()));
    } else {
      LogicalAction la = (LogicalAction) action;
      int method = la.getMethod();
      Object[] params = la.getParameters();
      super.apply(objectManager, set, method, params);
    }

    while (cursor.next(encoding)) {
      LogicalAction la = cursor.getLogicalAction();
      int method = la.getMethod();
      Object[] params = la.getParameters();
      super.apply(objectManager, set, method, params);
    }
  }

  private void setComparator(Object treeSet, Object comparator) {
    try {
      Object treeMap = TREE_MAP_FIELD.get(treeSet);
      FieldUtils.tcSet(treeMap, comparator, COMPARATOR_FIELD);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    TreeSet set = (TreeSet) pojo;
    Comparator cmp = set.comparator();
    if (cmp != null) {
      Object cmpObj = getDehydratableObject(cmp, objectManager);
      if (cmpObj != null) {
        writer.addPhysicalAction(COMPARATOR_FIELDNAME, cmpObj);
      }
    }

    super.dehydrate(objectManager, tcObject, writer, pojo);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    TreeSet set = (TreeSet) pojo;
    filterPortableObject(set.comparator(), addTo);
    filterPortableObjects(set, addTo);
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
}
