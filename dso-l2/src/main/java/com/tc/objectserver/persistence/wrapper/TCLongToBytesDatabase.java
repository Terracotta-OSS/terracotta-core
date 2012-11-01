/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.wrapper;

import com.tc.objectserver.api.Transaction;

public interface TCLongToBytesDatabase {
  /**
   * Puts a <long, byte[]> key-value pair into the db.
   */
  public Status put(long id, byte[] value, Transaction tx);

  /**
   * Inserts a <long, byte[]> key-value pair to the db.
   */
  public Status insert(long id, byte[] b, Transaction tx);

  /**
   * Updates a <long, byte[]> key-value pair to the db.
   */
  public Status update(long id, byte[] b, Transaction tx);

  /**
   * Gets the value <byte[]> mapped to the key id
   */
  public byte[] get(long id, Transaction tx);

  /**
   * Deletes a <long, byte[]> key-value pair to the db.
   */
  public Status delete(long id, Transaction tx);

}