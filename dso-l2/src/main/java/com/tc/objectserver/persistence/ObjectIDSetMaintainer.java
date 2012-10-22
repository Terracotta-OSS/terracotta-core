package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorageMutationListener;
import org.terracotta.corestorage.Retriever;

import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

/**
 * @author tim
 */
public class ObjectIDSetMaintainer implements KeyValueStorageMutationListener<Long, byte[]> {

  private final ObjectIDSet extantObjectIDSet = new ObjectIDSet();
  private final ObjectIDSet evictableObjectIDSet = new ObjectIDSet();
  private final ObjectIDSet mapObjectIDSet = new ObjectIDSet();

  enum ObjectMetadataEnum {
    TYPE
  }

  public synchronized ObjectIDSet objectIDSnapshot() {
    return new ObjectIDSet(extantObjectIDSet);
  }

  public synchronized ObjectIDSet evictableObjectIDSetSnapshot() {
    return new ObjectIDSet(evictableObjectIDSet);
  }

  public synchronized ObjectIDSet mapObjectIDSetSnapshot() {
    return new ObjectIDSet(mapObjectIDSet);
  }

  @Override
  public synchronized void added(Retriever<? extends Long> key, Retriever<? extends byte[]> value, byte metadata) {
    ObjectID k = new ObjectID(key.retrieve());
    if (PersistentCollectionsUtil.isEvictableMapType(metadata)) {
      evictableObjectIDSet.add(k);
    }
    if (PersistentCollectionsUtil.isPersistableCollectionType(metadata)) {
      mapObjectIDSet.add(k);
    }
    extantObjectIDSet.add(k);
  }

  @Override
  public synchronized void removed(Retriever<? extends Long> key, Retriever<? extends byte[]> value) {
    ObjectID k = new ObjectID(key.retrieve());
    evictableObjectIDSet.remove(k);
    mapObjectIDSet.remove(k);
    extantObjectIDSet.remove(k);
  }

}
