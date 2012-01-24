/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.SystemConfigObject;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.object.config.schema.L1DSOConfigObject;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.terracottatech.config.Authentication;
import com.terracottatech.config.HttpAuthentication;
import com.terracottatech.config.License;
import com.terracottatech.config.Offheap;
import com.terracottatech.config.TcConfigDocument.TcConfig;
import com.terracottatech.config.TcProperties;

import java.io.File;
import java.lang.reflect.Method;

public class TcConfigDefaultInitializationTest extends TCTestCase {
  private static Class[] exemptedElements = { License.class, TcProperties.class, Authentication.class,
      HttpAuthentication.class, Offheap.class };
  private TcConfig       config;

  @Override
  protected void setUp() throws Exception {
    this.config = TcConfig.Factory.newInstance();
    SchemaDefaultValueProvider defaultValueProvider = new SchemaDefaultValueProvider();
    L2DSOConfigObject.initializeServers(this.config, new SchemaDefaultValueProvider(), new File("tmp"));
    SystemConfigObject.initializeSystem(this.config, defaultValueProvider);
    L1DSOConfigObject.initializeClients(this.config, defaultValueProvider);
    config.getServers().getMirrorGroups().getMirrorGroupArray(0).setGroupName("test-group");
  }

  public void testDefaultInitialization() throws Exception {
    testAllInitialized("TcConfig", config);
    testAllArrayInitialized("TcConfig", config);
  }

  private void testAllArrayInitialized(String testingFor, XmlObject xmlObject) throws Exception {
    System.out.println("testsing array initialized for " + testingFor + "....");

    Method[] methods = xmlObject.getClass().getMethods();

    for (Method method : methods) {
      if (method.getName().endsWith("Array") && method.getName().startsWith("get")) {
        Object arrayElements = fetchDataFromXmlObjectByReflection(xmlObject, method.getName());
        Assert.assertTrue("array of type: " + arrayElements.getClass().getSimpleName(), arrayElements.getClass()
            .isArray());

        Assert.assertTrue("array of type: " + arrayElements.getClass().getSimpleName(),
                          ((Object[]) arrayElements).length > 0);
        Object[] childern = (Object[]) arrayElements;
        for (Object child : childern) {
          if (child instanceof XmlObject) {
            String testFor = method.getName().replace("get", "");
            testFor.replace("Array", "");
            testAllInitialized(testFor, (XmlObject) child);
            testAllArrayInitialized(testFor, (XmlObject) child);
          }
        }
      }
    }
  }

  private void testAllInitialized(String testingFor, XmlObject xmlObject) throws Exception {
    System.out.println("testing all initialized for " + testingFor + "....");
    Method[] methods = xmlObject.getClass().getMethods();

    for (Method method : methods) {
      if (method.getName().startsWith("isSet")) {
        Class returnTypeOfGetMethod = findReturnTypeOfGet(xmlObject, method.getName());
        if (isExmpted(returnTypeOfGetMethod)) continue;
        Boolean isSet = (Boolean) method.invoke(xmlObject, new Object[0]);
        Assert.assertTrue("method: " + method.getName(), isSet);
        System.out.println(method.getName() + ": true");

        Object child = fetchDataFromXmlObjectByReflection(xmlObject, method.getName());

        // if child is of complex type
        if (child instanceof XmlObject) {
          String testFor = method.getName().replace("isSet", "");
          testAllInitialized(testFor, (XmlObject) child);
          testAllArrayInitialized(testFor, (XmlObject) child);
        }
      }
    }

  }

  private Class findReturnTypeOfGet(XmlObject xmlObject, String isSetMethodName) {
    return findGetMethod(xmlObject, isSetMethodName).getReturnType();
  }

  private Object fetchDataFromXmlObjectByReflection(XmlObject xmlObject, String methodName) {
    Method method = findGetMethod(xmlObject, methodName);
    try {
      return method.invoke(xmlObject, new Object[0]);
    } catch (Exception e) {
      System.out.println("Exception in invoking methdod " + method.getName() + " on " + xmlObject);
      throw new AssertionError(e);
    }
  }

  private Method findGetMethod(XmlObject xmlObject, String methodName) {
    String getMethod = methodName.replace("isSet", "get");
    Method[] methods = xmlObject.getClass().getMethods();

    for (Method method : methods) {
      if (method.getName().equals(getMethod) && method.getParameterTypes().length == 0) { return method; }
    }

    throw new AssertionError("can't get the method with method name : " + getMethod);
  }

  private static boolean isExmpted(Class clazz) {
    for (Class exemptedElement : exemptedElements) {
      if (exemptedElement.equals(clazz)) return true;
    }

    return false;
  }
}
