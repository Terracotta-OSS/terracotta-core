/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class LogicalManagedObjectState extends AbstractManagedObjectState {

  private final long classID;

  public LogicalManagedObjectState(long classID) {
    this.classID = classID;
  }

  protected LogicalManagedObjectState(ObjectInput in) throws IOException {
    this.classID = in.readLong();
  }

  protected abstract Collection getAllReferences();

  public final Set getObjectReferences() {
    // XXX: this is inefficient. I'm going to have to fix that

    return getObjectReferencesFrom(getAllReferences());
  }
  
  protected Set getObjectReferencesFrom(Collection refs) {
    if (refs == null || refs.size() == 0) { return Collections.EMPTY_SET; }
    Set results = new HashSet(refs.size());
    for (Iterator i = refs.iterator(); i.hasNext();) {
      Object o = i.next();
      if (o instanceof ObjectID) {
        results.add(o);
      }
    }
    return results;
  }

  
  // XXX:: This default behavior needs to be overridden by class that needs specific behavior (like MapManagedObjectState)
  public void addObjectReferencesTo(ManagedObjectTraverser traverser) {
    traverser.addRequiredObjectIDs(getObjectReferences());
  }

  public final void writeTo(ObjectOutput out) throws IOException {
    out.writeLong(classID);
    basicWriteTo(out);
  }

  protected abstract void basicWriteTo(ObjectOutput out) throws IOException;
  
  public final String getClassName() {
    return getStateFactory().getClassName(classID);
  }

  public final String getLoaderDescription() {
    return getStateFactory().getLoaderDescription(classID);
  }

  protected final boolean basicEquals(AbstractManagedObjectState o) {
    LogicalManagedObjectState lmo = ((LogicalManagedObjectState) o);
    return lmo.classID == classID && basicEquals(lmo);
  }

  protected abstract boolean basicEquals(LogicalManagedObjectState o);
}
