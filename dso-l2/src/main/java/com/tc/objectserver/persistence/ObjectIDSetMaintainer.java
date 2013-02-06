package com.tc.objectserver.persistence;

import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;
import org.terracotta.corestorage.KeyValueStorageMutationListener;
import org.terracotta.corestorage.Retriever;

/**
 * @author tim
 */
public class ObjectIDSetMaintainer implements KeyValueStorageMutationListener<Long, byte[]> {

  private final ObjectIDSet evictableObjectIDSet = new ObjectIDSet();
  private final ObjectIDSet noReferencesObjectIDSet = new ObjectIDSet();
  private final ObjectIDSet referencesObjectIDSet = new ObjectIDSet();

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
