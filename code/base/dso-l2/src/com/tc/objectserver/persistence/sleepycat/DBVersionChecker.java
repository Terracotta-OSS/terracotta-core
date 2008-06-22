/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.persistence.api.PersistentMapStore;

public class DBVersionChecker {

  private static final String   DBKEY_VERSION      = "DBKEY_VERSION";
  private static final String   DB_VERISON_1       = "1.0";
  private static final String   DB_VERSION_CURRENT = DB_VERISON_1;

  private PersistentMapStore    clusterStore;
  private static final TCLogger logger             = CustomerLogging.getDSOGenericLogger();

  public DBVersionChecker(PersistentMapStore clusterStore) {
    this.clusterStore = clusterStore;
  }

  public void versionCheck() {
    String dbVersion = null;
    try {
      dbVersion = clusterStore.get(DBKEY_VERSION);
      if (dbVersion == null) {
        clusterStore.put(DBKEY_VERSION, DB_VERSION_CURRENT);
        logger.info("Sleepy Cat DB version is " + DB_VERSION_CURRENT);
      } else {
        logger.info("Sleepy Cat DB version is " + dbVersion);
        if (!dbVersion.equals(DB_VERSION_CURRENT)) { throw new AssertionError(
                                                                              "There is a mismatch in Terracotta and DB data format. "
                                                                                  + "Please ensure that both Terracotta Server and "
                                                                                  + "DB are upgraded to the same version."
                                                                                  + " Expected : " + DB_VERSION_CURRENT
                                                                                  + " Actual: " + dbVersion); }
      }
    } catch (DBException e) {
      // the key was not found
      throw new AssertionError("Unable to get the value of the version from the DB");
    }
  }
}
