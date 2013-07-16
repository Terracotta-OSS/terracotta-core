package com.tc.objectserver.persistence;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import org.terracotta.corestorage.KeyValueStorageMutationListener;
import org.terracotta.corestorage.Retriever;

/**
 * @author tim
 */
public class ObjectIDSetMaintainer implements KeyValueStorageMutationListener<Long, byte[]> {
  private static final TCLogger logger = TCLogging.getLogger(ObjectIDSetMaintainer.class);

  private final ObjectIDSet evictableObjectIDSet = new ObjectIDSet();
  private final ObjectIDSet noReferencesObjectIDSet;
  private final ObjectIDSet referencesObjectIDSet = new ObjectIDSet();

  public ObjectIDSetMaintainer() {
    String type = TCPropertiesImpl.getProperties().getProperty(TCPropertiesConsts.L2_OBJECTMANAGER_OIDSET_TYPE, true);
    if (type == null) {
      noReferencesObjectIDSet = new ObjectIDSet(ObjectIDSet.ObjectIDSetType.BITSET_BASED_SET);
    } else {
      noReferencesObjectIDSet = new ObjectIDSet(ObjectIDSet.ObjectIDSetType.valueOf(type));
      logger.info("Using object id set of type " + type);
    }
  }

  public synchronized ObjectIDSet objectIDSnapshot() {
    ObjectIDSet oids = new ObjectIDSet(noReferencesObjectIDSet);
    oids.addAll(referencesObjectIDSet);
    return oids;
  }

  public synchronized ObjectIDSet evictableObjectIDSetSnapshot() {
    return new ObjectIDSet(evictableObjectIDSet);
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
  public synchronized void removed(Retriever<? extends Long> key, Retriever<? extends byte[]> value) {
    ObjectID oid = new ObjectID(key.retrieve());
    evictableObjectIDSet.remove(oid);
    if (!noReferencesObjectIDSet.remove(oid)) {
      referencesObjectIDSet.remove(oid);
    }
  }
}
