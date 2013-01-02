/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.objectserver.api.EvictableEntry;
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

public class TDCSerializedEntryManagedObjectState extends AbstractManagedObjectState implements EvictableEntry {

  private static final TCLogger logger                 = TCLogging
                                                           .getLogger(TDCSerializedEntryManagedObjectState.class);

  public static final String    CREATE_TIME_FIELD      = "createTime";
  public static final String    LAST_ACCESS_TIME_FIELD = "lastAccessedTime";

  private final long            classID;

  private byte[]                value;
  private int                   createTime;
  private int                   lastAccessedTime;

  public TDCSerializedEntryManagedObjectState(final long classID) {
    this.classID = classID;
  }

  @Override
  public int expiresIn(int now, int ttiSeconds, int ttlSeconds) {
    return computeExpiresIn(now, ttiSeconds, ttlSeconds);
  }

  protected int computeExpiresIn(int now, int ttiSeconds, int ttlSeconds) {
    if (ttiSeconds <= 0 && ttlSeconds <= 0) {
      // This is eternal. Also most likely the lastAccessedTime is not updated from tim-ehcache. We return a number that
      // is proportionate to its age or access time
      int lastTime = Math.max(createTime, lastAccessedTime);
      return Integer.MAX_VALUE - (now > lastTime ? now - lastTime : 0);
    }
    final int expiresAtTTI = ttiSeconds <= 0 ? Integer.MAX_VALUE : this.lastAccessedTime + ttiSeconds;
    final int expiresAtTTL = ttlSeconds <= 0 ? Integer.MAX_VALUE : this.createTime + ttlSeconds;
    return Math.min(expiresAtTTI, expiresAtTTL) - now;
  }

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState o) {
    final TDCSerializedEntryManagedObjectState other = (TDCSerializedEntryManagedObjectState) o;

    if (this.createTime != other.createTime) { return false; }
    if (this.lastAccessedTime != other.lastAccessedTime) { return false; }
    if (!Arrays.equals(this.value, other.value)) { return false; }

    return true;
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
          } else {
            final String type = safeTypeName(array);
            logger.error("received array of type " + type + " -- ignoring it");
          }
        } else {
          physicalActionApply(pa);
        }
      } else {
        final LogicalAction la = (LogicalAction) action;
        switch (la.getMethod()) {
          case SerializationUtil.FIELD_CHANGED:
            Object[] parameters = la.getParameters();
            String fieldName = getString(parameters[0]);
            if (LAST_ACCESS_TIME_FIELD.equals(fieldName)) {
              this.lastAccessedTime = (Integer) parameters[1];
            } else {
              throw new AssertionError("Got unsupported logical change for field: " + fieldName + ", parameters: "
                                       + parameters);
            }
            break;
          default:
            throw new AssertionError("Unknown logical action - " + la);
        }
      }
    }
  }

  private static String getString(Object param) {
    if (param instanceof UTF8ByteDataHolder) {
      return ((UTF8ByteDataHolder) param).asString();
    } else if (param instanceof String) {
      return (String) param;
    } else {
      throw new AssertionError("Not a string value: " + safeTypeName(param) + " - " + param);
    }
  }

  protected void physicalActionApply(final PhysicalAction pa) {
    final Object val = pa.getObject();
    final String field = pa.getFieldName();
    // last access time is the only field that should be updated, check it first
    if (LAST_ACCESS_TIME_FIELD.equals(field)) {
      if (val instanceof Integer) {
        this.lastAccessedTime = ((Integer) val).intValue();
      } else {
        logInvalidType(LAST_ACCESS_TIME_FIELD, val);
      }
    } else if (CREATE_TIME_FIELD.equals(field)) {
      if (val instanceof Integer) {
        this.createTime = ((Integer) val).intValue();
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
    final String type = obj == null ? "null" : obj.getClass().getName();
    return type;
  }

  @Override
  public ManagedObjectFacade createFacade(final ObjectID objectID, final String className, final int limit) {
    final Map<String, Object> fields = addFacadeFields(new HashMap<String, Object>());
    return new PhysicalManagedObjectFacade(objectID, null, className, fields, false, DNA.NULL_ARRAY_SIZE, false);
  }

  protected Map<String, Object> addFacadeFields(final Map<String, Object> fields) {
    // The byte[] value field is not shown in the admin console
    fields.put(CREATE_TIME_FIELD, Integer.valueOf(this.createTime));
    fields.put(LAST_ACCESS_TIME_FIELD, Integer.valueOf(this.lastAccessedTime));
    return fields;
  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    writer.addEntireArray(this.value);
    writer.addPhysicalAction(CREATE_TIME_FIELD, Integer.valueOf(this.createTime));
    writer.addPhysicalAction(LAST_ACCESS_TIME_FIELD, Integer.valueOf(this.lastAccessedTime));
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
    out.writeInt(this.createTime);
    out.writeInt(this.lastAccessedTime);
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
    this.createTime = in.readInt();
    this.lastAccessedTime = in.readInt();

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
    result = prime * result + createTime;
    result = prime * result + lastAccessedTime;
    result = prime * result + Arrays.hashCode(value);
    return result;
  }

  @Override
  public String toString() {
    return "TDCSerializedEntryManagedObjectState{" +
           "classID=" + classID +
           ", value=" + Arrays.toString(value) +
           ", createTime=" + createTime +
           ", lastAccessedTime=" + lastAccessedTime +
           '}';
  }
}
