/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.test.TCTestCase;
import com.tc.util.UUID;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashMap;

import junit.framework.Assert;

public class ServerThreadContextFactoryTest extends TCTestCase {
  private final int                  TEST_COUNT = 200;
  private ServerThreadContextFactory factory;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    factory = new ServerThreadContextFactory();
  }

  private byte[] getUUID() {
    return UUID.getUUID().toString().getBytes();
  }

  public void testServerThreadContactFactory() {
    HashMap<NodeID, ServerThreadContext> map = new HashMap<NodeID, ServerThreadContext>();
    ThreadID tid = new ThreadID(1, "Test thread id 1");
    for (int i = 0; i < TEST_COUNT; ++i) {
      NodeID nid = new ServerID("Server-" + i, getUUID());
      ServerThreadContext context = factory.getOrCreate(nid, tid);
      map.put(nid, context);
    }
    Assert.assertEquals(TEST_COUNT, factory.size());

    // verify caching
    for (NodeID nid : map.keySet()) {
      Assert.assertTrue(map.get(nid) == factory.getOrCreate(nid, tid));
    }

    // verify GCing WeakHashMap stuffs
    int count = TEST_COUNT;
    for (NodeID nid : map.keySet()) {
      map.put(nid, null);
      System.gc();
      ThreadUtil.reallySleep(10);
      System.gc();
      System.out.println("XX GCing item " + count);
      Assert.assertEquals(--count, factory.size());
    }

    map.clear();
    System.gc();
    Assert.assertEquals(0, factory.size());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

}
