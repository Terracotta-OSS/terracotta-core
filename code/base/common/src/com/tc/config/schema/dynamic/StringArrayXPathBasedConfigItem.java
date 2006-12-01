/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.impl.common.XPath;

import com.tc.config.schema.context.ConfigContext;
import com.tc.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * An {@link XPathBasedConfigItem} that returns a {@link String} array. The {@link XPath} should point to the top-level
 * element (i.e., the one that has child elements that contain nothing but {@link String}s).
 */
public class StringArrayXPathBasedConfigItem extends XPathBasedConfigItem implements StringArrayConfigItem {

  public StringArrayXPathBasedConfigItem(ConfigContext context, String xpath) {
    super(context, xpath);
  }

  protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
    if (xmlObject == null) return null;
    
    // This is a little tricky; the name of the method that returns String[] can be variable. However, there seems to be
    // only one such method declared in the class itself, so we can find it by reflection. If this assumption ever
    // proves to be false, we'll need to re-visit this logic.
    Method targetMethod = null;
    Method[] allMethods = xmlObject.getClass().getMethods();

    for (int i = 0; i < allMethods.length; ++i) {
      Method candidate = allMethods[i];
      if (candidate.getReturnType().equals(String[].class) && candidate.getParameterTypes().length == 0
          && Modifier.isPublic(candidate.getModifiers()) && candidate.getName().startsWith("get")
          && candidate.getName().endsWith("Array")) {
        if (targetMethod != null) {
          // formatting
          throw Assert
              .failure("Whoa! There are multiple public methods that start with 'get', end with 'Array', take no parameters, "
                       + "and return String[] on class "
                       + xmlObject.getClass().getName()
                       + ". One is "
                       + targetMethod
                       + ", and another is " + candidate + ". We should fix " + "the program to account for this.");
        }

        targetMethod = candidate;
      }
    }

    if (targetMethod == null) {
      // formatting
      throw Assert.failure("Class " + xmlObject.getClass().getName() + " has no public methods that start with 'get', "
                           + "end with 'Array', take no parameters, and return String[].");
    }

    try {
      return targetMethod.invoke(xmlObject, new Object[0]);
    } catch (IllegalArgumentException iae) {
      throw Assert.failure("Couldn't invoke method " + targetMethod + " on object " + xmlObject + ": ", iae);
    } catch (IllegalAccessException iae) {
      throw Assert.failure("Couldn't invoke method " + targetMethod + " on object " + xmlObject + ": ", iae);
    } catch (InvocationTargetException ite) {
      throw Assert.failure("Couldn't invoke method " + targetMethod + " on object " + xmlObject + ": ", ite);
    }
  }

  public String[] getStringArray() {
    return (String[]) getObject();
  }

}
