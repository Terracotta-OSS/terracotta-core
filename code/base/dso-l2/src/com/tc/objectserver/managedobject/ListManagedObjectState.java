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
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.mgmt.LogicalManagedObjectFacade;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
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

  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo includeIDs) throws IOException {
    while (cursor.next()) {
      LogicalAction action = cursor.getLogicalAction();
      int method = action.getMethod();
      Object[] params = action.getParameters();
      applyOperation(method, objectID, includeIDs, params);
    }
  }

  protected void applyOperation(int method, ObjectID objectID, ApplyTransactionInfo includeIDs, Object[] params)
      throws AssertionError {
    switch (method) {
      case SerializationUtil.ADD:
      case SerializationUtil.ADD_LAST:
        addChangeToCollector(objectID, params[0], includeIDs);
        references.add(params[0]);
        break;
      case SerializationUtil.ADD_FIRST:
        addChangeToCollector(objectID, params[0], includeIDs);
        references.add(0, params[0]);
        break;
      case SerializationUtil.INSERT_AT:
      case SerializationUtil.ADD_AT:
        addChangeToCollector(objectID, params[1], includeIDs);
        int ai = Math.min(((Integer) params[0]).intValue(), references.size());
        if (references.size() < ai) {
          references.add(params[1]);
        } else {
          references.add(ai, params[1]);
        }
        break;
      case SerializationUtil.REMOVE:
        references.remove(params[0]);
        break;
      case SerializationUtil.REMOVE_ALL:
        references.removeAll(Arrays.asList(params));
        break;
      case SerializationUtil.REMOVE_AT:
        int index = ((Integer) params[0]).intValue();
        if (references.size() > index) {
          references.remove(index);
        }
        break;
      case SerializationUtil.REMOVE_RANGE: {
        int size = references.size();
        int fromIndex = ((Integer) params[0]).intValue();
        int toIndex = ((Integer) params[1]).intValue();
        int removeIndex = fromIndex;
        if (size > fromIndex && size >= toIndex) {
          while (fromIndex++ < toIndex) {
            references.remove(removeIndex);
          }
        }
      }
        break;
      case SerializationUtil.CLEAR:
        references.clear();
        break;
      case SerializationUtil.SET_ELEMENT:
      case SerializationUtil.SET:
        addChangeToCollector(objectID, params[1], includeIDs);
        int si = Math.min(((Integer) params[0]).intValue(), references.size());
        if (references.size() <= si) {
          references.add(params[1]);
        } else {
          references.set(si, params[1]);
        }
        break;
      case SerializationUtil.REMOVE_FIRST:
        if (references.size() > 0) {
          references.remove(0);
        }
        break;
      case SerializationUtil.REMOVE_LAST:
        int size = references.size();
        if (size > 0) {
          references.remove(size - 1);
        }
        break;
      case SerializationUtil.SET_SIZE:
        int setSize = ((Integer) params[0]).intValue();
        int listSize = references.size();

        if (listSize < setSize) {
          for (int i = listSize; i < setSize; i++) {
            references.add(ObjectID.NULL_ID);
          }
        } else if (listSize > setSize) {
          for (int i = listSize; i != setSize; i--) {
            references.remove(i - 1);
          }
        }
        break;
      case SerializationUtil.TRIM_TO_SIZE:
        // do nothing for now
        break;
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

  protected void addAllObjectReferencesTo(Set refs) {
    addAllObjectReferencesFromIteratorTo(references.iterator(), refs);
  }

  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    for (Iterator i = references.iterator(); i.hasNext();) {
      Object value = i.next();
      writer.addLogicalAction(SerializationUtil.ADD, new Object[] { value });
    }
  }

  public String toString() {
    return "ListManagedStateObject(" + references + ")";
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
    return LIST_TYPE;
  }

  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeInt(references.size());
    for (Iterator i = references.iterator(); i.hasNext();) {
      out.writeObject(i.next());
    }
  }

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

}