package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
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
public class ConcurrentStringMapManagedObjectState extends PartialMapManagedObjectState {
  public static final String DSO_LOCK_TYPE_FIELDNAME = "dsoLockType";
  public static final String LOCK_STRATEGY_FIELDNAME = "lockStrategy";

  private int                dsoLockType;
  private ObjectID           lockStrategy;

  private ConcurrentStringMapManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  protected ConcurrentStringMapManagedObjectState(long classId, Map map) {
    super(classId, map);
  }

  @Override
  public byte getType() {
    return CONCURRENT_STRING_MAP_TYPE;
  }

  @Override
  protected void addAllObjectReferencesTo(Set refs) {
    super.addAllObjectReferencesTo(refs);
    if (!lockStrategy.isNull()) {
      refs.add(lockStrategy);
    }
  }

  @Override
  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    while (cursor.next()) {
      Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        PhysicalAction physicalAction = (PhysicalAction) action;

        String fieldName = physicalAction.getFieldName();
        if (fieldName.equals(DSO_LOCK_TYPE_FIELDNAME)) {
          dsoLockType = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(LOCK_STRATEGY_FIELDNAME)) {
          ObjectID newLockStrategy = (ObjectID) physicalAction.getObject();
          getListener().changed(objectID, lockStrategy, newLockStrategy);
          lockStrategy = newLockStrategy;
        } else {
          throw new AssertionError("unexpected field name: " + fieldName);
        }
      } else {
        LogicalAction logicalAction = (LogicalAction) action;
        int method = logicalAction.getMethod();
        Object[] params = logicalAction.getParameters();
        applyMethod(objectID, includeIDs, method, params);
      }
    }
  }

  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeInt(dsoLockType);
    out.writeLong(lockStrategy.getObjectID());
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer) {
    writer.addPhysicalAction(DSO_LOCK_TYPE_FIELDNAME, Integer.valueOf(dsoLockType));
    writer.addPhysicalAction(LOCK_STRATEGY_FIELDNAME, lockStrategy);
    super.dehydrate(objectID, writer);
  }

  static MapManagedObjectState readFrom(ObjectInput in) throws IOException {
    ConcurrentStringMapManagedObjectState csmMos = new ConcurrentStringMapManagedObjectState(in);
    csmMos.dsoLockType = in.readInt();
    csmMos.lockStrategy = new ObjectID(in.readLong());
    return csmMos;
  }
}
