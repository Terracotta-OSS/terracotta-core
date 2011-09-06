/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.TCMap;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UnclusteredHashMapTestApp extends AbstractErrorCatchingTransparentApp {

  public UnclusteredHashMapTestApp(final String appId, final ApplicationConfig cfg,
                                   final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    System.out.println("Starting test...");
    try {
      HashMap map = new HashMap();
      for (int i = 0; i < 5; i++) {
        map.put("key-" + i, "Value-" + i);
      }

      ((TCMap) map).__tc_remove_logical("key-0");
      Assert.assertEquals(false, map.containsKey("key-0"));

      ((TCMap) map).__tc_put_logical("key-0", "Value-0");
      Assert.assertEquals(true, map.containsKey("key-0"));

      Collection coll;

      coll = ((TCMap) map).__tc_getAllEntriesSnapshot();
      Assert.assertEquals(5, coll.size());
      for (Iterator iter = coll.iterator(); iter.hasNext();) {
        Map.Entry entry = (Map.Entry) iter.next();
        System.out.println("The entry: " + entry);
      }

      coll = ((TCMap) map).__tc_getAllLocalEntriesSnapshot();
      Assert.assertEquals(5, coll.size());
      for (Iterator iter = coll.iterator(); iter.hasNext();) {
        Map.Entry entry = (Map.Entry) iter.next();
        System.out.println("The entry: " + entry);
      }

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("Caught Unexpected Exception: " + e);
    }
    System.out.println("Test passed successfully");
  }
}
