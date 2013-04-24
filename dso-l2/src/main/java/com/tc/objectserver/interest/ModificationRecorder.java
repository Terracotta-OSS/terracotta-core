/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.tc.object.ObjectID;
import com.tc.objectserver.impl.SamplingType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Shelestovich
 */
public final class ModificationRecorder {

  private final List<Modification> modifications = new ArrayList<Modification>();
  private final Map<ObjectID, byte[]> oidToValueMap = new HashMap<ObjectID, byte[]>();

  public void recordOperation(final ModificationType type, final Object key, final ObjectID objectId,
                              final String cacheName) {
    final Modification modification = new Modification(type, key, objectId, cacheName);
    modifications.add(modification);
  }

  public void recordOperationValue(final ObjectID objectId, final byte[] value) {
    oidToValueMap.put(objectId, value);
  }

  /**
   * Transforms removals into evictions or expirations.
   */
  public void reconsiderRemovals(final SamplingType samplingType) {
    for (Modification modification : modifications) {
      if (modification.getType() == ModificationType.REMOVE) {
        switch (samplingType) {
          case FOR_EVICTION:
            modification.setType(ModificationType.EVICT);
            break;
          case FOR_EXPIRATION:
            modification.setType(ModificationType.EXPIRE);
            break;
        }
      }
    }
  }

  public void markExpiration() {
    for (Modification modification : modifications) {
      if (modification.getType() == ModificationType.REMOVE) {
        modification.setType(ModificationType.EVICT);
      }
    }
  }

  public List<Modification> getModifications() {
    for (final Modification modification : modifications) {
      final byte[] value = oidToValueMap.get(modification.getObjectId());
      if (value != null) {
        modification.setValue(value);
      }
    }
    return modifications;
  }

}
