/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * ManagedObjectState for sets.
 */
public class TreeSetManagedObjectState extends SetManagedObjectState {
  private static final String COMPARATOR_FIELDNAME = TreeMapManagedObjectState.COMPARATOR_FIELDNAME;

  private ObjectID            comparator           = null;

  public TreeSetManagedObjectState(long classID, Set set) {
    super(classID, set);
  }

  protected TreeSetManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo includeIDs) throws IOException {
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

  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    if (comparator != null) {
      writer.addPhysicalAction(COMPARATOR_FIELDNAME, comparator);
    }
    super.dehydrate(objectID, writer, type);
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
  }

  protected boolean basicEquals(LogicalManagedObjectState other) {
    TreeSetManagedObjectState mo = (TreeSetManagedObjectState) other;
        
    return (comparator == mo.comparator || (comparator != null && comparator.equals(mo.comparator)))
           && references.equals(mo.references);
  }

  static TreeSetManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    if (false) {
      // to remove the warning
      throw new ClassNotFoundException();
    }
    TreeSetManagedObjectState tsm = new TreeSetManagedObjectState(in);
    ObjectID comparator;
    if (in.readBoolean()) {
      comparator = new ObjectID(in.readLong());
    } else {
      comparator = null;
    }
    tsm.comparator = comparator;
    return tsm;
  }

  public byte getType() {
    return TREE_SET_TYPE;
  }
}
