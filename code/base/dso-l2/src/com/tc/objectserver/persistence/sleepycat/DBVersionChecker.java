/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.objectserver.persistence.api.PersistentMapStore;

public class DBVersionChecker {
  private PersistentMapStore    clusterStore;
  public static final String    VERSION = "1.0";
  private static final TCLogger logger  = CustomerLogging.getDSOGenericLogger();

  public DBVersionChecker(PersistentMapStore clusterStore) {
    this.clusterStore = clusterStore;
  }

  public void versionCheck() {
    String dbVersion = null;
    try {
      dbVersion = clusterStore.get(DBVersionChecker.VERSION);
      if (dbVersion == null) {
        clusterStore.put(DBVersionChecker.VERSION, DBVersionChecker.VERSION);
        logger.info("Sleepy Cat DB version is " + DBVersionChecker.VERSION);
      } else {
        logger.info("Sleepy Cat DB version is " + dbVersion);
        if (!dbVersion.equals(DBVersionChecker.VERSION)) { 
          throw new AssertionError("There is a mismatch in Terracotta and DB data format. " +
          		                      "Please ensure that both Terracotta Server and DB are upgraded to the same version" + 
          		                      " Expected : " + DBVersionChecker.VERSION + " Actual: " + dbVersion); 
        }
      }
    } catch (DBException e) {
      // the key was not found
      throw new AssertionError("Unable to get the value of the version from the DB");
    }
  }
}
