/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ManagedObjectState for sets.
 */
public class LinkedHashSetManagedObjectState extends SetManagedObjectState {
  LinkedHashSetManagedObjectState(long classID) {
    super(classID, new LinkedHashSet(1, 0.75f));
  }

  protected LinkedHashSetManagedObjectState(ObjectInput in) throws IOException {
    super(in);
  }

  public byte getType() {
    return LINKED_HASHSET_TYPE;
  }

  static LinkedHashSetManagedObjectState readFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    LinkedHashSetManagedObjectState setmo = new LinkedHashSetManagedObjectState(in);
    int size = in.readInt();
    Set set = new LinkedHashSet(size, 0.75f);
    for (int i = 0; i < size; i++) {
      set.add(in.readObject());
    }
    setmo.setSet(set);
    return setmo;
  }

  @Override
  protected void basicWriteTo(ObjectOutput out) throws IOException {
    out.writeInt(references.size());
    for (Iterator i = references.iterator(); i.hasNext();) {
      out.writeObject(i.next());
    }
  }
}
