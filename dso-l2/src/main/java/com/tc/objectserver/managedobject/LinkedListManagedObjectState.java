/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.LogicalAction;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.LinkedList;

/**
 * Server representation of a linkedList
 */
public class LinkedListManagedObjectState extends ListManagedObjectState {
  
  LinkedListManagedObjectState(ObjectInput in) throws IOException {
    super(in);
    references = new LinkedList();
  }

  protected LinkedListManagedObjectState(long classID) {
    super(classID);
    references = new LinkedList();
  }
  

  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo includeIDs) throws IOException {
    while (cursor.next()) {
      LogicalAction action = cursor.getLogicalAction();
      int method = action.getMethod();
      Object[] params = action.getParameters();
      
      LinkedList linkedListReferences = getLinkedList();
      
      switch (method) {
        case SerializationUtil.ADD:
        case SerializationUtil.ADD_LAST:
          addChangeToCollector(objectID, params[0], includeIDs);
          linkedListReferences.addLast(params[0]);
          break;
        case SerializationUtil.ADD_FIRST:
          addChangeToCollector(objectID, params[0], includeIDs);
          linkedListReferences.addFirst(params[0]);
          break;
        case SerializationUtil.REMOVE_FIRST:
          if (linkedListReferences.size() > 0) {
            linkedListReferences.removeFirst();
          }
          break;
        case SerializationUtil.REMOVE_LAST:
          int size = linkedListReferences.size();
          if (size > 0) {
            linkedListReferences.removeLast();
          }
          break;
        default:
          super.applyOperation(method, objectID, includeIDs , params);
      }
    }
  }

  public String toString() {
    return "LinkedListManagedStateObject(" + references + ")";
  }

  public byte getType() {
    return LINKED_LIST_TYPE;
  }
  
  private LinkedList getLinkedList() {
    return (LinkedList)references;
  }

  static LinkedListManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    LinkedListManagedObjectState listmo = new LinkedListManagedObjectState(in);
    int size = in.readInt();
    LinkedList list = new LinkedList();
    for (int i = 0; i < size; i++) {
      list.add(in.readObject());
    }
    listmo.references = list;
    return listmo;
  }

}