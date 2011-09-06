/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.object.ObjectID;
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
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Apply a logical action to an object
 */
public class ConcurrentHashMapApplicator extends PartialHashMapApplicator {
  private static final String SEGMENT_MASK_FIELD_NAME  = "segmentMask";
  private static final String SEGMENT_SHIFT_FIELD_NAME = "segmentShift";
  private static final String SEGMENT_FIELD_NAME       = "segments";

  private static final Field  SEGMENT_MASK_FIELD;
  private static final Field  SEGMENT_SHIFT_FIELD;
  private static final Field  SEGMENT_FIELD;

  static {
    try {
      SEGMENT_MASK_FIELD = ConcurrentHashMap.class.getDeclaredField(SEGMENT_MASK_FIELD_NAME);
      SEGMENT_MASK_FIELD.setAccessible(true);

      SEGMENT_SHIFT_FIELD = ConcurrentHashMap.class.getDeclaredField(SEGMENT_SHIFT_FIELD_NAME);
      SEGMENT_SHIFT_FIELD.setAccessible(true);

      SEGMENT_FIELD = ConcurrentHashMap.class.getDeclaredField(SEGMENT_FIELD_NAME);
      SEGMENT_FIELD.setAccessible(true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ConcurrentHashMapApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  @Override
  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    super.getPortableObjects(pojo, addTo);
    getPhysicalPortableObjects(pojo, addTo);
    return addTo;
  }

  public void getPhysicalPortableObjects(Object pojo, TraversedReferences addTo) {
    try {
      filterPortableObject(SEGMENT_MASK_FIELD.get(pojo), addTo);
      filterPortableObject(SEGMENT_SHIFT_FIELD.get(pojo), addTo);
      Object[] segments = (Object[]) SEGMENT_FIELD.get(pojo);
      for (Object segment : segments) {
        filterPortableObject(segment, addTo);
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

  @Override
  public void hydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNA dna, Object po)
      throws IOException, ClassNotFoundException {
    Object[] segments = null;
    DNACursor cursor = dna.getCursor();

    while (cursor.next(encoding)) {
      Object action = cursor.getAction();
      if (action instanceof LogicalAction) {
        LogicalAction logicalAction = cursor.getLogicalAction();
        int method = logicalAction.getMethod();
        Object[] params = logicalAction.getParameters();
        apply(objectManager, po, method, params);
      } else if (action instanceof PhysicalAction) {
        // Physical fields are shared only when the DNA is not a delta.
        Assert.assertFalse(dna.isDelta());

        PhysicalAction physicalAction = cursor.getPhysicalAction();
        if (physicalAction.isEntireArray()) {
          segments = (Object[]) physicalAction.getObject();
          segments = resolveReferences(objectManager, segments);
          setSegmentField(segments, po);
        } else {
          Assert.assertTrue(physicalAction.isTruePhysical());
          String fieldName = physicalAction.getFieldName();
          Object value = physicalAction.getObject();
          try {
            if (fieldName.equals(SEGMENT_MASK_FIELD_NAME)) {
              FieldUtils.tcSet(po, value, SEGMENT_MASK_FIELD);
            } else if (fieldName.equals(SEGMENT_SHIFT_FIELD_NAME)) {
              FieldUtils.tcSet(po, value, SEGMENT_SHIFT_FIELD);
            } else {
              throw new AssertionError("Unknown Physical Action : " + physicalAction + " for " + dna.getObjectID());
            }
          } catch (IllegalAccessException e) {
            throw new TCRuntimeException(e);
          }
        }
      }
    }
  }

  private Object[] resolveReferences(ApplicatorObjectManager objectManager, Object[] sids)
      throws ClassNotFoundException {
    Object segment = objectManager.lookupObject((ObjectID) sids[0]);
    Object[] segments = (Object[]) Array.newInstance(segment.getClass(), sids.length);
    segments[0] = segment;
    for (int i = 1; i < sids.length; i++) {
      segments[i] = objectManager.lookupObject((ObjectID) sids[i]);
    }
    return segments;
  }

  private void setSegmentField(Object[] segments, Object po) {
    try {
      FieldUtils.tcSet(po, segments, SEGMENT_FIELD);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
  }

  @Override
  public void dehydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer, Object pojo) {
    dehydrateFields(objectManager, tcObject, writer, pojo);
    super.dehydrate(objectManager, tcObject, writer, pojo);
  }

  private void dehydrateFields(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer,
                               Object pojo) {
    try {
      Object segmentMask = SEGMENT_MASK_FIELD.get(pojo);
      segmentMask = getDehydratableObject(segmentMask, objectManager);
      writer.addPhysicalAction(SEGMENT_MASK_FIELD_NAME, segmentMask);

      Object segmentShift = SEGMENT_SHIFT_FIELD.get(pojo);
      segmentShift = getDehydratableObject(segmentShift, objectManager);
      writer.addPhysicalAction(SEGMENT_SHIFT_FIELD_NAME, segmentShift);

      // XXX::FIXME It is weird that we dont just make this array physically shared, historical reasons
      Object[] segments = (Object[]) SEGMENT_FIELD.get(pojo);
      Object[] segmentIDs = new Object[segments.length];
      for (int i = 0; i < segments.length; i++) {
        segmentIDs[i] = getDehydratableObject(segments[i], objectManager);
      }
      writer.addEntireArray(segmentIDs);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
  }

}
