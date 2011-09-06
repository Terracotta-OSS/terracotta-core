/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

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

  public LogicalManagedObjectState(final long classID) {
    this.classID = classID;
  }

  protected LogicalManagedObjectState(final ObjectInput in) throws IOException {
    this.classID = in.readLong();
  }

  protected abstract void addAllObjectReferencesTo(Set refs);

  protected final void addAllObjectReferencesFromIteratorTo(final Iterator i, final Set refs) {
    for (; i.hasNext();) {
      final Object o = i.next();
      if (o instanceof ObjectID) {
        refs.add(o);
      }
    }
  }

  public final Set getObjectReferences() {
    final ObjectIDSet refs = new ObjectIDSet();
    addAllObjectReferencesTo(refs);
    return refs;
  }

  protected Set getObjectReferencesFrom(final Collection refs) {
    if (refs == null || refs.size() == 0) { return Collections.EMPTY_SET; }
    final Set results = new HashSet(refs.size());
    for (final Iterator i = refs.iterator(); i.hasNext();) {
      final Object o = i.next();
      if (o instanceof ObjectID) {
        results.add(o);
      }
    }
    return results;
  }

  // XXX:: This default behavior needs to be overridden by class that needs specific behavior (like
  // MapManagedObjectState)
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addRequiredObjectIDs(getObjectReferences());
  }

  public final void writeTo(final ObjectOutput out) throws IOException {
    out.writeLong(this.classID);
    basicWriteTo(out);
  }

  protected abstract void basicWriteTo(ObjectOutput out) throws IOException;

  public final String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  public final String getLoaderDescription() {
    return getStateFactory().getLoaderDescription(this.classID);
  }

  @Override
  protected final boolean basicEquals(final AbstractManagedObjectState o) {
    final LogicalManagedObjectState lmo = ((LogicalManagedObjectState) o);
    return lmo.classID == this.classID && basicEquals(lmo);
  }

  protected abstract boolean basicEquals(LogicalManagedObjectState o);
}
