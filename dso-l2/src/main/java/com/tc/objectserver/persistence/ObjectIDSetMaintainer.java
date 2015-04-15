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
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorageMutationListener;
import org.terracotta.corestorage.Retriever;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ExpandingBitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Set;

/**
 * @author tim
 */
public class ObjectIDSetMaintainer implements KeyValueStorageMutationListener<Long, byte[]> {
  private static enum ObjectIDSetType {
    BITSET_BASED_SET, EXPANDING_BITSET_BASED_SET
  }

  private static final TCLogger logger = TCLogging.getLogger(ObjectIDSetMaintainer.class);

  private final ObjectIDSet evictableObjectIDSet = new BitSetObjectIDSet();
  private final ObjectIDSet noReferencesObjectIDSet;
  private final ObjectIDSet referencesObjectIDSet = new BitSetObjectIDSet();

  public ObjectIDSetMaintainer() {
    noReferencesObjectIDSet = create(TCCollections.EMPTY_OBJECT_ID_SET);
    logger.info("Using ObjectIDSetType " + getObjectIDSetType());
  }

  public synchronized ObjectIDSet objectIDSnapshot() {
    ObjectIDSet oids = create(noReferencesObjectIDSet);
    oids.addAll(referencesObjectIDSet);
    return oids;
  }

  private static ObjectIDSetType getObjectIDSetType() {
    String type = TCPropertiesImpl.getProperties().getProperty(TCPropertiesConsts.L2_OBJECTMANAGER_OIDSET_TYPE, true);
    if (type == null) {
      return ObjectIDSetType.EXPANDING_BITSET_BASED_SET;
    } else {
      return ObjectIDSetType.valueOf(type);
    }
  }

  private static ObjectIDSet create(Set<ObjectID> clone) {
    switch (getObjectIDSetType()) {
      case BITSET_BASED_SET:
        return new BitSetObjectIDSet(clone);
      case EXPANDING_BITSET_BASED_SET:
        return new ExpandingBitSetObjectIDSet(clone);
    }
    throw new UnsupportedOperationException("Unsupported ObjectIDSet type " + getObjectIDSetType());
  }

  public synchronized ObjectIDSet evictableObjectIDSetSnapshot() {
    return new BitSetObjectIDSet(evictableObjectIDSet);
  }

  public synchronized boolean hasNoReferences(ObjectID id) {
    return noReferencesObjectIDSet.contains(id);
  }

  @Override
  public synchronized void added(Retriever<? extends Long> key, Retriever<? extends byte[]> value, byte metadata) {
    ObjectID k = new ObjectID(key.retrieve());
    if (PersistentCollectionsUtil.isEvictableMapType(metadata)) {
      evictableObjectIDSet.add(k);
    }
    if (PersistentCollectionsUtil.isNoReferenceObjectType(metadata)) {
      noReferencesObjectIDSet.add(k);
    } else {
      referencesObjectIDSet.add(k);
    }
  }

  @Override
  public synchronized void removed(Retriever<? extends Long> key) {
    ObjectID oid = new ObjectID(key.retrieve());
    evictableObjectIDSet.remove(oid);
    if (!noReferencesObjectIDSet.remove(oid)) {
      referencesObjectIDSet.remove(oid);
    }
  }
}
