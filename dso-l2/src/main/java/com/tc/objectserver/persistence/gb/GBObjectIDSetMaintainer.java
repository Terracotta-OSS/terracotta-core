package com.tc.objectserver.persistence.gb;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.util.ObjectIDSet;

import java.util.Map;
import org.terracotta.corestorage.KeyValueStorageMutationListener;
import org.terracotta.corestorage.Retriever;

/**
 * @author tim
 */
public class GBObjectIDSetMaintainer implements KeyValueStorageMutationListener<Long, byte[]> {

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
  public synchronized void added(Retriever<? extends Long> key, Retriever<? extends byte[]> value, Map<? extends Enum, Object> metadata) {
    byte[] array = value.retrieve();
    byte type  = array[8];
    ObjectID k = new ObjectID(key.retrieve());
    if (PersistentCollectionsUtil.isEvictableMapType(type)) {
      evictableObjectIDSet.add(k);
    }
    if (PersistentCollectionsUtil.isPersistableCollectionType(type)) {
      mapObjectIDSet.add(k);
    }
    extantObjectIDSet.add(k);
  }

  @Override
  public synchronized void removed(Retriever<? extends Long> key, Retriever<? extends byte[]> value, Map<? extends Enum, Object> metadata) {
    byte[] array = value.retrieve();
    byte type  = array[8];
    ObjectID k = new ObjectID(key.retrieve());
    evictableObjectIDSet.remove(k);
    mapObjectIDSet.remove(k);
    extantObjectIDSet.remove(k);
  }

}
