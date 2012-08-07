/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.PhysicalManagedObjectFacade;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

public class ClusteredNotifierManagedObjectState extends AbstractManagedObjectState {
  private final long classID;

  public ClusteredNotifierManagedObjectState(final long classID) {
    this.classID = classID;
  }

  @Override
  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo applyInfo) throws IOException {
    while (cursor.next()) {
      final Object action = cursor.getAction();
      if (action instanceof LogicalAction) {
        // do nothing
      }
    }
  }

  @Override
  public Set getObjectReferences() {
    return Collections.EMPTY_SET;
  }

  @Override
  public void addObjectReferencesTo(ManagedObjectTraverser traverser) {
    traverser.addReachableObjectIDs(getObjectReferences());
  }

  @Override
  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type) {
    //
  }

  @Override
  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit) {
    return new PhysicalManagedObjectFacade(objectID, null, className, Collections.EMPTY_MAP, false,
                                           DNA.NULL_ARRAY_SIZE, false);
  }

  @Override
  public byte getType() {
    return ManagedObjectStateStaticConfig.CLUSTERED_NOTIFIER.getStateObjectType();
  }

  @Override
  public String getClassName() {
    return getStateFactory().getClassName(this.classID);
  }

  @Override
  public void writeTo(ObjectOutput out) throws IOException {
    out.writeLong(this.classID);
  }

  @Override
  protected boolean basicEquals(AbstractManagedObjectState o) {
    if (o instanceof ClusteredNotifierManagedObjectState) {
      return true;
    } else {
      return false;
    }
  }

  public static ClusteredNotifierManagedObjectState readFrom(ObjectInput in) throws IOException {
    final ClusteredNotifierManagedObjectState state = new ClusteredNotifierManagedObjectState(in.readLong());
    return state;
  }

}
