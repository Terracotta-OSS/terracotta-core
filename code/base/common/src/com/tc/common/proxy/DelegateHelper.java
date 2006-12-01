/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.common.proxy;

import java.lang.reflect.Proxy;

/**
 * @author andrew A helper for easily creating overridden delegates for classes.
 */
public class DelegateHelper {

  /**
   * Creates a delegate object. The object returned will be an instance of the given interfaces. Any calls to the
   * returned object will simply call through to the passed-in delegate.
   * </p>
   * <p>
   * So why use this method? Well, delegate does <em>not</em> have to implement <em>any</em> of the given interface.
   * Any methods on the interface that are not implemented in the delegate will simply throw a {@link NoSuchMethodError}
   * if called. This is useful for many things &mdash; most especially, mock-object generation for tests.
   */
  public static Object createDelegate(Class[] theInterfaces, Object delegate) {
    return Proxy.newProxyInstance(DelegateHelper.class.getClassLoader(), theInterfaces,
                                  new GenericInvocationHandler(delegate));
  }

  public static Object createDelegate(Class theInterface, Object delegate) {
    return createDelegate(new Class[] { theInterface }, delegate);
  }

  /**
   * Creates a delegate object. The object returned will be an instance of the given interfaces; by default, it will
   * simply call through to the delegate object. However, any calls to methods of any of the interfaces that are also
   * defined in the overrider get sent there, instead.
   * </p>
   * <p>
   * Note that neither the delegate nor the overrider need comply to <em>any</em> of the given interfaces; if a method
   * in one of the interfaces is defined in neither the handler nor the delegate, you'll get a {@link NoSuchMethodError}
   * if you try to call it.
   */
  public static Object createDelegate(Class[] theInterfaces, Object delegate, Object overrider) {
    return Proxy.newProxyInstance(DelegateHelper.class.getClassLoader(), theInterfaces,
                                  new DelegatingInvocationHandler(delegate, overrider));
  }

  public static Object createDelegate(Class theInterface, Object delegate, Object overrider) {
    return createDelegate(new Class[] { theInterface }, delegate, overrider);
  }
  
}