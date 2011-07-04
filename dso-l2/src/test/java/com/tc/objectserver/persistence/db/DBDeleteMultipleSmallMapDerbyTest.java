/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.derby.DerbyDBEnvironment;

import java.io.File;
import java.io.IOException;

public class DBDeleteMultipleSmallMapDerbyTest extends AbstractDBCollectionsDeleteTestParent {

  @Override
  protected DBEnvironment getDBEnvironMent(File dbHome) throws IOException {
    return new DerbyDBEnvironment(true, dbHome);
  }

  public void testMap() throws Exception {
    super.doTestDeleteMap(5000, 10);
  }

}
