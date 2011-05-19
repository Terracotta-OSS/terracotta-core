/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.derby.DerbyDBEnvironment;

import java.io.File;
import java.io.IOException;

public class DBDeleteLargeMapDerbyTest extends AbstractDBCollectionsDeleteTest {

  @Override
  protected DBEnvironment getDBEnvironMent(File dbHome) throws IOException {
    return new DerbyDBEnvironment(true, dbHome);
  }

  public void testDeleteLargeMap() throws Exception {
    super.doTestDeleteMap(1, 150000);
  }

}
