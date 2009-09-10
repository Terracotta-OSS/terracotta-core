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

  public static final String    SERIALIZED_ENTRY       = "org.terracotta.cache.serialization.SerializedEntry";

  public static final String    CREATE_TIME_FIELD      = SERIALIZED_ENTRY + ".createTime";
  public static final String    LAST_ACCESS_TIME_FIELD = SERIALIZED_ENTRY + ".lastAccessedTime";

  private final long            classID;

  private byte[]                value;
  private int                   createTime;
  private int                   lastAccessedTime;

  public TDCSerializedEntryManagedObjectState(long classID) {
    this.classID = classID;
  }

  @Override
  protected boolean basicEquals(AbstractManagedObjectState o) {
    TDCSerializedEntryManagedObjectState other = (TDCSerializedEntryManagedObjectState) o;

    if (createTime != other.createTime) return false;
    if (lastAccessedTime != other.lastAccessedTime) return false;
    if (!Arrays.equals(value, other.value)) return false;

    return true;
  }

  public void addObjectReferencesTo(ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
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
    }
  }

  /**
   * This method returns whether this ManagedObjectState can have references or not.
   * @ return true : The Managed object represented by this state object will never have any reference to other objects.
   *         false : The Managed object represented by this state object can have references to other objects.
   */
  @Override
  public boolean hasNoReferences() {
    return true;
  }

  private static void logInvalidType(String field, Object val) {
    logger.error("recieved invalid type (" + safeTypeName(val) + "] for " + field + " field -- ignoring it");
  }

  private static String safeTypeName(Object obj) {
    String type = obj == null ? "null" : obj.getClass().getName();
    return type;
  }

  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    // The byte[] value field is not shown in the admin console
    Map data = new HashMap();
    data.put(CREATE_TIME_FIELD, Integer.valueOf(createTime));
    data.put(LAST_ACCESS_TIME_FIELD, Integer.valueOf(lastAccessedTime));

    return new PhysicalManagedObjectFacade(objectID, null, className, data, false, DNA.NULL_ARRAY_SIZE, false);
  }

  public void dehydrate(ObjectID objectID, DNAWriter writer) {
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

  public void writeTo(ObjectOutput out) throws IOException {
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

  static TDCSerializedEntryManagedObjectState readFrom(ObjectInput in) throws IOException {
    TDCSerializedEntryManagedObjectState state = new TDCSerializedEntryManagedObjectState(in.readLong());

    state.createTime = in.readInt();
    state.lastAccessedTime = in.readInt();

    int length = in.readInt();
    if (length >= 0) {
      byte[] data = new byte[length];
      in.read(data, 0, length);
      state.value = data;
    }

    return state;
  }
}
