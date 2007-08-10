/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.exception.TCRuntimeException;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.Manageable;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.object.tx.optimistic.TCObjectClone;
import com.tc.util.Assert;
import com.tc.util.FieldUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Apply a logical action to an object
 */
public class ConcurrentHashMapApplicator extends HashMapApplicator {
  private static final String CONCURRENT_HASH_MAP_FIELD_NAME_PREFIX = ConcurrentHashMap.class.getName() + ".";
  private static final String SEGMENT_MASK_FIELD_NAME               = "segmentMask";
  private static final String SEGMENT_SHIFT_FIELD_NAME              = "segmentShift";
  private static final String SEGMENT_FIELD_NAME                    = "segments";
  private static final String TC_PUT_METHOD_NAME                    = ByteCodeUtil.TC_METHOD_PREFIX + "put";

  private static final Field  SEGMENT_MASK_FIELD;
  private static final Field  SEGMENT_SHIFT_FIELD;
  private static final Field  SEGMENT_FIELD;
  private static final Method TC_PUT_METHOD;

  static {
    try {
      SEGMENT_MASK_FIELD = ConcurrentHashMap.class.getDeclaredField(SEGMENT_MASK_FIELD_NAME);
      SEGMENT_MASK_FIELD.setAccessible(true);

      SEGMENT_SHIFT_FIELD = ConcurrentHashMap.class.getDeclaredField(SEGMENT_SHIFT_FIELD_NAME);
      SEGMENT_SHIFT_FIELD.setAccessible(true);

      SEGMENT_FIELD = ConcurrentHashMap.class.getDeclaredField(SEGMENT_FIELD_NAME);
      SEGMENT_FIELD.setAccessible(true);

      TC_PUT_METHOD = ConcurrentHashMap.class.getDeclaredMethod(TC_PUT_METHOD_NAME, new Class[] { Object.class,
      Object.class });
      TC_PUT_METHOD.setAccessible(true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ConcurrentHashMapApplicator(DNAEncoding encoding) {
    super(encoding);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    super.getPortableObjects(pojo, addTo);
    getPhysicalPortableObjects(pojo, addTo);
    return addTo;
  }

  public void getPhysicalPortableObjects(Object pojo, TraversedReferences addTo) {
    try {
      filterPortableObject(SEGMENT_MASK_FIELD.get(pojo), addTo);
      filterPortableObject(SEGMENT_SHIFT_FIELD.get(pojo), addTo);
      Object[] segments = (Object[])SEGMENT_FIELD.get(pojo);
      for (int i=0; i<segments.length; i++) {
        filterPortableObject(segments[i], addTo);
      }
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
  }

  private void filterPortableObject(Object value, TraversedReferences addTo) {
    if (value != null && isPortableReference(value.getClass())) {
      addTo.addAnonymousReference(value);
    }
  }

  public void hydrate(ClientObjectManager objectManager, TCObject tcObject, DNA dna, Object po) throws IOException,
      ClassNotFoundException {
    int segmentLength = -1;
    Object segment = null;
    Object[] segments = null;
    int segmentIndex = 0;
    DNACursor cursor = dna.getCursor();

    while (cursor.next(encoding)) {
      Object action = cursor.getAction();
      if (action instanceof LogicalAction) {
        if (segmentLength != -1) {
          setSegmentField(segments, po, segmentLength, segmentIndex);
          segmentLength = -1;
        }

        LogicalAction logicalAction = cursor.getLogicalAction();
        int method = logicalAction.getMethod();
        Object[] params = logicalAction.getParameters();
        apply(objectManager, po, method, params);
      } else if (action instanceof LiteralAction) {
        LiteralAction literalAction = (LiteralAction) action;
        segmentLength = ((Integer) literalAction.getObject()).intValue();
      } else if (action instanceof PhysicalAction) {
        Assert.assertFalse(dna.isDelta()); // Physical fields are shared only when the DNA is not
        // a delta.

        PhysicalAction physicalAction = cursor.getPhysicalAction();

        Assert.eval(physicalAction.isTruePhysical());
        String fieldName = physicalAction.getFieldName();
        Object value = physicalAction.getObject();
        try {
          if (fieldName.equals(CONCURRENT_HASH_MAP_FIELD_NAME_PREFIX + SEGMENT_MASK_FIELD_NAME)) {
            FieldUtils.tcSet(po, value, SEGMENT_MASK_FIELD);
          } else if (fieldName.equals(CONCURRENT_HASH_MAP_FIELD_NAME_PREFIX + SEGMENT_SHIFT_FIELD_NAME)) {
            FieldUtils.tcSet(po, value, SEGMENT_SHIFT_FIELD);
          } else if (fieldName.equals(CONCURRENT_HASH_MAP_FIELD_NAME_PREFIX + SEGMENT_FIELD_NAME + segmentIndex)) {
            segment = objectManager.lookupObject((ObjectID) value);
            if (segments == null) {
              segments = (Object[]) Array.newInstance(segment.getClass(), segmentLength);
            }
            segments[segmentIndex] = segment;
            segmentIndex++;
          }
        } catch (IllegalAccessException e) {
          throw new TCRuntimeException(e);
        }
      }
    }
    setSegmentField(segments, po, segmentLength, segmentIndex);
  }
  
  private void setSegmentField(Object[] segments, Object po, int segmentLength, int segmentIndex) {
    if (segmentLength != -1) {
      Assert.assertEquals(segmentLength, segmentIndex);
      try {
        FieldUtils.tcSet(po, segments, SEGMENT_FIELD);
      } catch (IllegalAccessException e) {
        throw new TCRuntimeException(e);
      }
    }
  }

  public void dehydrate(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    dehydrateFields(objectManager, tcObject, writer, pojo);
    super.dehydrate(objectManager, tcObject, writer, pojo);
  }

  private void dehydrateFields(ClientObjectManager objectManager, TCObject tcObject, DNAWriter writer, Object pojo) {
    try {
      Object segmentMask = SEGMENT_MASK_FIELD.get(pojo);
      segmentMask = getDehydratableObject(segmentMask, objectManager);
      writer.addPhysicalAction(CONCURRENT_HASH_MAP_FIELD_NAME_PREFIX + SEGMENT_MASK_FIELD_NAME, segmentMask);

      Object segmentShift = SEGMENT_SHIFT_FIELD.get(pojo);
      segmentShift = getDehydratableObject(segmentShift, objectManager);
      writer.addPhysicalAction(CONCURRENT_HASH_MAP_FIELD_NAME_PREFIX + SEGMENT_SHIFT_FIELD_NAME, segmentShift);

      Object[] segments = (Object[]) SEGMENT_FIELD.get(pojo);
      writer.addLiteralValue(new Integer(segments.length));
      for (int i = 0; i < segments.length; i++) {
        Object segment = segments[i];
        segment = getDehydratableObject(segment, objectManager);
        writer.addPhysicalAction(CONCURRENT_HASH_MAP_FIELD_NAME_PREFIX + SEGMENT_FIELD_NAME + i, segment);
      }
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
  }

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
      try {
        TC_PUT_METHOD.invoke(destMap, new Object[] { copyKey, copyValue });
      } catch (IllegalArgumentException e1) {
        throw new TCRuntimeException(e1);
      } catch (IllegalAccessException e1) {
        throw new TCRuntimeException(e1);
      } catch (InvocationTargetException e1) {
        throw new TCRuntimeException(e1);
      }
    }

    destManaged.__tc_managed(new TCObjectClone(sourceManageable.__tc_managed(), txManager));
    return cloned;
  }
}
