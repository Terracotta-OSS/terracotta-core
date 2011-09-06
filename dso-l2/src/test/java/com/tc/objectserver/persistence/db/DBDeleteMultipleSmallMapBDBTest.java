/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;

import java.io.File;
import java.io.IOException;

public class DBDeleteMultipleSmallMapBDBTest extends AbstractDBCollectionsDeleteTestParent {

  @Override
  protected DBEnvironment getDBEnvironMent(File dbHome) throws IOException {
    return new BerkeleyDBEnvironment(true, dbHome);
  }

  public void testMap() throws Exception {
    super.doTestDeleteMap(5000, 10);
  }

}
