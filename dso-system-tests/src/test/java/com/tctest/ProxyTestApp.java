/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.loaders.IsolationClassLoader;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProxyTestApp extends AbstractTransparentApp {
  private final CyclicBarrier barrier;
  private MyProxyInf1         proxyImpl;

  private final DataRoot      dataRoot = new DataRoot();

  public ProxyTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();

      proxyDSOInterfaces1(index);
      proxyDSOInterfaces2(index);
      proxySystemInterface(index);
      validProxyTest(index);
      subclassProxyTest(index);
      differentLoaderTest(index);
      multipleInterfacesProxyTest(index);
      reflectionTest(index);

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  /**
   * A simple test to create a proxy with DSO interfaces.
   */
  private void proxyDSOInterfaces1(int index) throws Throwable {
    if (index == 0) {
      MyInvocationHandler handler = new MyInvocationHandler();
      Proxy myProxy = (Proxy) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { Manageable.class,
          MyProxyInf1.class, MyProxyInf2.class, TransparentAccess.class }, handler);
      dataRoot.setMyProxy(myProxy);
    }

    barrier.barrier();

    Class[] interfaces = dataRoot.getMyProxy().getClass().getInterfaces();
    Assert.assertEquals(2, interfaces.length);
    for (Class interface1 : interfaces) {
      Assert.assertFalse(interface1.equals(Manageable.class.getName()));
      Assert.assertFalse(interface1.equals(TransparentAccess.class.getName()));
    }

    barrier.barrier();
  }

  private void proxyDSOInterfaces2(int index) throws Throwable {
    if (index == 0) {
      MyInvocationHandler handler = new MyInvocationHandler();
      Object o = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { Manageable.class,
          TransparentAccess.class, Serializable.class }, handler);

      dataRoot.setMyProxy(o);
    }

    barrier.barrier();

    Class[] interfaces = dataRoot.getMyProxy().getClass().getInterfaces();
    Assert.assertEquals(1, interfaces.length);
    Assert.assertEquals(Serializable.class.getName(), interfaces[0].getName());

    barrier.barrier();

  }

  private void reflectionTest(int index) throws Throwable {
    if (index == 0) {
      MyInvocationHandler handler = new MyInvocationHandler();
      Proxy myProxy = (Proxy) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { MyProxyInf1.class,
          MyProxyInf2.class }, handler);
      ((MyProxyInf2) myProxy).setStringValue("Testing2");
      dataRoot.setMyProxy(myProxy);
    }

    barrier.barrier();

    Assert.assertEquals("Testing2", ((MyProxyInf2) dataRoot.getMyProxy()).getStringValue());

    barrier.barrier();

    if (index == 1) {
      MyInvocationHandler handler = new MyInvocationHandler();
      Proxy myProxy = (Proxy) dataRoot.getMyProxy();
      Field field = myProxy.getClass().getSuperclass().getDeclaredField("h"); // get the invocation handler field
      // through reflection.
      field.setAccessible(true);
      synchronized (myProxy) {
        field.set(myProxy, handler);
      }
    }

    barrier.barrier();

    Assert.assertNull(((MyProxyInf2) dataRoot.getMyProxy()).getStringValue());

    barrier.barrier();

    if (index == 0) {
      synchronized (dataRoot.getMyProxy()) {
        ((MyProxyInf1) dataRoot.getMyProxy()).setValue(2002);
      }
    }

    barrier.barrier();

    Assert.assertEquals(2002, ((MyProxyInf1) dataRoot.getMyProxy()).getValue());

    barrier.barrier();
  }

  private void multipleInterfacesProxyTest(int index) throws Throwable {
    if (index == 0) {
      MyInvocationHandler handler = new MyInvocationHandler();
      Proxy myProxy = (Proxy) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { MyProxyInf1.class,
          MyProxyInf2.class }, handler);
      ((MyProxyInf1) myProxy).setValue(2000);
      ((MyProxyInf2) myProxy).setStringValue("Testing");
      dataRoot.setMyProxy(myProxy);
    }

    barrier.barrier();

    Assert.assertTrue(dataRoot.getMyProxy() instanceof MyProxyInf1);
    Assert.assertTrue(dataRoot.getMyProxy() instanceof MyProxyInf2);

    Assert.assertEquals(2000, ((MyProxyInf1) dataRoot.getMyProxy()).getValue());
    Assert.assertEquals("Testing", ((MyProxyInf2) dataRoot.getMyProxy()).getStringValue());

    barrier.barrier();
  }

  private void differentLoaderTest(int index) throws Throwable {
    if (index == 0) {
      MyMapInvocationHandler handler = new MyMapInvocationHandler();

      Assert.assertFalse(this.getClass().getClassLoader() == Map.class.getClassLoader());

      Object myProxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { Map.class }, handler);

      ((Map) myProxy).put("key1", "value1");
      ((Map) myProxy).put("key2", "value2");

      dataRoot.setMyProxy(myProxy);

    }

    barrier.barrier();

    Assert.assertTrue(dataRoot.getMyProxy().getClass().getClassLoader() instanceof IsolationClassLoader);
    Assert.assertEquals(2, ((Map) dataRoot.getMyProxy()).size());
    Assert.assertEquals("value1", ((Map) dataRoot.getMyProxy()).get("key1"));
    Assert.assertEquals("value2", ((Map) dataRoot.getMyProxy()).get("key2"));

    barrier.barrier();
  }

  private void subclassProxyTest(int index) throws Throwable {
    if (index == 0) {
      MyInvocationHandler handler = new MyInvocationHandler();
      MyProxyInf1 myProxy = new MySubProxy(handler);

      myProxy.setValue(137);
      dataRoot.setMyProxy(myProxy);
    }

    barrier.barrier();

    Assert.assertFalse(Proxy.isProxyClass(dataRoot.getMyProxy().getClass()));

    Assert.assertEquals(-1, ((MyProxyInf1) dataRoot.getMyProxy()).getValue());

    barrier.barrier();
  }

  private void proxySystemInterface(int index) throws Throwable {
    if (index == 0) {
      MyInvocationHandler handler = new MyInvocationHandler();
      Collection colProxyImpl = (Collection) Proxy.newProxyInstance(System.class.getClassLoader(),
                                                                    new Class[] { Collection.class }, handler);
      Assert.assertNull(colProxyImpl.getClass().getClassLoader());
    }
    barrier.barrier();
  }

  private void validProxyTest(int index) throws Throwable {
    if (index == 0) {
      MyInvocationHandler handler = new MyInvocationHandler();
      proxyImpl = (MyProxyInf1) Proxy.newProxyInstance(MyProxyInf1.class.getClassLoader(),
                                                       new Class[] { MyProxyInf1.class }, handler);

    }

    barrier.barrier();

    if (index == 1) {
      synchronized (proxyImpl) {
        proxyImpl.setValue(123);
      }
    }

    barrier.barrier();

    Assert.assertTrue(proxyImpl.getClass().getClassLoader() instanceof IsolationClassLoader);
    Assert.assertEquals(123, proxyImpl.getValue());

    barrier.barrier();

    if (index == 0) {
      MyInvocationHandler handler = new MyInvocationHandler();
      MyProxyInf1 myProxy = (MyProxyInf1) Proxy.newProxyInstance(MyProxyInf1.class.getClassLoader(),
                                                                 new Class[] { MyProxyInf1.class }, handler);
      myProxy.setValue(137);
      dataRoot.setMyProxy(myProxy);
    }

    barrier.barrier();

    Assert.assertEquals(137, ((MyProxyInf1) dataRoot.getMyProxy()).getValue());

    barrier.barrier();

    if (index == 1) {
      dataRoot.setMyProxy(proxyImpl);
    }

    barrier.barrier();

    Assert.assertTrue(dataRoot.getMyProxy() == proxyImpl);
    Assert.assertEquals(123, ((MyProxyInf1) dataRoot.getMyProxy()).getValue());

    barrier.barrier();

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = ProxyTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$DataRoot");
    config.addIncludePattern(testClass + "$MySubProxy");
    config.addIncludePattern(testClass + "$MyInvocationHandler");
    config.addIncludePattern(testClass + "$MyMapInvocationHandler");
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("proxyImpl", "proxyImpl");
    spec.addRoot("dataRoot", "dataRoot");
    spec.addRoot("mapRoot", "mapRoot");
  }

  private static class MySubProxy extends Proxy implements MyProxyInf1 {
    public MySubProxy(InvocationHandler h) {
      super(h);
    }

    public int getValue() {
      return -1;
    }

    public void setValue(int i) {
      //
    }
  }

  public interface MyProxySuperIntf {
    public void setSuperIntfValue();
  }

  public interface MyProxyInf1 {
    public int getValue();

    public void setValue(int i);
  }

  public interface MyProxyInf2 extends MyProxySuperIntf {
    public String getStringValue();

    public void setStringValue(String str);
  }

  public static class MyInvocationHandler implements InvocationHandler {
    private final Map values       = new HashMap();
    private final Map stringValues = new HashMap();

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("getValue")) {
        return values.get(proxy);
      } else if (method.getName().equals("setValue")) {
        values.put(proxy, args[0]);
        return null;
      } else if (method.getName().equals("setStringValue")) {
        stringValues.put(proxy, args[0]);
        return null;
      } else if (method.getName().equals("getStringValue")) {
        return stringValues.get(proxy);
      } else if (method.getName().equals("hashCode")) { return Integer.valueOf(System.identityHashCode(proxy)); }
      return null;
    }
  }

  private static class MyMapInvocationHandler implements InvocationHandler {
    private final Map values = new HashMap();

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("get")) {
        return values.get(args[0]);
      } else if (method.getName().equals("put")) {
        values.put(args[0], args[1]);
        return null;
      } else if (method.getName().equals("size")) {
        return Integer.valueOf(values.size());
      } else if (method.getName().equals("hashCode")) { return Integer.valueOf(System.identityHashCode(proxy)); }
      return null;
    }
  }

  private static class DataRoot {
    private Object myProxy;

    public DataRoot() {
      super();
    }

    public Object getMyProxy() {
      return myProxy;
    }

    public synchronized void setMyProxy(Object myProxy) {
      this.myProxy = myProxy;
    }
  }

  public static class MyProxyClassLoader extends URLClassLoader {
    private static final ClassLoader SYSTEM_LOADER = ClassLoader.getSystemClassLoader();

    public MyProxyClassLoader() {
      super(getSystemURLS(), null);
    }

    private static URL[] getSystemURLS() {
      return ((URLClassLoader) SYSTEM_LOADER).getURLs();
    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
      return super.loadClass(name);
    }

  }
}
