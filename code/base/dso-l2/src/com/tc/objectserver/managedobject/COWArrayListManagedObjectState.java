/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

public class COWArrayListManagedObjectState extends ListManagedObjectState {
  private static final String LOCK_FIELD_NAME = "java.util.concurrent.CopyOnWriteArrayList.lock";

  private ObjectID            lockField = null;

  COWArrayListManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  protected COWArrayListManagedObjectState(long classID) {
    super(classID);
  }

  @Override
  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    while (cursor.next()) {
      Object action = cursor.getAction();
      if (action instanceof LogicalAction) {
        LogicalAction logicalAction = (LogicalAction) action;
        int method = logicalAction.getMethod();
        Object[] params = logicalAction.getParameters();
        applyOperation(method, objectID, includeIDs, params);
      } else {
        PhysicalAction physicalAction = (PhysicalAction) action;
        updateReference(objectID, physicalAction.getFieldName(), physicalAction.getObject(), includeIDs);
      }
    }
  }

  private void updateReference(ObjectID objectID, String fieldName, Object value, BackReferences includeIDs) {
    if (LOCK_FIELD_NAME.equals(fieldName)) {
      lockField = (ObjectID) value;
      getListener().changed(objectID, null, lockField);
      includeIDs.addBackReference(lockField, objectID);
    }
  }
  
  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer) {
    if (lockField != null) {
      writer.addPhysicalAction(LOCK_FIELD_NAME, lockField);
    }
    super.dehydrate(objectID, writer);
  }
  
  @Override
  public String toString() {
    return "COWArrayListManagedStateObject(" + references + ")";
  }
  
  public byte getType() {
    return COPY_ON_WRITE_ARRAY_LIST_TYPE;
  }
  
  static ListManagedObjectState readFrom(ObjectInput in) throws IOException {
    COWArrayListManagedObjectState cm = new COWArrayListManagedObjectState(in);
    if (in.readBoolean()) {
      cm.lockField = new ObjectID(in.readLong());
    }

    return cm;
  }
  
  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    // XXX: This facade is incomplete...it doesn't include the lock field
    return super.createFacade(objectID,className, limit);
  }
  
  @Override
  protected void addAllObjectReferencesTo(Set refs) {
    super.addAllObjectReferencesTo(refs);
    if (lockField != null) {
      refs.add(lockField);
    }
  }
  
  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    if (lockField == null) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      out.writeLong(lockField.toLong());
    }
  }
  
  @Override
  protected boolean basicEquals(LogicalManagedObjectState o) {
    COWArrayListManagedObjectState co = (COWArrayListManagedObjectState) o;
    return (lockField == co.lockField || (lockField != null && lockField.equals(co.lockField)))
           && super.basicEquals(o);
  }
}
