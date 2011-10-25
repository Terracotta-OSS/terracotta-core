/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.PhysicalAction;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

public class TDCCustomLifespanSerializedEntryManagedObjectState extends TDCSerializedEntryManagedObjectState {

  public static final String CUSTOM_SERIALIZED_ENTRY = "org.terracotta.cache.serialization.CustomLifespanSerializedEntry";

  public static final String CUSTOM_TTI_FIELD        = CUSTOM_SERIALIZED_ENTRY + ".customTti";
  public static final String CUSTOM_TTL_FIELD        = CUSTOM_SERIALIZED_ENTRY + ".customTtl";

  private int                customTti;
  private int                customTtl;

  public TDCCustomLifespanSerializedEntryManagedObjectState(final long classID) {
    super(classID);
  }

  @Override
  public int expiresIn(int now, int ttiSeconds, int ttlSeconds) {
    return computeExpiresIn(now, this.customTti, this.customTtl);
  }

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState o) {
    final TDCCustomLifespanSerializedEntryManagedObjectState other = (TDCCustomLifespanSerializedEntryManagedObjectState) o;

    if (this.customTti != other.customTti) { return false; }
    if (this.customTtl != other.customTtl) { return false; }

    return super.basicEquals(o);
  }

  @Override
  protected void physicalActionApply(final PhysicalAction pa) {
    final Object val = pa.getObject();
    final String field = pa.getFieldName();
    if (CUSTOM_TTI_FIELD.equals(field)) {
      if (val instanceof Integer) {
        this.customTti = ((Integer) val).intValue();
      } else {
        logInvalidType(CUSTOM_TTI_FIELD, val);
      }
    } else if (CUSTOM_TTL_FIELD.equals(field)) {
      if (val instanceof Integer) {
        this.customTtl = ((Integer) val).intValue();
      } else {
        logInvalidType(CUSTOM_TTL_FIELD, val);
      }
    } else {
      super.physicalActionApply(pa);
    }
  }

  @Override
  protected Map<String, Object> addFacadeFields(Map<String, Object> fields) {
    // The byte[] value field is not shown in the admin console
    fields = super.addFacadeFields(fields);
    fields.put(CUSTOM_TTI_FIELD, Integer.valueOf(this.customTti));
    fields.put(CUSTOM_TTL_FIELD, Integer.valueOf(this.customTtl));
    return fields;
  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    super.dehydrate(objectID, writer, type);
    writer.addPhysicalAction(CUSTOM_TTI_FIELD, Integer.valueOf(this.customTti));
    writer.addPhysicalAction(CUSTOM_TTL_FIELD, Integer.valueOf(this.customTtl));
  }

  @Override
  public byte getType() {
    return TDC_CUSTOM_LIFESPAN_SERIALIZED_ENTRY;
  }

  @Override
  public void writeTo(final ObjectOutput out) throws IOException {
    super.writeTo(out);
    out.writeInt(this.customTti);
    out.writeInt(this.customTtl);
  }

  static TDCSerializedEntryManagedObjectState readFrom(final ObjectInput in) throws IOException {
    final TDCCustomLifespanSerializedEntryManagedObjectState state = new TDCCustomLifespanSerializedEntryManagedObjectState(
                                                                                                                            in.readLong());
    state.readFromInternal(in);
    return state;
  }

  @Override
  protected void readFromInternal(final ObjectInput in) throws IOException {
    super.readFromInternal(in);
    this.customTti = in.readInt();
    this.customTtl = in.readInt();
  }
}
