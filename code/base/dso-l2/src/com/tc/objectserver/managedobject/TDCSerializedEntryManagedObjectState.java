/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.api.DNA.DNAType;
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

  private static final TCLogger logger                     = TCLogging
                                                               .getLogger(TDCSerializedEntryManagedObjectState.class);

  public static final String    SERIALIZED_ENTRY           = "org.terracotta.cache.serialization.SerializedEntry";
  public static final String    ABSTRACT_TIMESTAMPED_VALUE = "org.terracotta.cache.value.AbstractTimestampedValue";

  public static final String    CREATE_TIME_FIELD          = SERIALIZED_ENTRY + ".createTime";
  public static final String    LAST_ACCESS_TIME_FIELD     = ABSTRACT_TIMESTAMPED_VALUE + ".lastAccessedTime";

  private final long            classID;

  private byte[]                value;
  private int                   createTime;
  private int                   lastAccessedTime;

  public TDCSerializedEntryManagedObjectState(final long classID) {
    this.classID = classID;
  }

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState o) {
    TDCSerializedEntryManagedObjectState other = (TDCSerializedEntryManagedObjectState) o;

    if (createTime != other.createTime) return false;
    if (lastAccessedTime != other.lastAccessedTime) return false;
    if (!Arrays.equals(value, other.value)) return false;

    return true;
  }

  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  public void apply(final ObjectID objectID, final DNACursor cursor, final BackReferences includeIDs) throws IOException {
    while (cursor.next()) {
      PhysicalAction pa = cursor.getPhysicalAction();
      if (pa.isEntireArray()) {
        Object array = pa.getObject();
        if (array instanceof byte[]) {
          value = (byte[]) array;
        } else {
          String type = safeTypeName(array);
          logger.error("received array of type " + type + " -- ignoring it");
        }
      } else {
        physicalActionApply(pa);
      }
    }
  }

  protected void physicalActionApply(PhysicalAction pa) {
    Object val = pa.getObject();
    String field = pa.getFieldName();
    // last access time is the only field that should be updated, check it first
    if (LAST_ACCESS_TIME_FIELD.equals(field)) {
      if (val instanceof Integer) {
        lastAccessedTime = ((Integer) val).intValue();
      } else {
        logInvalidType(LAST_ACCESS_TIME_FIELD, val);
      }
    } else if (CREATE_TIME_FIELD.equals(field)) {
      if (val instanceof Integer) {
        createTime = ((Integer) val).intValue();
      } else {
        logInvalidType(CREATE_TIME_FIELD, val);
      }
    } else {
      logger.error("recieved data for field named [" + field + "] -- ignoring it");
    }
  }
  
  /**
   * This method returns whether this ManagedObjectState can have references or not. @ return true : The Managed object
   * represented by this state object will never have any reference to other objects. false : The Managed object
   * represented by this state object can have references to other objects.
   */
  @Override
  public boolean hasNoReferences() {
    return true;
  }

  protected static void logInvalidType(final String field, final Object val) {
    logger.error("recieved invalid type (" + safeTypeName(val) + "] for " + field + " field -- ignoring it");
  }

  private static String safeTypeName(final Object obj) {
    String type = obj == null ? "null" : obj.getClass().getName();
    return type;
  }

  public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
    Map<String, Object> fields = addFacadeFields(new HashMap<String, Object>());
    return new PhysicalManagedObjectFacade(objectID, null, className, fields, false, DNA.NULL_ARRAY_SIZE, false);
  }

  protected Map<String, Object> addFacadeFields(Map<String, Object> fields) {
    // The byte[] value field is not shown in the admin console
    fields.put(CREATE_TIME_FIELD, Integer.valueOf(createTime));
    fields.put(LAST_ACCESS_TIME_FIELD, Integer.valueOf(lastAccessedTime));
    return fields;
  }
  
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, DNAType type) {
    writer.addEntireArray(value);
    writer.addPhysicalAction(CREATE_TIME_FIELD, Integer.valueOf(createTime));
    writer.addPhysicalAction(LAST_ACCESS_TIME_FIELD, Integer.valueOf(lastAccessedTime));
  }

  public final String getClassName() {
    return getStateFactory().getClassName(classID);
  }

  public final String getLoaderDescription() {
    return getStateFactory().getLoaderDescription(classID);
  }

  public Set getObjectReferences() {
    return Collections.EMPTY_SET;
  }

  public byte getType() {
    return TDC_SERIALIZED_ENTRY;
  }

  public void writeTo(final ObjectOutput out) throws IOException {
    out.writeLong(classID);
    out.writeInt(createTime);
    out.writeInt(lastAccessedTime);
    if (value != null) {
      out.writeInt(value.length);
      out.write(value, 0, value.length);
    } else {
      out.writeInt(-1);
    }
  }

  static TDCSerializedEntryManagedObjectState readFrom(final ObjectInput in) throws IOException {
    TDCSerializedEntryManagedObjectState state = new TDCSerializedEntryManagedObjectState(in.readLong());
    state.readFromInternal(in);
    return state;
  }
  
  protected void readFromInternal(final ObjectInput in) throws IOException {
    this.createTime = in.readInt();
    this.lastAccessedTime = in.readInt();

    int length = in.readInt();
    if (length >= 0) {
      byte[] data = new byte[length];
      in.read(data, 0, length);
      this.value = data;
    }
  }
}
