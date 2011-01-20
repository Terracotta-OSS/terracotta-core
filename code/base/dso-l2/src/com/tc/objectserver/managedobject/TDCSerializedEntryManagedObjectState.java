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
import com.tc.object.dna.api.PhysicalAction;
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

  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo includeIDs)
      throws IOException {
    while (cursor.next()) {
      final PhysicalAction pa = cursor.getPhysicalAction();
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

  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    writer.addEntireArray(this.value);
    writer.addPhysicalAction(CREATE_TIME_FIELD, Integer.valueOf(this.createTime));
    writer.addPhysicalAction(LAST_ACCESS_TIME_FIELD, Integer.valueOf(this.lastAccessedTime));
  }

  public final String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  public final String getLoaderDescription() {
    return getStateFactory().getLoaderDescription(this.classID);
  }

  public Set getObjectReferences() {
    return Collections.EMPTY_SET;
  }

  public byte getType() {
    return TDC_SERIALIZED_ENTRY;
  }

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
      in.read(data, 0, length);
      this.value = data;
    }
  }
}
