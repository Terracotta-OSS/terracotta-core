/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * state for maps
 */
public class ConcurrentHashMapManagedObjectState extends PartialMapManagedObjectState {
  public static final String SEGMENT_MASK_FIELD_NAME  = "segmentMask";
  public static final String SEGMENT_SHIFT_FIELD_NAME = "segmentShift";

  private Object             segmentMask;
  private Object             segmentShift;
  private ObjectID[]         segments                 = null;

  private ConcurrentHashMapManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  protected ConcurrentHashMapManagedObjectState(long classID, Map map) {
    super(classID, map);
  }

  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo includeIDs) throws IOException {
    while (cursor.next()) {
      Object action = cursor.getAction();
      if (action instanceof LogicalAction) {
        LogicalAction logicalAction = (LogicalAction) action;
        int method = logicalAction.getMethod();
        Object[] params = logicalAction.getParameters();
        applyMethod(objectID, includeIDs, method, params);
      } else if (action instanceof PhysicalAction) {
        PhysicalAction physicalAction = (PhysicalAction) action;
        if (physicalAction.isEntireArray()) {
          initalizeSegments(objectID, (Object[]) physicalAction.getObject(), includeIDs);
        } else {
          String fieldName = physicalAction.getFieldName();
          if (SEGMENT_MASK_FIELD_NAME.equals(fieldName)) {
            segmentMask = physicalAction.getObject();
          } else if (SEGMENT_SHIFT_FIELD_NAME.equals(fieldName)) {
            segmentShift = physicalAction.getObject();
          }
        }
      }
    }
  }

  private void initalizeSegments(ObjectID objectID, Object[] array, ApplyTransactionInfo includeIDs) {
    Assert.assertNull(segments);
    segments = new ObjectID[array.length];
    for (int i = 0; i < array.length; i++) {
      segments[i] = (ObjectID) array[i];
      getListener().changed(objectID, null, segments[i]);
      includeIDs.addBackReference(segments[i], objectID);
    }
  }

  protected void addAllObjectReferencesTo(Set refs) {
    super.addAllObjectReferencesTo(refs);
    if (segments != null) {
      for (int i = 0; i < segments.length; i++) {
        refs.add(segments[i]);
      }
    }
  }

  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    dehydrateFields(objectID, writer);
    super.dehydrate(objectID, writer, type);
  }

  private void dehydrateFields(ObjectID objectID, DNAWriter writer) {
    writer.addPhysicalAction(SEGMENT_MASK_FIELD_NAME, segmentMask);
    writer.addPhysicalAction(SEGMENT_SHIFT_FIELD_NAME, segmentShift);
    writer.addEntireArray(segments);
  }

  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeObject(segmentMask);
    out.writeObject(segmentShift);
    out.writeInt(segments.length);
    for (int i = 0; i < segments.length; i++) {
      out.writeLong(segments[i].toLong());
    }
  }

  static MapManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    ConcurrentHashMapManagedObjectState mo = new ConcurrentHashMapManagedObjectState(in);
    mo.segmentMask = in.readObject();
    mo.segmentShift = in.readObject();

    int segmentLength = in.readInt();
    mo.segments = new ObjectID[segmentLength];
    for (int i = 0; i < segmentLength; i++) {
      mo.segments[i] = new ObjectID(in.readLong());
    }
    return mo;
  }

  public byte getType() {
    return CONCURRENT_HASHMAP_TYPE;
  }

  protected boolean basicEquals(LogicalManagedObjectState o) {
    ConcurrentHashMapManagedObjectState mo = (ConcurrentHashMapManagedObjectState) o;

    return ((segmentMask == mo.segmentMask) || (segmentMask != null && segmentMask.equals(mo.segmentMask)))
           && ((segmentShift == mo.segmentShift) || (segmentShift != null && segmentShift.equals(mo.segmentShift)))
           && ((segments == mo.segments) || (segments != null && Arrays.equals(segments, mo.segments)))
           && super.basicEquals(o);
  }
}