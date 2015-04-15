/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link InvocationHandler}that logs every call made through it, along with the thread that made it. This can be
 * very useful for debugging in certain situations.
 * </p>
 * <p>
 * This class will also usefully create proxy wrappers around any objects returned from any calls that implement any of
 * the interfaces supplied in the <code>furtherProxies</code> argument to the constructor. This allows you to
 * 'capture' an entire object graph resulting from a series of calls pretty easily.
 */
public class LoggingInvocationHandler implements InvocationHandler {

  private final Object  delegate;
  private final Writer  dest;
  private final Class[] furtherProxies;

  public LoggingInvocationHandler(Object delegate, Writer dest, Class[] furtherProxies) {
    Assert.assertNotNull(delegate);
    Assert.assertNotNull(dest);
    Assert.assertNoNullElements(furtherProxies);
    this.delegate = delegate;
    this.dest = dest;
    this.furtherProxies = furtherProxies;
  }

  @Override
  public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable {
    Thread theThread = Thread.currentThread();
    StringBuffer message = new StringBuffer();

    message.append("[" + theThread.getName() + "]: " + delegate + "." + arg1 + "(" + describeArguments(arg2) + ")");
    try {
      Object result = arg1.invoke(delegate, arg2);
      message.append(" ==> " + result);
      process(message.toString());
      return reproxify(result);
    } catch (InvocationTargetException ite) {
      message.append(" => THROWABLE: ");
      message.append(ExceptionUtils.getStackTrace(ite.getCause()));
      message.append(".");
      process(message.toString());
      throw ite.getCause();
    }
  }

  private Object reproxify(Object out) {
    if (out == null) return out;
    if (Proxy.isProxyClass(out.getClass())) return out;

    List implementedClasses = new ArrayList();

    for (int i = 0; i < furtherProxies.length; ++i) {
      if (furtherProxies[i].isInstance(out)) {
        implementedClasses.add(furtherProxies[i]);
      }
    }

    if (implementedClasses.size() > 0) {
      return Proxy.newProxyInstance(getClass().getClassLoader(), (Class[]) implementedClasses
          .toArray(new Class[implementedClasses.size()]), new LoggingInvocationHandler(out, this.dest,
                                                                                       this.furtherProxies));
    } else {
      return out;
    }
  }

  private String describeArguments(Object[] arguments) {
    if (arguments == null) return "";

    StringBuffer out = new StringBuffer();

    for (int i = 0; i < arguments.length; ++i) {
      if (i > 0) out.append(", ");
      out.append(arguments[i]);
    }

    return out.toString();
  }

  private void process(String message) {
    try {
      dest.write("\n\n" + message + "\n");
      dest.flush();
    } catch (IOException ioe) {
      throw Assert.failure("Got an IOException when writing.", ioe);
    }
  }

}
