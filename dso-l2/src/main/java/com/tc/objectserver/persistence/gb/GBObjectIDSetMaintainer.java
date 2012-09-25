package com.tc.objectserver.persistence.gb;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.gbapi.GBMapMutationListener;
import com.tc.gbapi.GBRetriever;
import com.tc.util.ObjectIDSet;

import java.util.Map;

/**
 * @author tim
 */
public class GBObjectIDSetMaintainer implements GBMapMutationListener<ObjectID, ManagedObject> {

  private final ObjectIDSet extantObjectIDSet = new ObjectIDSet();
  private final ObjectIDSet evictableObjectIDSet = new ObjectIDSet();
  private final ObjectIDSet mapObjectIDSet = new ObjectIDSet();

  enum ObjectMetadataEnum {
    TYPE
  }

  public ObjectIDSet objectIDSnapshot() {
    return new ObjectIDSet(extantObjectIDSet);
  }

  public ObjectIDSet evictableObjectIDSetSnapshot() {
    return new ObjectIDSet(evictableObjectIDSet);
  }

  public ObjectIDSet mapObjectIDSetSnapshot() {
    return new ObjectIDSet(mapObjectIDSet);
  }

  @Override
  public void added(GBRetriever<ObjectID> key, GBRetriever<ManagedObject> value, Map<? extends Enum, Object> metadata) {
    byte type = (Byte) metadata.get(ObjectMetadataEnum.TYPE);
    ObjectID k = key.retrieve();
    if (PersistentCollectionsUtil.isEvictableMapType(type)) {
      evictableObjectIDSet.add(k);
    }
    if (PersistentCollectionsUtil.isPersistableCollectionType(type)) {
      mapObjectIDSet.add(k);
    }
    extantObjectIDSet.add(k);
  }

  @Override
  public void removed(GBRetriever<ObjectID> key, GBRetriever<ManagedObject> value, Map<? extends Enum, Object> metadata) {
    byte type = (Byte) metadata.get(ObjectMetadataEnum.TYPE);
    ObjectID k = key.retrieve();
    if (PersistentCollectionsUtil.isEvictableMapType(type)) {
      evictableObjectIDSet.remove(k);
    }
    if (PersistentCollectionsUtil.isPersistableCollectionType(type)) {
      mapObjectIDSet.remove(k);
    }
    extantObjectIDSet.remove(k);
  }

}
