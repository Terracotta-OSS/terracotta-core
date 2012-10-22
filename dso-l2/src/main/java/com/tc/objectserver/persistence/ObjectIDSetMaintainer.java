package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorageMutationListener;
import org.terracotta.corestorage.Retriever;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.ObjectIDSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

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
        CollectObjectID oid = new CollectObjectID() {

          @Override
          public void setObjectID(byte type, ObjectID oid) {
                if (PersistentCollectionsUtil.isEvictableMapType(type)) {
                  evictableObjectIDSet.remove(oid);
                }

                if (PersistentCollectionsUtil.isPersistableCollectionType(type)) {
                  mapObjectIDSet.remove(oid);
                }
                extantObjectIDSet.add(oid);
          }
      };
        
        extractType(value, oid);
  }
  
  interface CollectObjectID {
      void setObjectID(byte type, ObjectID oid);
  }
  
  private byte extractType(Retriever<? extends byte[]> value,CollectObjectID collector) {
    try {
        ObjectInputStream array = new ObjectInputStream(new ByteArrayInputStream(value.retrieve()));
        long version = array.readLong();
        ObjectID id = new ObjectID(array.readLong());
        
        byte type = array.readByte();
        if (type <= 0 && type >= ManagedObjectState.MAX_TYPE) {
            throw new AssertionError("unknown object type in stream: " + Byte.toString(type));
        }
        
        return type;
    } catch ( IOException ioe ) {
        throw new RuntimeException("unknown type");
    }
  }
}
