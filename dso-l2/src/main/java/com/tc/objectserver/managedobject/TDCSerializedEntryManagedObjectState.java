/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.PhysicalManagedObjectFacade;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TDCSerializedEntryManagedObjectState extends AbstractManagedObjectState {

  private static final TCLogger logger                 = TCLogging
                                                           .getLogger(TDCSerializedEntryManagedObjectState.class);

  private final long            classID;

  private byte[]                value;

  public TDCSerializedEntryManagedObjectState(final long classID) {
    this.classID = classID;
  }

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState o) {
    final TDCSerializedEntryManagedObjectState other = (TDCSerializedEntryManagedObjectState) o;

    return Arrays.equals(this.value, other.value);
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  @Override
  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo includeIDs)
      throws IOException {
    while (cursor.next()) {
      Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        final PhysicalAction pa = (PhysicalAction) action;
        if (pa.isEntireArray()) {
          final Object array = pa.getObject();
          if (array instanceof byte[]) {
            this.value = (byte[]) array;
            // collect [oid, value] pair for futher broadcasting
            includeIDs.getModificationRecorder().recordOperationValue(objectID, value);
          } else {
            final String type = safeTypeName(array);
            logger.error("received array of type " + type + " -- ignoring it");
          }
        } else {
          physicalActionApply(pa);
        }
      } else {
        final LogicalAction la = (LogicalAction) action;
        throw new AssertionError("Unknown logical action - " + la);
      }
    }
  }

  protected void physicalActionApply(final PhysicalAction pa) {
    final String field = pa.getFieldName();
    throw new IllegalArgumentException("recieved data for field named [" + field + "] -- ignoring it");
  }

  protected static void logInvalidType(final String field, final Object val) {
    logger.error("recieved invalid type (" + safeTypeName(val) + "] for " + field + " field -- ignoring it");
  }

  private static String safeTypeName(final Object obj) {
    final String type = obj == null ? "null" : obj.getClass().getName();
    return type;
  }

  @Override
  public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
    final Map<String, Object> fields = addFacadeFields(new HashMap<String, Object>());
    return new PhysicalManagedObjectFacade(objectID, null, className, fields, false, DNA.NULL_ARRAY_SIZE, false);
  }

  protected Map<String, Object> addFacadeFields(final Map<String, Object> fields) {
    return fields;
  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    writer.addEntireArray(this.value);
  }

  @Override
  public final String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  @Override
  public Set getObjectReferences() {
    return Collections.EMPTY_SET;
  }

  @Override
  public byte getType() {
    return TDC_SERIALIZED_ENTRY;
  }

  @Override
  public void writeTo(final ObjectOutput out) throws IOException {
    out.writeLong(this.classID);
    if (this.value != null) {
      out.writeInt(this.value.length);
      out.write(this.value, 0, this.value.length);
    } else {
      out.writeInt(-1);
    }
  }

  static TDCSerializedEntryManagedObjectState readFrom(final ObjectInput in) throws IOException {
    final TDCSerializedEntryManagedObjectState state = new TDCSerializedEntryManagedObjectState(in.readLong());
    state.readFromInternal(in);
    return state;
  }

  protected void readFromInternal(final ObjectInput in) throws IOException {

    final int length = in.readInt();
    if (length >= 0) {
      final byte[] data = new byte[length];
      for (int pos = 0; pos < length;) {
        int read = in.read(data, pos, length - pos);
        if (read == -1) {
          break;
        }
        pos += read;
      }
      this.value = data;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (classID ^ (classID >>> 32));
    result = prime * result + Arrays.hashCode(value);
    return result;
  }

  @Override
  public String toString() {
    return "TDCSerializedEntryManagedObjectState{" +
           "classID=" + classID +
           ", value=" + Arrays.toString(value) +
           '}';
  }
}
