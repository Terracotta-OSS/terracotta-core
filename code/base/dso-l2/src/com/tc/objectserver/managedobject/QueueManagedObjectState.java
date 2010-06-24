/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Server representation of a queue
 */
public class QueueManagedObjectState extends LogicalManagedObjectState {
  private static final String TAKE_LOCK_FIELD_NAME = "takeLock";
  private static final String PUT_LOCK_FIELD_NAME  = "putLock";
  private static final String CAPACITY_FIELD_NAME  = "capacity";

  private ObjectID            takeLockField;
  private ObjectID            putLockField;
  private Object              capacityField;

  private List                references;

  QueueManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  protected QueueManagedObjectState(long classID) {
    super(classID);
    references = new LinkedList();
  }

  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo includeIDs) throws IOException {
    while (cursor.next()) {
      Object action = cursor.getAction();
      if (action instanceof LogicalAction) {
        LogicalAction logicalAction = (LogicalAction) action;
        int method = logicalAction.getMethod();
        Object[] params = logicalAction.getParameters();
        applyMethod(objectID, includeIDs, method, params);
      } else if (action instanceof PhysicalAction) {
        PhysicalAction physicalAction = (PhysicalAction) action;
        updateReference(objectID, physicalAction.getFieldName(), physicalAction.getObject(), includeIDs);
      }
    }
  }

  private void updateReference(ObjectID objectID, String fieldName, Object value, ApplyTransactionInfo includeIDs) {
    if (TAKE_LOCK_FIELD_NAME.equals(fieldName)) {
      takeLockField = (ObjectID) value;
      getListener().changed(objectID, null, takeLockField);
      includeIDs.addBackReference(takeLockField, objectID);
    } else if (PUT_LOCK_FIELD_NAME.equals(fieldName)) {
      putLockField = (ObjectID) value;
      getListener().changed(objectID, null, putLockField);
      includeIDs.addBackReference(putLockField, objectID);
    } else if (CAPACITY_FIELD_NAME.equals(fieldName)) {
      capacityField = value;
    }
  }

  public void applyMethod(ObjectID objectID, ApplyTransactionInfo includeIDs, int method, Object[] params) {
    switch (method) {
      case SerializationUtil.PUT:
        addChangeToCollector(objectID, params[0], includeIDs);
        references.add(params[0]);
        break;
      case SerializationUtil.TAKE:
        references.remove(0);
        break;
      case SerializationUtil.CLEAR:
        references.clear();
        break;
      case SerializationUtil.REMOVE_FIRST_N:
        int n = ((Integer) params[0]).intValue();
        for (int i = 0; i < n; i++) {
          references.remove(0);
        }
        break;
      case SerializationUtil.REMOVE_AT:
        int i = ((Integer) params[0]).intValue();
        Assert.assertTrue(references.size() > i);
        references.remove(i);
        break;
      default:
        throw new AssertionError("Invalid method:" + method + " state:" + this);
    }
  }

  // Since LinkedBlockingQueue supports partial collection, we are not adding it to back references
  private void addChangeToCollector(ObjectID objectID, Object newValue, ApplyTransactionInfo includeIDs) {
    if (newValue instanceof ObjectID) {
      getListener().changed(objectID, null, (ObjectID) newValue);
    }
  }

  public void addObjectReferencesTo(ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  protected void addAllObjectReferencesTo(Set refs) {
    addAllObjectReferencesFromIteratorTo(references.iterator(), refs);
    if (takeLockField != null) {
      refs.add(takeLockField);
    }
    if (putLockField != null) {
      refs.add(putLockField);
    }
  }

  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    dehydrateFields(objectID, writer);
    dehydrateMembers(objectID, writer);
  }

  private void dehydrateFields(ObjectID objectId, DNAWriter writer) {
    writer.addPhysicalAction(TAKE_LOCK_FIELD_NAME, takeLockField);
    writer.addPhysicalAction(PUT_LOCK_FIELD_NAME, putLockField);
    writer.addPhysicalAction(CAPACITY_FIELD_NAME, capacityField);
  }

  private void dehydrateMembers(ObjectID objectID, DNAWriter writer) {
    for (Iterator i = references.iterator(); i.hasNext();) {
      Object o = i.next();
      writer.addLogicalAction(SerializationUtil.PUT, new Object[] { o });
    }
  }

  public String toString() {
    return "QueueManagedStateObject(" + references + ")";
  }

  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    final int size = references.size();

    if (limit < 0) {
      limit = size;
    } else {
      limit = Math.min(limit, size);
    }

    Object[] data = new Object[limit];

    int index = 0;
    for (Iterator iter = references.iterator(); iter.hasNext() && index < limit; index++) {
      data[index] = iter.next();
    }

    return LogicalManagedObjectFacade.createListInstance(objectID, className, data, size);
  }

  public byte getType() {
    return QUEUE_TYPE;
  }

  private void writeField(ObjectOutput out, String fieldName, Object fieldValue) throws IOException {
    out.writeUTF(fieldName);
    if (fieldValue == null) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      if (fieldValue instanceof ObjectID) {
        out.writeLong(((ObjectID) fieldValue).toLong());
      } else {
        out.writeObject(fieldValue);
      }
    }
  }

  protected void basicWriteTo(ObjectOutput out) throws IOException {
    writeField(out, TAKE_LOCK_FIELD_NAME, takeLockField);
    writeField(out, PUT_LOCK_FIELD_NAME, putLockField);
    writeField(out, CAPACITY_FIELD_NAME, capacityField);

    out.writeInt(references.size());
    for (Iterator i = references.iterator(); i.hasNext();) {
      out.writeObject(i.next());
    }
  }

  protected boolean basicEquals(LogicalManagedObjectState o) {
    QueueManagedObjectState mo = (QueueManagedObjectState) o;
    return ((takeLockField == mo.takeLockField) || (takeLockField != null && takeLockField.equals(mo.takeLockField)))
           && ((putLockField == mo.putLockField) || (putLockField != null && putLockField.equals(mo.putLockField)))
           && ((capacityField == mo.capacityField) || (capacityField != null && capacityField.equals(mo.capacityField)))
           && references.equals(mo.references);
  }

  private static void readField(ObjectInput in, QueueManagedObjectState mo) throws ClassNotFoundException, IOException {
    String fieldName = in.readUTF();
    boolean fieldExist = in.readBoolean();
    if (fieldExist) {
      if (fieldName.equals(TAKE_LOCK_FIELD_NAME)) {
        mo.takeLockField = new ObjectID(in.readLong());
      } else if (fieldName.equals(PUT_LOCK_FIELD_NAME)) {
        mo.putLockField = new ObjectID(in.readLong());
      } else if (fieldName.equals(CAPACITY_FIELD_NAME)) {
        mo.capacityField = in.readObject();
      } else {
        throw new AssertionError("Field not recognized in QueueManagedObjectState.readFrom().");
      }
    }
  }

  static QueueManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    QueueManagedObjectState mo = new QueueManagedObjectState(in);
    readField(in, mo);
    readField(in, mo);
    readField(in, mo);
    int size = in.readInt();
    LinkedList list = new LinkedList();
    for (int i = 0; i < size; i++) {
      list.add(in.readObject());
    }
    mo.references = list;
    return mo;
  }
}
