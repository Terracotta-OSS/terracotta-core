/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.wrapper;

import com.tc.exception.TCRuntimeException;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.Transaction;
import java.io.IOException;
import java.util.Map;

public interface TCMapsDatabase {
  /**
   * Updates an entry<K, V> for a particular map identified by id in the DB.
   * 
   * @throws IOException
   * @throws TCDatabaseException
   */
  public int update(Transaction tx, long id, Object key, Object value, TCCollectionsSerializer serializer)
      throws IOException, TCRuntimeException;

  /**
   * Inserts an entry<K, V> for a particular map identified by id into the DB.
   * 
   * @throws IOException
   * @throws TCDatabaseException
   */
  public int insert(Transaction tx, long id, Object key, Object value, TCCollectionsSerializer serializer)
      throws IOException, TCRuntimeException;

  /**
   * Deletes a key from the map whose object id is passed in as the parameter
   * 
   * @throws IOException
   */
  public int delete(Transaction tx, long id, Object key, TCCollectionsSerializer serializer)
      throws TCRuntimeException, IOException;

  /**
   * Deletes an entire collection
   */

  public int deleteCollection(long id, Transaction tx) throws TCRuntimeException;

  /**
   * Deletes a collection using the {@link BatchedTransaction}. Methods calling this interface should call
   * {@link BatchedTransaction#startBatchedTransaction()} before calling this and call
   * {@link BatchedTransaction#completeBatchedTransaction()} after this method
   * 
   * @throws TCDatabaseException
   */
  public void deleteCollectionBatched(long id, BatchedTransaction batchedTransaction) throws TCRuntimeException;

  public void loadMap(Transaction tx, long id, Map map, TCCollectionsSerializer serializer)
      throws TCRuntimeException;

  /**
   * Return the number of entries in the database. Used in tests.
   */
  public long count(Transaction tx);

  /**
   * Returns a factory that is used to create a map that is backed by this DB
   */
  public BackingMapFactory getBackingMapFactory(TCCollectionsSerializer serializer);

  public interface BackingMapFactory {

    public Map createBackingMapFor(ObjectID mapID);

    public Map createBackingTinyMapFor(ObjectID mapID);

  }

}