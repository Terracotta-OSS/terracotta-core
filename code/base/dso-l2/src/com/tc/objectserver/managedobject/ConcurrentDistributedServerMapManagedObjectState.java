/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNA.DNAType;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Map;

public class ConcurrentDistributedServerMapManagedObjectState extends ConcurrentDistributedMapManagedObjectState {

  protected ConcurrentDistributedServerMapManagedObjectState(final ObjectInput in) throws IOException {
    super(in);
  }

  protected ConcurrentDistributedServerMapManagedObjectState(final long classId, final Map map) {
    super(classId, map);
  }

  @Override
  public byte getType() {
    return CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE;
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    // Nothing to add since nothing is required to be faulted in the L1
  }

  @Override
  public void dehydrate(final ObjectID objectID, final DNAWriter writer, final DNAType type) {
    if (type == DNAType.L2_SYNC) {
      // Write entire state info
      super.dehydrate(objectID, writer, type);
    } else if (type == DNAType.L1_FAULT) {
      // Don't fault the references
      dehydrateFields(objectID, writer);
    }
  }

  @Override
  public void apply(final ObjectID objectID, final DNACursor cursor, final BackReferences includeIDs)
      throws IOException {
    includeIDs.ignoreBroadcastFor(objectID);
    super.apply(objectID, cursor, includeIDs);
  }

  @Override
  protected void applyMethod(final ObjectID objectID, final BackReferences includeIDs, final int method,
                             final Object[] params) {
    super.applyMethod(objectID, includeIDs, method, params);
    if (method == SerializationUtil.CLEAR) {
      // clear needs to be broadcasted so local caches can be cleared elsewhere
      includeIDs.forceBroadcastFor(objectID);
    }
  }

  public Object getValueForKey(final Object portableKey) {
    return this.references.get(portableKey);
  }

  public Integer getSize() {
    return this.references.size();
  }

  static MapManagedObjectState readFrom(final ObjectInput in) throws IOException {
    final ConcurrentDistributedServerMapManagedObjectState cdmMos = new ConcurrentDistributedServerMapManagedObjectState(
                                                                                                                         in);
    return cdmMos;
  }

}
