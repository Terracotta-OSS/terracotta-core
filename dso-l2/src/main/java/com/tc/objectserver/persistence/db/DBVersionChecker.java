/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.persistence.api.PersistentMapStore;

public class DBVersionChecker {

  private static final String DBKEY_VERSION = "DBKEY_VERSION";

  private static enum DbVersions {
    /**
     * TC r2.6 : dbVersion 1.0; TC r2.7 : dbVersion 2.0;
     */
    DB_VERSION_1("1.0"), DB_VERSION_2("2.0"), DB_VERSION_2_1("2.1"), DB_VERSION_2_2("2.2");

    private String version;

    private DbVersions(String ver) {
      this.version = ver;
    }

    String getVersion() {
      return this.version;
    }

    @Override
    public String toString() {
      return "Database version [" + getVersion() + "]";
    }

    // TODO: db upgrade/revert routines
  }

  private static final DbVersions  DB_VERSION_CURRENT = DbVersions.DB_VERSION_2_2;

  private final PersistentMapStore clusterStore;
  private static final TCLogger    logger             = CustomerLogging.getDSOGenericLogger();

  public DBVersionChecker(PersistentMapStore clusterStore) {
    this.clusterStore = clusterStore;
  }

  public void versionCheck() {
    String dbVersion = null;
    try {
      dbVersion = clusterStore.get(DBKEY_VERSION);
      if (dbVersion == null) {
        clusterStore.put(DBKEY_VERSION, DB_VERSION_CURRENT.getVersion());
        logger.info("Terracotta persistence version is " + DB_VERSION_CURRENT.getVersion());
      } else {
        logger.info("Terracotta persistence version is " + dbVersion);
        if (!dbVersion.equals(DB_VERSION_CURRENT.getVersion())) { throw new DBVersionMismatchException(
                                                                                                       "There is a mismatch in Terracotta and DB data format. "
                                                                                                           + "Please ensure that both Terracotta Server instance and "
                                                                                                           + "DB are upgraded to the same version."
                                                                                                           + " Expected : "
                                                                                                           + DB_VERSION_CURRENT
                                                                                                               .getVersion()
                                                                                                           + " Actual: "
                                                                                                           + dbVersion); }
      }
    } catch (DBException e) {
      // the key was not found
      throw new AssertionError("Unable to get the value of the version from the DB");
    }
  }
}
