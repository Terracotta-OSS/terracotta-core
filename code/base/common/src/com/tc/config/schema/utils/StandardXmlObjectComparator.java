/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.utils;

import org.apache.xmlbeans.XmlObject;

import com.tc.util.Assert;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The standard implementation of {@link XmlObjectComparator}.
 */
public class StandardXmlObjectComparator implements XmlObjectComparator {

  public boolean equals(XmlObject one, XmlObject two) {
    try {
      checkEquals(one, two);
      return true;
    } catch (NotEqualException nee) {
      return false;
    }
  }

  public void checkEquals(XmlObject one, XmlObject two) throws NotEqualException {
    checkEquals(one, two, "");
  }

  private void checkEquals(XmlObject one, XmlObject two, String where) throws NotEqualException {
    Assert.assertNotNull(where);

    if ((one == null) != (two == null)) {
      // formatting
      throw new NotEqualException(where + ": Objects are not both null or not both non-null.");
    }

    if (one == null) return;

    Class oneInterface = getBeanInterface(one);
    Class twoInterface = getBeanInterface(two);

    if (!oneInterface.equals(twoInterface)) {
      // formatting
      throw new NotEqualException(where + ": Bean interface for " + one + " is " + oneInterface
                                  + ", and bean interface for two is " + twoInterface + ".");
    }

    Method[] methods = fetchAndFilterMethods(oneInterface);
    Assert.eval(methods.length > 0);

    for (int i = 0; i < methods.length; ++i) {
      Method method = methods[i];

      String thisWhere = where + "/" + getPropertyFromMethodName(method.getName());

      try {
        Object oneValue = method.invoke(one);
        Object twoValue = method.invoke(two);

        compareValues(thisWhere, oneValue, twoValue);
      } catch (IllegalArgumentException iae) {
        throw Assert.failure(thisWhere + ": Unable to fetch property from a bean; method " + method + " failed.", iae);
      } catch (IllegalAccessException iae) {
        throw Assert.failure(thisWhere + ": Unable to fetch property from a bean; method " + method + " failed.", iae);
      } catch (InvocationTargetException ite) {
        throw Assert.failure(thisWhere + ": Unable to fetch property from a bean; method " + method + " failed.", ite);
      }
    }
  }

  private void compareValues(String thisWhere, Object oneValue, Object twoValue) throws NotEqualException {
    if ((oneValue == null) != (twoValue == null)) {
      // formatting
      throw new NotEqualException(thisWhere + ": First value " + (oneValue == null ? "is" : "isn't") + " null, "
                                  + "but second value " + (twoValue == null ? "is" : "isn't."));
    }

    if (oneValue != null) {
      if ((oneValue instanceof XmlObject) && (twoValue instanceof XmlObject)) {
        checkEquals((XmlObject) oneValue, (XmlObject) twoValue, thisWhere);
      } else if ((oneValue instanceof XmlObject) || (twoValue instanceof XmlObject)) {
        throw new NotEqualException(thisWhere + ": One value is an XmlObject and the other isn't; value one is "
                                    + oneValue + ", and value two is " + twoValue);
      } else if (oneValue.getClass().isArray() && twoValue.getClass().isArray()) {
        if (Array.getLength(oneValue) != Array.getLength(twoValue)) {
          // formatting
          throw new NotEqualException(thisWhere + ": Value one is an array of length " + Array.getLength(oneValue)
                                      + ", and value two " + "is an array of length " + Array.getLength(twoValue));
        }

        int length = Array.getLength(oneValue);
        for (int j = 0; j < length; ++j) {
          compareValues(thisWhere + "[" + j + "]", Array.get(oneValue, j), Array.get(twoValue, j));
        }
      } else {
        if (!oneValue.equals(twoValue)) {
          // formatting
          throw new NotEqualException(
                                      thisWhere
                                          + ": Neither value is an XmlObject, and Object.equals() didn't return true; value one is '"
                                          + oneValue + "', and value two is '" + twoValue + "'.");
        }
      }
    }
  }

  private static String getPropertyFromMethodName(String methodName) {
    Assert.assertNotBlank(methodName);
    Assert.eval(methodName.length() >= "get".length());

    return methodName.substring("get".length(), "get".length() + 1).toLowerCase()
           + methodName.substring("get".length() + 1);
  }

  private static Class getBeanInterface(Object value) {
    Class[] interfaces = value.getClass().getInterfaces();

    if (interfaces.length != 1) {
      // formatting
      throw Assert.failure("Class " + value.getClass() + ", the class of object " + value + ", implements "
                           + interfaces.length + " interfaces, not 1. We don't support this yet.");
    }
    return interfaces[0];
  }

  private static Method[] fetchAndFilterMethods(Class theClass) {
    Method[] allMethods = theClass.getDeclaredMethods();
    List out = new ArrayList();

    for (int i = 0; i < allMethods.length; ++i) {
      Method method = allMethods[i];

      if (method.getParameterTypes().length == 0 && method.getName().startsWith("get")) out.add(method);
    }

    return (Method[]) out.toArray(new Method[out.size()]);
  }

}
