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

// XXX: This is a rather ugly hack to get around the requirements of tim-concurrent-collections.
public class ConcurrentStringMapManagedObjectState extends PartialMapManagedObjectState {
  public static final String DSO_LOCK_TYPE_FIELDNAME     = "dsoLockType";
  public static final String HASH_CODE_LOCKING_FIELDNAME = "hashCodeLocking";

  private int                dsoLockType;
  private boolean            hashCodeLocking;

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
  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    while (cursor.next()) {
      Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        PhysicalAction physicalAction = (PhysicalAction) action;

        String fieldName = physicalAction.getFieldName();
        if (fieldName.equals(DSO_LOCK_TYPE_FIELDNAME)) {
          dsoLockType = ((Integer) physicalAction.getObject());
        } else if (fieldName.equals(HASH_CODE_LOCKING_FIELDNAME)) {
          hashCodeLocking = ((Boolean) physicalAction.getObject());
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
    out.writeBoolean(hashCodeLocking);
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer) {
    writer.addPhysicalAction(DSO_LOCK_TYPE_FIELDNAME, Integer.valueOf(dsoLockType));
    writer.addPhysicalAction(HASH_CODE_LOCKING_FIELDNAME, Boolean.valueOf(hashCodeLocking));
    super.dehydrate(objectID, writer);
  }

  static MapManagedObjectState readFrom(ObjectInput in) throws IOException {
    ConcurrentStringMapManagedObjectState csmMos = new ConcurrentStringMapManagedObjectState(in);
    csmMos.dsoLockType = in.readInt();
    csmMos.hashCodeLocking = in.readBoolean();
    return csmMos;
  }
}
