/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ManagedObjectState for sets.
 */
public class TreeSetManagedObjectState extends SetManagedObjectState {
  private static final String COMPARATOR_FIELDNAME = TreeMapManagedObjectState.COMPARATOR_FIELDNAME;

  private ObjectID            comparator           = null;

  public TreeSetManagedObjectState(long classID) {
    super(classID);
  }

  protected TreeSetManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    if (!cursor.next()) { return; }

    Object action = cursor.getAction();
    if (action instanceof PhysicalAction) {
      PhysicalAction pa = (PhysicalAction) action;
      Assert.assertEquals(COMPARATOR_FIELDNAME, pa.getFieldName());
      this.comparator = (ObjectID) pa.getObject();
      getListener().changed(objectID, null, comparator);
      includeIDs.addBackReference(comparator, objectID);
    } else {
      LogicalAction la = (LogicalAction) action;
      int method = la.getMethod();
      Object[] params = la.getParameters();
      super.apply(objectID, method, params, includeIDs);
    }

    while (cursor.next()) {
      LogicalAction la = cursor.getLogicalAction();
      int method = la.getMethod();
      Object[] params = la.getParameters();
      super.apply(objectID, method, params, includeIDs);
    }
  }

  public void dehydrate(ObjectID objectID, DNAWriter writer) {
    if (comparator != null) {
      writer.addPhysicalAction(COMPARATOR_FIELDNAME, comparator);
    }
    super.dehydrate(objectID, writer);
  }

  protected void addAllObjectReferencesTo(Set refs) {
    super.addAllObjectReferencesTo(refs);
    if (comparator != null) {
      refs.add(comparator);
    }
  }

  // TODO This Facade has not included the comparator.
  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    return super.createFacade(objectID, className, limit);
  }

  protected void basicWriteTo(ObjectOutput out) throws IOException {
    if (comparator == null) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      out.writeLong(comparator.toLong());
    }
    out.writeInt(references.size());
    for (Iterator i = references.iterator(); i.hasNext();) {
      out.writeObject(i.next());
    }
  }

  protected boolean basicEquals(Object other) {
    TreeSetManagedObjectState mo = (TreeSetManagedObjectState) other;
    return (comparator == mo.comparator || (comparator != null && comparator.equals(mo.comparator)))
           && references.equals(mo.references);
  }

  static SetManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    TreeSetManagedObjectState tsm = new TreeSetManagedObjectState(in);
    ObjectID comparator;
    if (in.readBoolean()) {
      comparator = new ObjectID(in.readLong());
    } else {
      comparator = null;
    }
    int size = in.readInt();
    Set set = new LinkedHashSet(size, 0.75f);
    for (int i = 0; i < size; i++) {
      set.add(in.readObject());
    }
    tsm.comparator = comparator;
    tsm.references = set;
    return tsm;
  }

  public byte getType() {
    return TREE_SET_TYPE;
  }
}
