/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.persistence.db.PersistableCollection;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * ManagedObjectState for sets.
 */
public class SetManagedObjectState extends LogicalManagedObjectState implements PersistableObjectState {
  protected Set references;

  SetManagedObjectState(long classID, Set set) {
    super(classID);
    this.references = set;
  }

  protected SetManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo includeIDs) throws IOException {
    while (cursor.next()) {
      LogicalAction action = cursor.getLogicalAction();
      int method = action.getMethod();
      Object[] params = action.getParameters();
      apply(objectID, method, params, includeIDs);
    }
  }

  protected void apply(ObjectID objectID, int method, Object[] params, ApplyTransactionInfo includeIDs) {
    switch (method) {
      case SerializationUtil.ADD:
        Object v = params[0];
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

  private void addChangeToCollector(ObjectID objectID, Object newValue, ApplyTransactionInfo includeIDs) {
    if (newValue instanceof ObjectID) {
      getListener().changed(objectID, null, (ObjectID) newValue);
      includeIDs.addBackReference((ObjectID) newValue, objectID);
    }
  }

  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    for (Iterator i = references.iterator(); i.hasNext();) {
      Object value = i.next();
      writer.addLogicalAction(SerializationUtil.ADD, new Object[] { value });
    }
  }

  @Override
  protected void addAllObjectReferencesTo(Set refs) {
    addAllObjectReferencesFromIteratorTo(this.references.iterator(), refs);
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

  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    // for removing warning
    if (false) throw new IOException();
  }

  @Override
  protected boolean basicEquals(LogicalManagedObjectState o) {
    SetManagedObjectState mo = (SetManagedObjectState) o;
    return references.equals(mo.references);
  }

  public void setSet(Set set) {
    if (this.references != null) { throw new AssertionError("The references map is already set ! " + references); }
    this.references = set;
  }

  public PersistableCollection getPersistentCollection() {
    return (PersistableCollection) references;
  }

  public void setPersistentCollection(PersistableCollection collection) {
    if (this.references != null) { throw new AssertionError("The references map is already set ! " + references); }
    this.references = (Set) collection;
  }

  static SetManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    if (false) {
      // This is added to make the compiler happy. For some reason if I have readFrom() method throw
      // ClassNotFoundException in LinkedHashMapManagedObjectState, it shows as an error !!
      throw new ClassNotFoundException();
    }
    return new SetManagedObjectState(in);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((references == null) ? 0 : references.hashCode());
    return result;
  }
}
