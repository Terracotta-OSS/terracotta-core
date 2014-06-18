/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalChangeResult;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Server representation of a list
 */
public class ListManagedObjectState extends LogicalManagedObjectState {
  protected List references;

  ListManagedObjectState(ObjectInput in) throws IOException {
    super(in);
    references = new ArrayList(1);
  }

  protected ListManagedObjectState(long classID) {
    super(classID);
    references = new ArrayList(1);
  }

  @Override
  protected LogicalChangeResult applyLogicalAction(final ObjectID objectID, final ApplyTransactionInfo applyInfo,
                                                   final LogicalOperation method,
                                      final Object[] params) throws AssertionError {
    switch (method) {
      case ADD:
        addChangeToCollector(objectID, params[0], applyInfo);
        references.add(params[0]);
        return LogicalChangeResult.SUCCESS;
      case ADD_AT:
        addChangeToCollector(objectID, params[1], applyInfo);
        int ai = Math.min(((Integer) params[0]).intValue(), references.size());
        if (references.size() < ai) {
          references.add(params[1]);
        } else {
          references.add(ai, params[1]);
        }
        return LogicalChangeResult.SUCCESS;
      case REMOVE:
        references.remove(params[0]);
        return LogicalChangeResult.SUCCESS;
      case REMOVE_AT:
        int index = (Integer) params[0];
        if (references.size() > index) {
          references.remove(index);
        }
        return LogicalChangeResult.SUCCESS;
      case REMOVE_RANGE: {
        int size = references.size();
        int fromIndex = (Integer) params[0];
        int toIndex = (Integer) params[1];
        int removeIndex = fromIndex;
        if (size > fromIndex && size >= toIndex) {
          while (fromIndex++ < toIndex) {
            references.remove(removeIndex);
          }
        }
      }
        return LogicalChangeResult.SUCCESS;
      case CLEAR:
      case DESTROY:
        references.clear();
        return LogicalChangeResult.SUCCESS;
      case SET:
        addChangeToCollector(objectID, params[1], applyInfo);
        int si = Math.min(((Integer) params[0]).intValue(), references.size());
        if (references.size() <= si) {
          references.add(params[1]);
        } else {
          references.set(si, params[1]);
        }
        return LogicalChangeResult.SUCCESS;
      default:
        throw new AssertionError("Invalid method:" + method + " state:" + this);
    }
  }

  protected void addChangeToCollector(ObjectID objectID, Object newValue, ApplyTransactionInfo includeIDs) {
    if (newValue instanceof ObjectID) {
      getListener().changed(objectID, null, (ObjectID) newValue);
      includeIDs.addBackReference((ObjectID) newValue, objectID);
    }
  }

  @Override
  protected void addAllObjectReferencesTo(Set refs) {
    addAllObjectReferencesFromIteratorTo(references.iterator(), refs);
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    for (Iterator i = references.iterator(); i.hasNext();) {
      Object value = i.next();
      writer.addLogicalAction(LogicalOperation.ADD, new Object[] { value });
    }
  }

  @Override
  public String toString() {
    return "ListManagedStateObject(" + references + ")";
  }

  @Override
  public byte getType() {
    return LIST_TYPE;
  }

  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeInt(references.size());
    for (Iterator i = references.iterator(); i.hasNext();) {
      out.writeObject(i.next());
    }
  }

  @Override
  protected boolean basicEquals(LogicalManagedObjectState o) {
    ListManagedObjectState mo = (ListManagedObjectState) o;
    return references.equals(mo.references);
  }

  static ListManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    ListManagedObjectState listmo = new ListManagedObjectState(in);
    int size = in.readInt();
    ArrayList list = new ArrayList(size);
    for (int i = 0; i < size; i++) {
      list.add(in.readObject());
    }
    listmo.references = list;
    return listmo;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((references == null) ? 0 : references.hashCode());
    return result;
  }

}