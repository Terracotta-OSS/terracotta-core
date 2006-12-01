/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aop;

import org.springframework.aop.framework.ProxyFactory;

import junit.framework.TestCase;

/**
 * @author Jonas Bon&#233;r
 */
public class AopProxy_Test extends TestCase {
  public AopProxy_Test(String name) {
    super(name);
  }
  
  public static void testBeforeAdvice() {
    MessageWriter target = new MessageWriter();
    ProxyFactory pf = new ProxyFactory();
    pf.addAdvice(new SimpleBeforeAdvice());
    pf.setTarget(target);
    
    MessageWriter proxy = (MessageWriter) pf.getProxy();
    assertNotNull(proxy);
    
    String msg = proxy.writeMessage();
    assertNotNull(msg);
    assertEquals("World", msg);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  public static junit.framework.Test suite() {
    return new junit.framework.TestSuite(AopProxy_Test.class);
  }
}
