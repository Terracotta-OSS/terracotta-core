/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.db.TCCollectionsSerializer;
import com.tc.objectserver.persistence.db.TCDatabaseException;

import java.io.IOException;
import java.util.Map;

public interface TCMapsDatabase {
  /**
   * Puts an entry<K,Y> for a particular map identified by id into the DB. The id here is the object id of the map.
   * 
   * @throws IOException
   */
  public int put(PersistenceTransaction tx, long id, Object key, Object value, TCCollectionsSerializer serializer)
      throws TCDatabaseException, IOException;

  /**
   * Updates an entry<K, V> for a particular map identified by id in the DB.
   * 
   * @throws IOException
   * @throws TCDatabaseException
   */
  public int update(PersistenceTransaction tx, long id, Object key, Object value, TCCollectionsSerializer serializer)
      throws IOException, TCDatabaseException;

  /**
   * Inserts an entry<K, V> for a particular map identified by id into the DB.
   * 
   * @throws IOException
   * @throws TCDatabaseException
   */
  public int insert(PersistenceTransaction tx, long id, Object key, Object value, TCCollectionsSerializer serializer)
      throws IOException, TCDatabaseException;

  /**
   * Deletes a key from the map whose object id is passed in as the parameter
   * 
   * @throws IOException
   */
  public int delete(PersistenceTransaction tx, long id, Object key, TCCollectionsSerializer serializer)
      throws TCDatabaseException, IOException;

  /**
   * Deletes an entire collection
   */

  public void deleteCollection(long id, PersistenceTransaction tx) throws TCDatabaseException;

  /**
   * Deletes a collection but only up to a max delete batch size and returns the number of entries deleted.
   * 
   * @return number of entries in Maps database deleted, if less than DELETE_BATCH_SIZE, then there could be more
   *         entries for the same map ID.
   * @throws TCDatabaseException
   */
  public int deleteCollectionBatched(long id, PersistenceTransaction tx, int maxDeleteBatchSize);

  public void loadMap(PersistenceTransaction tx, long id, Map map, TCCollectionsSerializer serializer)
      throws TCDatabaseException;

  /**
   * Return the number of entries in the database. Used in tests.
   */
  public long count(PersistenceTransaction tx);

  /**
   * Returns a factory that is used to create a map that is backed by this DB
   */
  public BackingMapFactory getBackingMapFactory(TCCollectionsSerializer serializer);

  public interface BackingMapFactory {

    public Map createBackingMapFor(ObjectID mapID);
  }

}
