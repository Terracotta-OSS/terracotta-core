/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tctest.spring.bean.ISharedLock;
import com.tctest.spring.integrationtests.SpringTwoServerTestSetup;

import junit.framework.Test;

/**
 * This class is testing shared lock behavior
 *
 * <ol>
 * <li> Trying to modify a shared object outside a shared lock should raise exception
 * <li> Locking on shared object applies to the whole cluster
 * <li> Shared value will not be propagated until execution thread exits the monitor
 * </ol>
 */
public class SharedLockTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME           = "SharedLock";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory-sharedlock.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/sharedlock-tc-config.xml";

  private ISharedLock         sharedLock1;
  private ISharedLock         sharedLock2;

  protected void setUp() throws Exception {
    super.setUp();

    sharedLock1 = (ISharedLock) server0.getProxy(ISharedLock.class, REMOTE_SERVICE_NAME);
    sharedLock2 = (ISharedLock) server1.getProxy(ISharedLock.class, REMOTE_SERVICE_NAME);
  }

  public void testSharedLock() throws Exception {
    logger.debug("testing ShareLock");

    long id1 = sharedLock1.getLocalID();
    long id2 = sharedLock2.getLocalID();

    assertTrue("Pre-condition is not satisfied - id1 - " + id1 + " - id2 - " + id2, id1 != id2);

    // test mutation w/o a lock
    sharedLock1.unlockedMutate();
    sharedLock2.unlockedMutate();

    LockAndMutate t1 = new LockAndMutate(sharedLock1);
    LockAndMutate t2 = new LockAndMutate(sharedLock2);
    t1.start();
    t2.start();

    LockAndMutate loser = null;
    LockAndMutate winner = null;
    while (true) {
      if (t1.wasFirst()) {
        winner = t1;
        break;
      } else if (t2.wasFirst()) {
        winner = t2;
        break;
      } else {
        Thread.sleep(250);
      }
    }

    loser = (winner == t1) ? t2 : t1;
    assertNotSame(winner, loser);

    // This sleep is unnecessary, but just used to help ferret out false positives
    Thread.sleep(5000);

    assertFalse(loser.wasFirst());
    assertTrue(winner.sharedLockHeld());
    assertFalse(loser.sharedLockHeld());
    assertNull(loser.getFirstHolder()); // This will be null for the loser since the winner has not yet committed

    winner.release();

    t1.join();
    t2.join();

    assertEquals(new Long(winner.getLocalID()), loser.getFirstHolder());

    logger.debug("!!!! Asserts passed !!!");
  }

  private static class LockAndMutate extends Thread {

    private final ISharedLock lock;

    LockAndMutate(ISharedLock lock) {
      this.lock = lock;
    }

    public void run() {
      lock.lockAndMutate();
    }

    Long getFirstHolder() {
      return lock.getFirstHolder();
    }

    boolean wasFirst() {
      return lock.isFirstHolder();
    }

    boolean sharedLockHeld() {
      return lock.sharedLockHeld();
    }

    void release() {
      lock.release();
    }

    long getLocalID() {
      return lock.getLocalID();
    }

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
