/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.sequence;

public interface ObjectIDSequence {

  /**
   * Requests a new batch of object ids.
   * 
   * @param batchSize The number of object ids you want in your batch.
   * @return The first id of the next batch of object ids.
   */
  public long nextObjectIDBatch(int batchSize);

  public void setNextAvailableObjectID(long startID);

  public long currentObjectIDValue();
}
