/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.LogicalOperation;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.Events;
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

  @Override
  public void apply(final ObjectID objectID, final DNACursor cursor, final ApplyTransactionInfo applyInfo)
      throws IOException {
    int eventCount = 0;
    while (cursor.next()) {
      final Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        final PhysicalAction physicalAction = (PhysicalAction)action;
        applyPhysicalAction(physicalAction, objectID, applyInfo);
      } else { // LogicalAction
        // notify subscribers about the mutation operation
        eventCount++;

        final LogicalAction logicalAction = (LogicalAction)action;
        final LogicalOperation operation = logicalAction.getLogicalOperation();
        final Object[] params = logicalAction.getParameters();
        LogicalChangeResult result = applyLogicalAction(objectID, applyInfo, operation, params);
        applyInfo.getApplyResultRecorder().recordResult(logicalAction.getLogicalChangeID(), result);
      }
    }
    if (eventCount != 0) {
      getOperationEventBus().post(Events.writeOperationCountChangeEvent(applyInfo.getServerTransactionID().getSourceID(), eventCount));
    }
  }

  protected void applyPhysicalAction(final PhysicalAction action, final ObjectID objectID, final ApplyTransactionInfo info) {
    // to be optionally implemented by subclasses
  }

  protected abstract LogicalChangeResult applyLogicalAction(final ObjectID objectID,
                                                            final ApplyTransactionInfo applyInfo,
                                               final LogicalOperation method,
                                             final Object[] params);

  protected abstract void addAllObjectReferencesTo(Set refs);

  protected final void addAllObjectReferencesFromIteratorTo(final Iterator i, final Set refs) {
    for (; i.hasNext(); ) {
      final Object o = i.next();
      if (o instanceof ObjectID) {
        refs.add(o);
      }
    }
  }

  @Override
  public final Set getObjectReferences() {
    final ObjectIDSet refs = new BitSetObjectIDSet();
    addAllObjectReferencesTo(refs);
    return refs;
  }

  protected Set getObjectReferencesFrom(final Collection refs) {
    if (refs == null || refs.size() == 0) { return Collections.EMPTY_SET; }
    final Set results = new HashSet(refs.size());
    for (final Iterator i = refs.iterator(); i.hasNext(); ) {
      final Object o = i.next();
      if (o instanceof ObjectID) {
        results.add(o);
      }
    }
    return results;
  }

  // XXX:: This default behavior needs to be overridden by class that needs specific behavior (like
  // MapManagedObjectState)
  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addRequiredObjectIDs(getObjectReferences());
  }

  @Override
  public final void writeTo(final ObjectOutput out) throws IOException {
    out.writeLong(this.classID);
    basicWriteTo(out);
  }

  protected abstract void basicWriteTo(ObjectOutput out) throws IOException;

  @Override
  public final String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  @Override
  protected final boolean basicEquals(final AbstractManagedObjectState o) {
    final LogicalManagedObjectState lmo = ((LogicalManagedObjectState)o);
    return lmo.classID == this.classID && basicEquals(lmo);
  }

  protected abstract boolean basicEquals(LogicalManagedObjectState o);
}
