/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

//import com.tc.objectserver.persistence.db.TCDatabaseException;

import com.tc.exception.TCRuntimeException;


public interface OffHeapEvictedEntriesCommitManager {

  public void commitEvictedEntries() throws TCRuntimeException;

}
