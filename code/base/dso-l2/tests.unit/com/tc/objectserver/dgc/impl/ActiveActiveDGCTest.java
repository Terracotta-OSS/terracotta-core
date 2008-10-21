/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet;

import java.util.Random;

public class ActiveActiveDGCTest extends TCTestCase {

  public void testGGCRun() {
    ClientStateManager clientStateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));
    ObjectManagerArray objMgrArray = new ObjectManagerArray(10, clientStateManager);

    MarkAndSweepGarbageCollector gc = new MarkAndSweepGarbageCollector(objMgrArray, clientStateManager,
                                                                       new ObjectManagerConfig(60000, true, true, true,
                                                                                               false, 6000000));

    objMgrArray.setGarbageCollector(gc);

    ActiveActiveGCHook aaGCHook = new ActiveActiveGCHook(objMgrArray, gc);

    ObjectIDSet oidSet = createObjectIDSet(600000);
    objMgrArray.createObjects(oidSet);
    
    objMgrArray.start();
    long start = System.currentTimeMillis();

    System.out.println("FULL GC STARTED");
    gc.start();
    gc.doGC(aaGCHook);
    
    System.out.println("Full GC took " + (System.currentTimeMillis() - start) + " millis");
  }

  private ObjectIDSet createObjectIDSet(int len) {
    Random ran = new Random();
    ObjectIDSet oidSet = new ObjectIDSet();

    for (int i = 0; i < len; i++) {
      oidSet.add(new ObjectID(ran.nextLong()));
    }
    return oidSet;
  }
}
