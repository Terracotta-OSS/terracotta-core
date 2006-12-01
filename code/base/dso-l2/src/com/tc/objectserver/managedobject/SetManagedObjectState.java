/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ManagedObjectState for sets.
 */
public class SetManagedObjectState extends LogicalManagedObjectState {
  protected Set references;

  SetManagedObjectState(long classID) {
    super(classID);
    this.references = new LinkedHashSet(1, 0.75f); // Accommodating LinkedHashSet.
  }

  protected SetManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  public void apply(ObjectID objectID, DNACursor cursor, BackReferences includeIDs) throws IOException {
    while (cursor.next()) {
      LogicalAction action = cursor.getLogicalAction();
      int method = action.getMethod();
      Object[] params = action.getParameters();
      apply(objectID, method, params, includeIDs);
    }
  }

  protected void apply(ObjectID objectID, int method, Object[] params, BackReferences includeIDs) {
    switch (method) {
      case SerializationUtil.ADD:
        Object v = getValue(params);
        addChangeToCollector(objectID, v, includeIDs);
        references.add(v);
        break;
      case SerializationUtil.REMOVE:
        references.remove(params[0]);
        break;
      case SerializationUtil.REMOVE_ALL:
        references.removeAll(Arrays.asList(params));
        break;
      case SerializationUtil.CLEAR:
        references.clear();
        break;
      default:
        throw new AssertionError("Invalid action:" + method);
    }
  }

  private Object getValue(Object[] params) {
    // hack for trove sets which replace the old set value (java ones do the opposite) clean this up
    return params.length == 2 ? params[1] : params[0];
  }

  private void addChangeToCollector(ObjectID objectID, Object newValue, BackReferences includeIDs) {
    if (newValue instanceof ObjectID) {
      getListener().changed(objectID, null, (ObjectID) newValue);
      includeIDs.addBackReference((ObjectID) newValue, objectID);
    }
  }

  public void dehydrate(ObjectID objectID, DNAWriter writer) {
    for (Iterator i = references.iterator(); i.hasNext();) {
      Object value = i.next();
      writer.addLogicalAction(SerializationUtil.ADD, new Object[] { value });
    }
  }

  protected Collection getAllReferences() {
    return Collections.unmodifiableSet(this.references);
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

    return LogicalManagedObjectFacade.createSetInstance(objectID, className, data, size);
  }

  public byte getType() {
    return SET_TYPE;
  }

  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeInt(references.size());
    for (Iterator i = references.iterator(); i.hasNext();) {
      out.writeObject(i.next());
    }
  }

  protected boolean basicEquals(LogicalManagedObjectState o) {
    SetManagedObjectState mo = (SetManagedObjectState) o;
    return references.equals(mo.references);
  }

  static SetManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    SetManagedObjectState setmo = new SetManagedObjectState(in);
    int size = in.readInt();
    Set set = new LinkedHashSet(size, 0.75f);
    for (int i = 0; i < size; i++) {
      set.add(in.readObject());
    }
    setmo.references = set;
    return setmo;
  }
}
