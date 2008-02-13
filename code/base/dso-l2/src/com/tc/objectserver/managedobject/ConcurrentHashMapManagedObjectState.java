/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;

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
  private static final String SEGMENT_MASK_FIELD_NAME  = "java.util.concurrent.ConcurrentHashMap.segmentMask";
  private static final String SEGMENT_SHIFT_FIELD_NAME = "java.util.concurrent.ConcurrentHashMap.segmentShift";
  private static final String SEGMENT_FIELD_NAME       = "java.util.concurrent.ConcurrentHashMap.segments";

  private Object              segmentMask;
  private Object              segmentShift;
  private ObjectID[]          segments                 = null;

  private ConcurrentHashMapManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  protected ConcurrentHashMapManagedObjectState(long classID, Map map) {
    super(classID, map);
  }

  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    int segmentIndex = 0;
    while (cursor.next()) {
      Object action = cursor.getAction();
      if (action instanceof LogicalAction) {
        LogicalAction logicalAction = (LogicalAction) action;
        int method = logicalAction.getMethod();
        Object[] params = logicalAction.getParameters();
        applyMethod(objectID, includeIDs, method, params);
      } else if (action instanceof LiteralAction) {
        LiteralAction literalAction = (LiteralAction) action;
        segments = new ObjectID[((Integer) literalAction.getObject()).intValue()];
      } else if (action instanceof PhysicalAction) {
        PhysicalAction physicalAction = (PhysicalAction) action;
        boolean updateSegment = updateReference(objectID, physicalAction.getFieldName(), physicalAction.getObject(),
                                                segmentIndex, includeIDs);
        if (updateSegment) {
          segmentIndex++;
        }
      }
    }
  }

  private boolean updateReference(ObjectID objectID, String fieldName, Object value, int segmentIndex,
                                  BackReferences includeIDs) {
    if (SEGMENT_MASK_FIELD_NAME.equals(fieldName)) {
      segmentMask = value;
      return false;
    } else if (SEGMENT_SHIFT_FIELD_NAME.equals(fieldName)) {
      segmentShift = value;
      return false;
    } else if ((SEGMENT_FIELD_NAME + segmentIndex).equals(fieldName)) {
      segments[segmentIndex] = (ObjectID) value;
      getListener().changed(objectID, null, segments[segmentIndex]);
      includeIDs.addBackReference(segments[segmentIndex], objectID);
      return true;
    } else {
      return false;
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

  public void dehydrate(ObjectID objectID, DNAWriter writer) {
    dehydrateFields(objectID, writer);
    super.dehydrate(objectID, writer);
  }

  private void dehydrateFields(ObjectID objectID, DNAWriter writer) {
    writer.addPhysicalAction(SEGMENT_MASK_FIELD_NAME, segmentMask);
    writer.addPhysicalAction(SEGMENT_SHIFT_FIELD_NAME, segmentShift);

    writer.addLiteralValue(new Integer(segments.length));
    for (int i = 0; i < segments.length; i++) {
      ObjectID segment = segments[i];
      writer.addPhysicalAction(SEGMENT_FIELD_NAME + i, segment);
    }
  }

  private void writeField(ObjectOutput out, String fieldName, Object fieldValue) throws IOException {
    out.writeUTF(fieldName);
    if (fieldValue == null) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      if (fieldValue instanceof ObjectID) {
        out.writeLong(((ObjectID) fieldValue).toLong());
      } else {
        out.writeObject(fieldValue);
      }
    }
  }

  protected void basicWriteTo(ObjectOutput out) throws IOException {
    writeField(out, SEGMENT_MASK_FIELD_NAME, segmentMask);
    writeField(out, SEGMENT_SHIFT_FIELD_NAME, segmentShift);
    if (segments == null) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      out.writeInt(segments.length);
      for (int i = 0; i < segments.length; i++) {
        writeField(out, SEGMENT_FIELD_NAME + i, segments[i]);
      }
    }
  }

  private static void readField(ObjectInput in, ConcurrentHashMapManagedObjectState mo, int index)
      throws ClassNotFoundException, IOException {
    String fieldName = in.readUTF();
    boolean fieldExist = in.readBoolean();
    if (fieldExist) {
      if (fieldName.equals(SEGMENT_MASK_FIELD_NAME)) {
        mo.segmentMask = in.readObject();
      } else if (fieldName.equals(SEGMENT_SHIFT_FIELD_NAME)) {
        mo.segmentShift = in.readObject();
      } else if (fieldName.equals((SEGMENT_FIELD_NAME + index))) {
        mo.segments[index] = new ObjectID(in.readLong());
      } else {
        throw new AssertionError("Field not recognized in ConcurrentHashMapManagedObjectState.readFrom().");
      }
    }
  }

  static MapManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    ConcurrentHashMapManagedObjectState mo = new ConcurrentHashMapManagedObjectState(in);
    readField(in, mo, -1);
    readField(in, mo, -1);

    boolean segmentExist = in.readBoolean();
    if (segmentExist) {
      int segmentLength = in.readInt();
      mo.segments = new ObjectID[segmentLength];
      for (int i = 0; i < segmentLength; i++) {
        readField(in, mo, i);
      }
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