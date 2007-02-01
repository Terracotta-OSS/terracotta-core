/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * state for tree maps
 */
public class TreeMapManagedObjectState extends MapManagedObjectState implements PrettyPrintable {
  static final String COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";
  private ObjectID    comparator           = null;

  TreeMapManagedObjectState(long classID) {
    super(classID, new HashMap());
  }

  protected TreeMapManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    while (cursor.next()) {
      Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        PhysicalAction pa = (PhysicalAction) action;
        Assert.assertEquals(COMPARATOR_FIELDNAME, pa.getFieldName());
        this.comparator = (ObjectID) pa.getObject();
      } else {
        LogicalAction la = (LogicalAction) action;
        int method = la.getMethod();
        Object[] params = la.getParameters();
        applyMethod(objectID, includeIDs, method, params);
      }
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
    out.writeInt(references.size());
    for (Iterator i = references.entrySet().iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      out.writeObject(entry.getKey());
      out.writeObject(entry.getValue());
    }
  }

  protected boolean basicEquals(LogicalManagedObjectState o) {
    TreeMapManagedObjectState mo = (TreeMapManagedObjectState) o;
    return (comparator == mo.comparator || (comparator != null && comparator.equals(mo.comparator)))
           && super.basicEquals(o);
  }

  static MapManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    TreeMapManagedObjectState tm = new TreeMapManagedObjectState(in);
    ObjectID comparator;
    if (in.readBoolean()) {
      comparator = new ObjectID(in.readLong());
    } else {
      comparator = null;
    }
    int size = in.readInt();
    Map map = new HashMap(size);
    for (int i = 0; i < size; i++) {
      map.put(in.readObject(), in.readObject());
    }
    tm.references = map;
    tm.comparator = comparator;
    
    return tm;
  }
}
