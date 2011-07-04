/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.Set;

/**
 * state for tree maps
 */
public class TreeMapManagedObjectState extends MapManagedObjectState implements PrettyPrintable {
  static final String COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";
  private ObjectID    comparator           = null;

  private TreeMapManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  public TreeMapManagedObjectState(long classID, Map map) {
    super(classID, map);
  }

  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo includeIDs) throws IOException {
    while (cursor.next()) {
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
        applyMethod(objectID, includeIDs, method, params);
      }
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

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out = out.println("TreeMapManagedObjectState").duplicateAndIndent();
    out.indent().println("references: " + references);
    out.indent().println("comparator: " + comparator);
    return rv;
  }

  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    // XXX: This facade is incomplete...it doesn't include the comparator field
    return super.createFacade(objectID,className, limit);
  }

  public byte getType() {
    return TREE_MAP_TYPE;
  }

  protected void basicWriteTo(ObjectOutput out) throws IOException {
    if (comparator == null) {
      out.writeBoolean(false);
    } else {
      out.writeBoolean(true);
      out.writeLong(comparator.toLong());
    }
  }

  protected boolean basicEquals(LogicalManagedObjectState o) {
    TreeMapManagedObjectState mo = (TreeMapManagedObjectState) o;
    return (comparator == mo.comparator || (comparator != null && comparator.equals(mo.comparator)))
           && super.basicEquals(o);
  }

  static MapManagedObjectState readFrom(ObjectInput in) throws IOException {
    TreeMapManagedObjectState tm = new TreeMapManagedObjectState(in);
    if (in.readBoolean()) {
      tm.comparator = new ObjectID(in.readLong());
    }

    return tm;
  }
}
