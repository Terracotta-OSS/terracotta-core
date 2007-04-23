/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tctest.spring.bean.ISimpleBean;

import java.util.HashSet;
import java.util.Set;

import junit.extensions.TestSetup;
import junit.framework.Test;

/**
 * Testing the following features
 * 1. Replication behavior
 * 2. Bean reference another bean and how the sharing
 *
 * Test depends on the order of the instantiation of the parent beans
 *
 * @author Liyu Yi
 */
public class ReferenceAndReplicationTest extends AbstractTwoServerDeploymentTest {

  private static final String SHAREDPARENT_SERVICE_NAME           = "ShareParent";
  private static final String LOCALCHILD1_SERVICE_NAME            = "LocalChild1";
  private static final String LOCALCHILD2_SERVICE_NAME            = "LocalChild2";
  private static final String LOCALCHILD3_SERVICE_NAME            = "LocalChild3";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST       = "classpath:/com/tctest/spring/beanfactory-referenceandreplication.xml";
  private static final String CONFIG_FILE_FOR_TEST                = "/tc-config-files/referenceandreplication-tc-config.xml";

  // for N1
  private static ISimpleBean  sharedParentN1;
  private static ISimpleBean  localChild1N1;
  private static ISimpleBean  localChild2N1;
  private static ISimpleBean  localChild3N1;

  // for N2
  private static ISimpleBean  sharedParentN2;
  private static ISimpleBean  localChild1N2;
  private static ISimpleBean  localChild2N2;
  private static ISimpleBean  localChild3N2;

  public void testReplication() throws Exception {
    logger.debug("testing replication of shared spring bean");
    // check pre-conditions
    long id2 = sharedParentN2.getId();
    long id1 = sharedParentN1.getId();

    assertFalse("Pre-condition check failed" , id1 == id2);

    long timeStamp1 = sharedParentN1.getTimeStamp();
    long timeStamp2 = sharedParentN2.getTimeStamp();
    long timeStampMin = timeStamp1 < timeStamp2 ? timeStamp1 : timeStamp2;


    // check shared field replication and also the behavior of
    // the GetBeanProtocol for the 2nd Node (which is N1) to grab the shared
    // bean from singletonCache in the mixin
    assertEquals("Shared field replication failed: " + sharedParentN1.getSharedId(), timeStampMin, sharedParentN1.getSharedId());
    assertEquals("Shared field replication failed: " + sharedParentN2.getSharedId(), timeStampMin, sharedParentN2.getSharedId());

    // check shared field replication again
    assertNull("", sharedParentN2.getField());
    sharedParentN1.setField("" + id1);
    assertEquals("Shared field replication failed: " + sharedParentN2.getField(), "" + id1, sharedParentN2.getField());

    // check transient field stopping replication
    assertNull("Pre-condition check failed", sharedParentN1.getTransientField());
    assertNull("Pre-condition check failed", sharedParentN2.getTransientField());
    sharedParentN1.setTransientField("" + id1);
    sharedParentN2.setTransientField("" + id2);
    assertEquals("Unexpected field replication: " + sharedParentN1.getTransientField(), "" + id1, sharedParentN1.getTransientField());
    assertEquals("Unexpected field replication: " + sharedParentN2.getTransientField(), "" + id2, sharedParentN2.getTransientField());

    // check "dso transient" field stopping replication
    assertNull("Pre-condition check failed", sharedParentN1.getDsoTransientField());
    assertNull("Pre-condition check failed", sharedParentN2.getDsoTransientField());
    sharedParentN1.setDsoTransientField("" + id1);
    sharedParentN2.setDsoTransientField("" + id2);
    assertEquals("Unexpected field replication: " + sharedParentN1.getDsoTransientField(), "" + id1, sharedParentN1.getDsoTransientField());
    assertEquals("Unexpected field replication: " + sharedParentN2.getDsoTransientField(), "" + id2, sharedParentN2.getDsoTransientField());

    // check static field stopping replication
    assertNull("Pre-condition check failed", sharedParentN1.getStaticField());
    assertNull("Pre-condition check failed", sharedParentN2.getStaticField());
    sharedParentN1.setStaticField("" + id1);
    sharedParentN2.setStaticField("" + id2);
    assertEquals("Unexpected field replication: " + sharedParentN1.getStaticField(), "" + id1, sharedParentN1.getStaticField());
    assertEquals("Unexpected field replication: " + sharedParentN2.getStaticField(), "" + id2, sharedParentN2.getStaticField());

    logger.debug("!!!! Asserts passed !!!");
  }

  public void testBeanReference() throws Exception {
    logger.debug("testing shared bean reference other beans");

    // check pre-conditions
    long id2 = sharedParentN2.getId();
    long id1 = sharedParentN1.getId();

    assertFalse("Pre-condition check failed" , id1 == id2);

    Set idSet = new HashSet();

    long id11 = localChild1N1.getId(); idSet.add(new Long(id11));
    long id21 = localChild2N1.getId(); idSet.add(new Long(id21));
    long id31 = localChild3N1.getId(); idSet.add(new Long(id31));
    long id12 = localChild1N2.getId(); idSet.add(new Long(id12));
    long id22 = localChild2N2.getId(); idSet.add(new Long(id22));
    long id32 = localChild3N2.getId(); idSet.add(new Long(id32));

    assertEquals("Pre-condition check failed: " + idSet, 6, idSet.size());

    // check localChild1 -- bean through shared reference is NOT shared if the reference bean is not declared as shared explicitly
//    localChild1N1.setField("localChild1N1");
//    localChild1N2.setField("localChild1N2");
//    assertEquals("Unexcpected field replication: " + localChild1N1.getField(), "localChild1N1", localChild1N1.getField());
//    assertEquals("Unexcpected field replication: " + localChild1N2.getField(), "localChild1N2", localChild1N2.getField());
//
//    assertEquals("Singleton semantics broken: " + id11 + " - " + sharedParentN1.getSharedRefId(), id11, sharedParentN1.getSharedRefId());
//    assertEquals("Singleton semantics broken: " + id12 + " - " + sharedParentN2.getSharedRefId(), id12, sharedParentN2.getSharedRefId());

    // check localChild2 -- bean through transient reference is not shared
    localChild2N1.setField("localChild2N1");
    localChild2N2.setField("localChild2N2");
    assertEquals("Unexcpected field replication: " + localChild2N1.getField(), "localChild2N1", localChild2N1.getField());
    assertEquals("Unexcpected field replication: " + localChild2N2.getField(), "localChild2N2", localChild2N2.getField());

    // check localChild3 -- bean through dso transient reference is not shared
    localChild3N1.setField("localChild3N1");
    localChild3N2.setField("localChild3N2");
    assertEquals("Unexcpected field replication: " + localChild3N1.getField(), "localChild3N1", localChild3N1.getField());
    assertEquals("Unexcpected field replication: " + localChild3N2.getField(), "localChild3N2", localChild3N2.getField());

    logger.debug("!!!! Asserts passed !!!");
  }

  private static class InnerTestSetup extends TwoSvrSetup {
    private InnerTestSetup() {
      super(ReferenceAndReplicationTest.class, CONFIG_FILE_FOR_TEST, "test-referenceandreplication");
    }

    protected void setUp() throws Exception {
      super.setUp();
      // for N1
      localChild1N1 = (ISimpleBean) server1.getProxy(ISimpleBean.class, LOCALCHILD1_SERVICE_NAME);
      localChild2N1 = (ISimpleBean) server1.getProxy(ISimpleBean.class, LOCALCHILD2_SERVICE_NAME);
      localChild3N1 = (ISimpleBean) server1.getProxy(ISimpleBean.class, LOCALCHILD3_SERVICE_NAME);
      sharedParentN1 = (ISimpleBean) server1.getProxy(ISimpleBean.class, SHAREDPARENT_SERVICE_NAME);
      // for N2
      localChild1N2 = (ISimpleBean) server2.getProxy(ISimpleBean.class, LOCALCHILD1_SERVICE_NAME);
      localChild2N2 = (ISimpleBean) server2.getProxy(ISimpleBean.class, LOCALCHILD2_SERVICE_NAME);
      localChild3N2 = (ISimpleBean) server2.getProxy(ISimpleBean.class, LOCALCHILD3_SERVICE_NAME);
      sharedParentN2 = (ISimpleBean) server2.getProxy(ISimpleBean.class, SHAREDPARENT_SERVICE_NAME);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(SHAREDPARENT_SERVICE_NAME, "sharedParent", ISimpleBean.class);
      builder.addRemoteService(LOCALCHILD1_SERVICE_NAME, "localChild1", ISimpleBean.class);
      builder.addRemoteService(LOCALCHILD2_SERVICE_NAME, "localChild2", ISimpleBean.class);
      builder.addRemoteService(LOCALCHILD3_SERVICE_NAME, "localChild3", ISimpleBean.class);
    }
  }

  /**
   *  JUnit test loader entry point
   */
  public static Test suite() {
    TestSetup setup = new InnerTestSetup();
    return setup;
  }
}
