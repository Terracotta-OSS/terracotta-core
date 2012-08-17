/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;

import java.io.Serializable;

public class FastReadSlowWriteTestClient extends ClientBase {

  public static final int NODE_COUNT = 10;

  public FastReadSlowWriteTestClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) throws Exception {
    int myId = getBarrierForAllClients().await();
    ToolkitMap<Serializable, Serializable> sharedMap = toolkit.getMap("testMap", null, null);
    if (myId % 5 == 1) {
      new TestWriter(sharedMap, "" + myId).write();
    } else {
      new TestReader(sharedMap, "" + myId).read();
    }
  }

}
