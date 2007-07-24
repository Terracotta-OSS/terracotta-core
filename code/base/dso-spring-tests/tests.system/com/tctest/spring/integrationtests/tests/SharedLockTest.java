/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tctest.spring.bean.ISharedLock;
import com.tctest.spring.integrationtests.SpringTwoServerTestSetup;

import java.util.List;

import junit.framework.Test;

/**
 *  This class is testing shared lock behavior
 *  1. Locking on shared object applies to the whole cluster
 *  2. Trying to modify a shared objec outside a shared lock should raise exception
 *  3. Shared value will not be propagated until execution thread exits the monitor
 */
public class SharedLockTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME           = "SharedLock";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory-sharedlock.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/sharedlock-tc-config.xml";

  private ISharedLock   sharedLock1;
  private ISharedLock   sharedLock2;

  
  protected void setUp() throws Exception {
    super.setUp();

    sharedLock1 = (ISharedLock) server0.getProxy(ISharedLock.class, REMOTE_SERVICE_NAME);
    sharedLock2 = (ISharedLock) server1.getProxy(ISharedLock.class, REMOTE_SERVICE_NAME);
  }
  
  public void testSharedLock() throws Exception {

    logger.debug("testing ShareLock");
    sharedLock1.start();
    sharedLock2.start();

    long id1 = sharedLock1.getLocalID();
    long id2 = sharedLock2.getLocalID();
    
    List sharedVar1 = sharedLock1.getSharedVar();
    List sharedVar2 = sharedLock2.getSharedVar();
    List unSharedVar1 = sharedLock1.gethUnSharedVar();
    List unSharedVar2 = sharedLock2.gethUnSharedVar();
    
    
    assertTrue("Pre-condition is not satisfied - id1 - " + id1 + " - id2 - " + id2, 
               id1 != id2);
    assertEquals("Pre-condition is not satisfied", 0, unSharedVar1.size());
    assertEquals("Pre-condition is not satisfied", 0, unSharedVar2.size());
    assertEquals("Pre-condition is not satisfied", 0, sharedVar2.size());
    assertEquals("Pre-condition is not satisfied", 0, sharedVar2.size());
    
    sharedLock1.moveToStep(10);
    sharedLock2.moveToStep(10);
    
    sharedVar1 = sharedLock1.getSharedVar();
    sharedVar2 = sharedLock2.getSharedVar();
    unSharedVar1 = sharedLock1.gethUnSharedVar();
    unSharedVar2 = sharedLock2.gethUnSharedVar();

    assertEquals("Expected exception not happen: " + sharedVar1, 0, sharedVar1.size());
    assertEquals("Expected exception not happen: " + sharedVar2, 0, sharedVar2.size());
    assertEquals("Expected exception not happen: " + sharedVar1, 1, unSharedVar1.size());
    assertEquals("Expected exception not happen: " + sharedVar2, 1, unSharedVar2.size());
    assertEquals("Expected exception not happen: " + sharedVar1, unSharedVar1.get(0), "ckpoint1-"+id1);
    assertEquals("Expected exception not happen: " + sharedVar2, unSharedVar2.get(0), "ckpoint1-" + id2);

    sharedLock1.moveToStep(20); // sharedLock1 get the lock and modify the sharedVar
    sharedLock2.moveToStep(20); // sharedLock1 should block

    sharedVar1 = sharedLock1.getSharedVar();
    sharedVar2 = sharedLock2.getSharedVar();
  
    assertEquals("Failed to aquire the lock and modify the shared variable: " + sharedVar1, 1, sharedVar1.size());
    assertEquals("Failed to aquire the lock and modify the shared variable: " + sharedVar1,  sharedVar1.get(0), "ckpoint2-"+id1);
    assertEquals("Failed to be blocked: " + sharedVar2, 0, sharedVar2.size());
    
    sharedLock1.moveToStep(30); // sharedLock1 release the lock and propagate the value

    sharedVar1 = sharedLock1.getSharedVar();
    sharedVar2 = sharedLock2.getSharedVar();
  
    assertEquals("Unexpected modification: " + sharedVar1, 1, sharedVar1.size());
    assertEquals("Unexpected modification: " + sharedVar1, sharedVar1.get(0), "ckpoint2-"+id1);
    assertEquals("Failed to aquired lock and receive the propagation: " + sharedVar2, 2, sharedVar2.size());
    assertTrue("Failed to aquired lock and receive the propagation: " + sharedVar2, sharedVar2.contains("ckpoint2-"+id1));
    assertTrue("Failed to aquired lock and receive the propagation: " + sharedVar2, sharedVar2.contains("ckpoint2-"+id2));
    
    sharedLock2.moveToStep(30); // sharedLock1 release the lock and propagate the value

    sharedVar1 = sharedLock1.getSharedVar();
    sharedVar2 = sharedLock2.getSharedVar();
  
    assertEquals("Failed receive the propagation: " + sharedVar1, 2, sharedVar1.size());
    assertTrue("Failed receive the propagation: " + sharedVar1, sharedVar1.contains("ckpoint2-"+id1));
    assertTrue("Failed receive the propagation: " + sharedVar1, sharedVar1.contains("ckpoint2-"+id2));   
        
    logger.debug("!!!! Asserts passed !!!");
  }

  
  private static class SharedLockTestSetup extends SpringTwoServerTestSetup {
    private SharedLockTestSetup() {
      super(SharedLockTest.class, CONFIG_FILE_FOR_TEST, "test-sharedlock");
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(REMOTE_SERVICE_NAME, "sharedlock", ISharedLock.class);
    }
  }

  public static Test suite() {
    return new SharedLockTestSetup();
  }

}
