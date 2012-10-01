/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.gbapi.GBMap;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.persistence.db.TCDestroyable;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Set;

/**
 * ManagedObjectState for sets.
 */
public class SetManagedObjectState extends LogicalManagedObjectState implements TCDestroyable {
  protected GBMap<Object, Object> references;

  SetManagedObjectState(long classID, GBMap<Object, Object> set) {
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
        references.put(v, true);
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
      case SerializationUtil.DESTROY:
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
    for (Object o : references.keySet()) {
      writer.addLogicalAction(SerializationUtil.ADD, new Object[] { o });
    }
  }

  @Override
  protected void addAllObjectReferencesTo(Set refs) {
    for (Object o : references.keySet()) {
      if (o instanceof ObjectID) {
        refs.add(o);
      }
    }
  }

  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    throw new UnsupportedOperationException();
//    final int size = references.size();
//
//    if (limit < 0) {
//      limit = size;
//    } else {
//      limit = Math.min(limit, size);
//    }
//
//    Object[] data = new Object[limit];
//
//    int index = 0;
//    for (Iterator iter = references.iterator(); iter.hasNext() && index < limit; index++) {
//      data[index] = iter.next();
//    }
//
//    return LogicalManagedObjectFacade.createSetInstance(objectID, className, data, size);
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

  public void destroy() {
    if (this.references instanceof TCDestroyable) {
      ((TCDestroyable) this.references).destroy();
    }
  }
}
