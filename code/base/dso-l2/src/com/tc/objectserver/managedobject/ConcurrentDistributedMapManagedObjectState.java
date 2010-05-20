package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.bytecode.NotClearable;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.Set;

// XXX: This is a rather ugly hack to get around the requirements of tim-concurrent-collections.
public class ConcurrentDistributedMapManagedObjectState extends PartialMapManagedObjectState implements NotClearable {
  public static final String DSO_LOCK_TYPE_FIELDNAME = "dsoLockType";
  public static final String LOCK_STRATEGY_FIELDNAME = "lockStrategy";

  private int                dsoLockType;
  private ObjectID           lockStrategy;

  protected ConcurrentDistributedMapManagedObjectState(final ObjectInput in) throws IOException {
    super(in);
    this.dsoLockType = in.readInt();
    this.lockStrategy = new ObjectID(in.readLong());
  }

  protected ConcurrentDistributedMapManagedObjectState(final long classId, final Map map) {
    super(classId, map);
  }

  @Override
  public byte getType() {
    return CONCURRENT_DISTRIBUTED_MAP_TYPE;
  }

  @Override
  protected void addAllObjectReferencesTo(final Set refs) {
    super.addAllObjectReferencesTo(refs);
    if (!this.lockStrategy.isNull()) {
      refs.add(this.lockStrategy);
    }
  }

  @Override
  public void apply(final ObjectID objectID, final DNACursor cursor, final BackReferences includeIDs)
      throws IOException {
    while (cursor.next()) {
      final Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        final PhysicalAction physicalAction = (PhysicalAction) action;

        final String fieldName = physicalAction.getFieldName();
        if (fieldName.equals(DSO_LOCK_TYPE_FIELDNAME)) {
          this.dsoLockType = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(LOCK_STRATEGY_FIELDNAME)) {
          final ObjectID newLockStrategy = (ObjectID) physicalAction.getObject();
          getListener().changed(objectID, this.lockStrategy, newLockStrategy);
          this.lockStrategy = newLockStrategy;
        } else {
          throw new AssertionError("unexpected field name: " + fieldName);
        }
      } else {
        final LogicalAction logicalAction = (LogicalAction) action;
        final int method = logicalAction.getMethod();
        final Object[] params = logicalAction.getParameters();
        applyMethod(objectID, includeIDs, method, params);
      }
    }
  }

  @Override
  protected void basicWriteTo(final ObjectOutput out) throws IOException {
    out.writeInt(this.dsoLockType);
    out.writeLong(this.lockStrategy.toLong());
  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer) {
    writer.addPhysicalAction(DSO_LOCK_TYPE_FIELDNAME, Integer.valueOf(this.dsoLockType));
    writer.addPhysicalAction(LOCK_STRATEGY_FIELDNAME, this.lockStrategy);
    super.dehydrate(objectID, writer);
  }

  static MapManagedObjectState readFrom(final ObjectInput in) throws IOException {
    final ConcurrentDistributedMapManagedObjectState cdmMos = new ConcurrentDistributedMapManagedObjectState(in);
    return cdmMos;
  }
}
