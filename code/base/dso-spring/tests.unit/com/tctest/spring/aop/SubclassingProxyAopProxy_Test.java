/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aop;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.test.TCTestCase;

// FIXME test IntroductionInterceptor
// FIXME more complex tests - chained tests etc. 

/**
 * @author Jonas Bon&#233;r
 */
public class SubclassingProxyAopProxy_Test extends TCTestCase {
  
  private static final String BEAN_CONFIG = "com/tctest/spring/beanfactory-fastproxy.xml";

  public SubclassingProxyAopProxy_Test(String name) {
    super(name);
    disableAllUntil("2008-09-18");  // XXX timebombed
  }
  
  public static void testBeforeAdvice() {
    Logger.log = "";
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(BEAN_CONFIG);
    SubclassingProxyTarget proxy = (SubclassingProxyTarget) ctx.getBean("testBeforeAdviceSubclassing");
    assertNotNull(proxy);    
    proxy.doStuff("fuzzy");
    assertEquals("before args(fuzzy) this(" + proxy.getClass().getName() + ") doStuff ", Logger.log);
  }

  public static void testAfterReturningAdvice() {
    Logger.log = "";
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(BEAN_CONFIG);
    SubclassingProxyTarget proxy = (SubclassingProxyTarget) ctx.getBean("testAfterReturningAdviceSubclassing");
    assertNotNull(proxy);
    String stuff = proxy.returnStuff("fuzzy");
    assertEquals("returnStuff after-returning(stuff) args(fuzzy) this(" + proxy.getClass().getName() + ") ", Logger.log);
  }

  public static void testAfterThrowingAdvice() {
    Logger.log = "";
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(BEAN_CONFIG);
    SubclassingProxyTarget proxy = (SubclassingProxyTarget) ctx.getBean("testAfterThrowingAdviceSubclassing");
    assertNotNull(proxy);
    try {
      proxy.throwStuff("fuzzy");
    } catch (ExpectedException e) {
      e.printStackTrace();
      assertEquals("throwStuff after-throwing(expected) args(fuzzy) this(" + proxy.getClass().getName() + ") ", Logger.log);
      return;
    }
    fail("should have exited with an exception");
  }

  public static void testAroundAdvice() {
    Logger.log = "";
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(BEAN_CONFIG);
    SubclassingProxyTarget proxy = (SubclassingProxyTarget) ctx.getBean("testAroundAdviceSubclassing");
    assertNotNull(proxy);
    proxy.doStuff("fuzzy");
    assertEquals("before-around args(fuzzy) this(" + proxy.getClass().getName() + ") doStuff after-around ", Logger.log);
  }


  public static void testAroundAdviceChain() {
    Logger.log = "";
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(BEAN_CONFIG);
    SubclassingProxyTarget proxy = (SubclassingProxyTarget) ctx.getBean("testAroundAdviceChainSubclassing");
    assertNotNull(proxy);    
    proxy.doStuff("fuzzy");
    assertEquals("before-around args(fuzzy) this(" + proxy.getClass().getName() + ") before-around args(fuzzy) this(" + proxy.getClass().getName() + ") doStuff after-around after-around ", Logger.log);
  }

  // XXX use test decoration to activate AW pipeline
  public static junit.framework.Test suite() {
    return new junit.framework.TestSuite(SubclassingProxyAopProxy_Test.class);
  }
  
}

