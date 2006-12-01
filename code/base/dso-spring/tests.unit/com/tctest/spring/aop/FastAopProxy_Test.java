/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aop;

import org.springframework.aop.framework.ProxyFactoryBean;

import com.tc.test.TCTestCase;
import com.tcspring.FastAopProxy;


// XXX use test decorator to activate AW pipeline
public class FastAopProxy_Test extends TCTestCase {
  IMessageWriter target = null;
  ProxyFactoryBean proxyFactoryBean = null;
  
  Object cglibProxied = null;
  Object cglibProxied2 = null;
  
  Object jdkDynamicProxied = null;
  Object jdkDynamicProxied2 = null;
  
  FastAopProxy fastProxy = null;
  
  
  public FastAopProxy_Test() {
    disableAllUntil("2008-09-18");  // XXX timebombed
  }
  
  public void setUp() {
    target = new MessageWriter();
    
    proxyFactoryBean = new ProxyFactoryBean();
    proxyFactoryBean.setFrozen(false);
    proxyFactoryBean.setOptimize(true);
    proxyFactoryBean.setTarget(target);    
    proxyFactoryBean.addAdvice(new SimpleBeforeAdvice());
    
    cglibProxied = proxyFactoryBean.getObject();

    proxyFactoryBean.setInterfaces(new Class[] {IMessageWriter.class });
    proxyFactoryBean.setOptimize(false);
    jdkDynamicProxied = proxyFactoryBean.getObject();

    // get the 2nd copy
    
    proxyFactoryBean = new ProxyFactoryBean();
    proxyFactoryBean.setFrozen(false);
    proxyFactoryBean.setOptimize(true);
    proxyFactoryBean.setTarget(target);    
    proxyFactoryBean.addAdvice(new SimpleBeforeAdvice());
    
    cglibProxied2 = proxyFactoryBean.getObject();
    
    proxyFactoryBean.setInterfaces(new Class[] {IMessageWriter.class });
    proxyFactoryBean.setOptimize(false);
    
    jdkDynamicProxied2 = proxyFactoryBean.getObject();
    
    // get the fast proxy
    
    fastProxy = new FastAopProxy(proxyFactoryBean);
  }
  
  public void testClassLoader() throws Exception {   
    assertTrue(cglibProxied instanceof MessageWriter);
       
    assertTrue(jdkDynamicProxied instanceof IMessageWriter);
    assertFalse(jdkDynamicProxied instanceof MessageWriter);
    
    Object fastProxied1 = fastProxy.getProxy();
    Object fastProxied2 = fastProxy.getProxy(target.getClass().getClassLoader());
    Object fastProxied3 = fastProxy.getProxy(new MyClassLoader(target.getClass().getClassLoader()));
    
    assertTrue(fastProxied1 instanceof IMessageWriter);
    assertFalse(fastProxied1 instanceof MessageWriter);
    
    assertTrue(fastProxied2 instanceof IMessageWriter);
    assertFalse(fastProxied2 instanceof MessageWriter);
    
    assertTrue(fastProxied3 instanceof IMessageWriter);
    assertFalse(fastProxied3 instanceof MessageWriter);
  }
  
  // interesting to observation
  public void testEquality() throws Exception {   
    assertFalse(jdkDynamicProxied.equals(jdkDynamicProxied2));
    
    assertFalse(cglibProxied.equals(cglibProxied2));
    
    Object fastProxied1 = fastProxy.getProxy();
    assertFalse(fastProxied1.equals(fastProxy.getProxy()));
  }
  
  public void testBeforeAdvice() throws Exception {
    Logger.log = null;
    String cglibProxiedRv = ((MessageWriter)cglibProxied).writeMessage();
    String cglibProxiedLog = Logger.log;
    assertEquals("World", cglibProxiedRv);
    assertTrue(cglibProxiedLog.indexOf("*EMPTY*") > -1 );
    
    Logger.log = null;
    String jdkDynamicProxiedRv = ((IMessageWriter)cglibProxied).writeMessage();
    String jdkDynamicProxiedLog = Logger.log;
    assertEquals("World", jdkDynamicProxiedRv);
    assertTrue(jdkDynamicProxiedLog.indexOf("*EMPTY*") > -1);
    
    Object fastProxied = fastProxy.getProxy();
    Logger.log = null;
    String fastProxiedRv = ((IMessageWriter)fastProxied).writeMessage();
    String fastProxiedLog = Logger.log;
    assertEquals("World", fastProxiedRv);
    assertTrue("This is what we get: " + fastProxiedLog, fastProxiedLog.indexOf("*EMPTY*") > -1);
  }
  
  public void testNested1() {
    ProxyFactoryBean factory = new ProxyFactoryBean();
    factory.setFrozen(false);
    factory.setOptimize(true);
    factory.setInterfaces(new Class[]{IMessageWriter.class});
    factory.setTarget(cglibProxied);

    FastAopProxy proxy = new FastAopProxy(factory);
    
    Object nested = proxy.getProxy();
    
    Logger.log = null;
    String nestedRv = ((IMessageWriter)nested).writeMessage();
    String nestedLog = Logger.log;
    assertEquals("World", nestedRv);
  }
  
  public void testNested2() {
    ProxyFactoryBean factory = new ProxyFactoryBean();
    factory.setFrozen(false);
    factory.setOptimize(true);
    factory.setInterfaces(new Class[]{IMessageWriter.class});
    factory.setTarget(this.jdkDynamicProxied);

    FastAopProxy proxy = new FastAopProxy(factory);
    
    Object nested = proxy.getProxy();
    
    Logger.log = null;
    String nestedRv = ((IMessageWriter)nested).writeMessage();
    String nestedLog = Logger.log;
    assertEquals("World", nestedRv);
  }
  
  public void testNested3() {
    ProxyFactoryBean factory = new ProxyFactoryBean();
    factory.setFrozen(false);
    factory.setOptimize(true);
    factory.setInterfaces(new Class[]{IMessageWriter.class});
    factory.setTarget(fastProxy.getProxy());

    FastAopProxy proxy = new FastAopProxy(factory);
    
    Object nested = proxy.getProxy();
    
    Logger.log = null;
    String nestedRv = ((IMessageWriter)nested).writeMessage();
    String nestedLog = Logger.log;
    assertEquals("World", nestedRv);
  }
  
  public void testNullConfig() {
    try {
      this.fastProxy = new FastAopProxy(null);
      fastProxy.getProxy();
      fail("Shouldn't allow null interceptors");
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void testNoTarget() {
    proxyFactoryBean = new ProxyFactoryBean();
    proxyFactoryBean.setFrozen(false);
    proxyFactoryBean.setOptimize(true);
    proxyFactoryBean.setInterfaces(new Class[] {IMessageWriter.class});
    proxyFactoryBean.addAdvice(new SimpleBeforeAdvice());
    
    try {
      this.fastProxy = new FastAopProxy(proxyFactoryBean);
      fastProxy.getProxy();
      fail("Shouldn't allow null interceptors");
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  
  
  private static class MyClassLoader extends ClassLoader {
    public MyClassLoader(ClassLoader parent) {
      super(parent);
    }
  }
    
}
