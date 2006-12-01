/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.impl.values.XmlObjectBase;

import com.tc.config.schema.dynamic.BooleanConfigItem;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.ConfigItemListener;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.dynamic.IntConfigItem;
import com.tc.config.schema.dynamic.ObjectArrayConfigItem;
import com.tc.config.schema.dynamic.StringArrayConfigItem;
import com.tc.config.schema.dynamic.StringConfigItem;
import com.tc.config.schema.dynamic.XPathBasedConfigItem;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * This class exists to avoid the massive pain of writing fully-compliant test versions of every single config object in
 * the system. This is used by the {@link com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory}; see
 * there for more details.
 * </p>
 * <p>
 * Basically, you can use this with the {@link java.util.Proxy} stuff and any random config interface (more
 * specifically, one that has methods that all take no parameters and return instances of {@link ConfigItem}, or
 * subinterfaces thereof), and you'll get back a config object that's special: when you call the normal methods that
 * return config items, you can cast the returned item to a {@link com.tc.config.schema.SettableConfigItem}, and that
 * will let you set its value. Note that doing anything else with the returned item, like trying to get its value or add
 * or remove listeners, will just reward you with a {@link com.tc.util.TCAssertionError}.
 * </p>
 * <p>
 * This class is messy (I am in a ridiculous rush right now to get this code down). The vast majority of its code is
 * involved in taking a root XML bean and an XPath, and then descending through child beans &mdash; creating them along
 * the way, if necessary &mdash; to
 */
public class TestConfigObjectInvocationHandler implements InvocationHandler {

  private static final TCLogger logger = TCLogging.getLogger(TestConfigObjectInvocationHandler.class);

  /**
   * This is the {@link ConfigItem} implementation we return. Feel free to add interfaces to this list as we create more
   * typed subinterfaces of {@link ConfigItem}; the methods shouldn't do anything, just throw
   * {@link com.tc.util.TCAssertionError}s.
   */
  private class OurSettableConfigItem implements SettableConfigItem, BooleanConfigItem, FileConfigItem, IntConfigItem,
      ObjectArrayConfigItem, StringArrayConfigItem, StringConfigItem {
    private final String xpath;

    public OurSettableConfigItem(String xpath) {
      this.xpath = xpath;
    }

    public synchronized void addListener(ConfigItemListener changeListener) {
      throw Assert.failure("You can only set values on these items; you shouldn't actually be using them.");
    }

    public synchronized Object getObject() {
      throw Assert.failure("You can only set values on these items; you shouldn't actually be using them.");
    }

    public synchronized void removeListener(ConfigItemListener changeListener) {
      throw Assert.failure("You can only set values on these items; you shouldn't actually be using them.");
    }

    public synchronized void setValue(Object newValue) {
      for (int i = 0; i < beansToSetOn.length; ++i) {
        logger.debug("Setting value " + newValue + " on XPath '" + xpath + "' for bean " + beansToSetOn[i]);
        setValueOnBean(newValue, beansToSetOn[i], this.xpath);
      }
    }

    private String toMethodName(String xpathComponent) {
      while (xpathComponent.startsWith("@"))
        xpathComponent = xpathComponent.substring(1);

      StringBuffer out = new StringBuffer();
      char[] in = xpathComponent.toCharArray();
      boolean capitalizeNext = true;

      for (int i = 0; i < in.length; ++i) {
        char ch = in[i];
        if (ch == '-') capitalizeNext = true;
        else {
          out.append(capitalizeNext ? Character.toUpperCase(ch) : ch);
          capitalizeNext = false;
        }
      }

      return out.toString();
    }

    private void setValueOnBean(Object newValue, XmlObject beanToSetOn, String theXPath) {
      synchronized (beanToSetOn) {
        if (StringUtils.isBlank(theXPath)) {
          logger.debug("Setting value " + newValue + " directly on bean " + beanToSetOn + ".");
          setValueDirectly(newValue, beanToSetOn);
        } else {
          String remainingComponents = allButFirstComponentOf(theXPath);
          String component = firstComponentOf(theXPath);

          if (StringUtils.isBlank(remainingComponents)) {
            // Special case, look for set...() method or xset...() method that takes an instance of our value type
            if (setValueAsPropertyDirectly(beanToSetOn, component, newValue, "set")) return;
            if (setValueAsPropertyDirectly(beanToSetOn, component, newValue, "xset")) return;
          }

          logger.debug("Looking for property '" + component + "' of bean " + beanToSetOn + ".");
          XmlObject[] children = beanToSetOn.selectPath(component);

          if (children == null || children.length == 0) {
            // Create a new one
            logger.debug("Property '" + component + "' doesn't exist; creating it.");
            XmlObject newChild = createAndAddNewChild(beanToSetOn, component);
            children = new XmlObject[] { newChild };
          }

          for (int i = 0; i < children.length; ++i) {
            logger.debug("Setting value " + newValue + " on child " + (i + 1) + " of " + children.length + ".");
            setValueOnBean(newValue, children[i], remainingComponents);
          }
        }
      }
    }

    private XmlObject createAndAddNewChild(XmlObject beanToSetOn, String name) {
      String asPropertyName = toMethodName(name);
      Class beanClass = beanToSetOn.getClass();
      Method creatorMethod = null;
      String creatorMethodName = "addNew" + asPropertyName;

      try {
        logger.debug("Trying to create new child for property '" + name + "' on bean " + beanToSetOn + " of "
                     + beanClass + ".");
        creatorMethod = beanClass.getMethod(creatorMethodName, new Class[0]);
        logger.debug("Found creator method " + creatorMethod);
        Assert.eval(XmlObject.class.isAssignableFrom(creatorMethod.getReturnType()));
        return (XmlObject) creatorMethod.invoke(beanToSetOn, new Object[0]);
      } catch (NoSuchMethodException nsme) {
        // leave it, try setting instead
        logger.debug("Didn't find creator method named '" + creatorMethodName + "'");
      } catch (IllegalArgumentException iae) {
        throw Assert.failure("Can't invoke creator method " + creatorMethod, iae);
      } catch (IllegalAccessException iae) {
        throw Assert.failure("Can't invoke creator method " + creatorMethod, iae);
      } catch (InvocationTargetException ite) {
        throw Assert.failure("Can't invoke creator method " + creatorMethod, ite);
      }

      Method[] allMethods = beanClass.getMethods();
      Method xsetMethod = null;
      String methodName = "xset" + asPropertyName;
      String factoryClassName = null;

      try {
        logger.debug("Looking for method named '" + methodName + "' instead.");
        for (int i = 0; i < allMethods.length; ++i) {
          Method thisMethod = allMethods[i];
          if (thisMethod.getName().equals(methodName) && thisMethod.getReturnType().equals(Void.TYPE)
              && thisMethod.getParameterTypes().length == 1
              && XmlObject.class.isAssignableFrom(thisMethod.getParameterTypes()[0])) {
            if (xsetMethod != null) {
              // formatting
              throw Assert.failure("There are multiple '" + methodName + "' methods in " + beanClass + "; one is "
                                   + xsetMethod + ", and another is " + thisMethod + ".");
            }
            xsetMethod = thisMethod;
          }
        }

        if (xsetMethod == null) {
          // formatting
          throw Assert.failure("There is no method named '" + methodName + "' in " + beanClass
                               + ". If this method does exist, but on a subclass of " + beanClass
                               + ", not on it itself, you may have specified the wrong XPath prefix when you created "
                               + "this invocation handler.");
        }
        logger.debug("Found xset...() method: " + xsetMethod);

        Class newObjectClass = xsetMethod.getParameterTypes()[0];
        factoryClassName = newObjectClass.getName() + ".Factory";
        logger.debug("Creating a new instance of " + newObjectClass + " using factory class '" + factoryClassName
                     + "'.");
        Class factoryClass = Class.forName(factoryClassName);
        Method createMethod = factoryClass.getMethod("newInstance", new Class[0]);
        Assert.eval(XmlObject.class.isAssignableFrom(createMethod.getReturnType()));
        Assert.eval(Modifier.isStatic(createMethod.getModifiers()));
        XmlObject out = (XmlObject) createMethod.invoke(null, null);
        logger.debug("Created new object " + out + ".");
        xsetMethod.invoke(beanToSetOn, new Object[] { out });
        logger.debug("Set new object " + out + " via xset...() method " + xsetMethod + " on bean " + beanToSetOn);
        return out;
      } catch (ClassNotFoundException cnfe) {
        throw Assert.failure("No factory class '" + factoryClassName + "'?", cnfe);
      } catch (IllegalArgumentException iae) {
        throw Assert.failure("Can't create instance using factory " + factoryClassName + " and assign using method "
                             + xsetMethod + "?", iae);
      } catch (IllegalAccessException iae) {
        throw Assert.failure("Can't create instance using factory " + factoryClassName + " and assign using method "
                             + xsetMethod + "?", iae);
      } catch (InvocationTargetException ite) {
        throw Assert.failure("Can't create instance using factory " + factoryClassName + " and assign using method "
                             + xsetMethod + "?", ite);
      } catch (SecurityException se) {
        throw Assert.failure("Can't create instance using factory " + factoryClassName + " and assign using method "
                             + xsetMethod + "?", se);
      } catch (NoSuchMethodException nsme) {
        throw Assert.failure("Can't create instance using factory " + factoryClassName + " and assign using method "
                             + xsetMethod + "?", nsme);
      }
    }

    private boolean assignmentCompatible(Class parameterType, Object actualObject) {
      if (parameterType.isPrimitive()) {
        if (parameterType.equals(Boolean.TYPE)) return actualObject instanceof Boolean;
        if (parameterType.equals(Character.TYPE)) return actualObject instanceof Character;
        if (parameterType.equals(Byte.TYPE)) return actualObject instanceof Byte;
        if (parameterType.equals(Short.TYPE)) return actualObject instanceof Short;
        if (parameterType.equals(Integer.TYPE)) return actualObject instanceof Integer;
        if (parameterType.equals(Long.TYPE)) return actualObject instanceof Long;
        if (parameterType.equals(Float.TYPE)) return actualObject instanceof Float;
        if (parameterType.equals(Double.TYPE)) return actualObject instanceof Double;
        throw Assert.failure("Unknown primitive type " + parameterType);
      } else {
        return actualObject == null || parameterType.isInstance(actualObject);
      }
    }

    private boolean setValueAsPropertyDirectly(XmlObject onBean, String propertyName, Object newValue,
                                               String prefixToTry) {
      Method[] allMethods = onBean.getClass().getMethods();
      String methodName = prefixToTry + toMethodName(propertyName);
      Method setterMethod = null;

      logger.debug("Looking for '" + methodName + "' method on " + onBean.getClass() + "...");
      for (int i = 0; i < allMethods.length; ++i) {
        if (allMethods[i].getName().equals(methodName) && allMethods[i].getParameterTypes().length == 1) {
          if (setterMethod != null) {
            // formatting
            logger.debug("There are multiple '" + methodName + "' methods in " + onBean.getClass() + "; one is "
                         + setterMethod + ", and another is " + allMethods[i] + ". Returning false.");
            return false;
          }

          setterMethod = allMethods[i];
        }
      }

      if (setterMethod == null) {
        logger.debug("Can't find '" + methodName + "' method with one argument on " + onBean.getClass()
                     + "; returning false.");
        return false;
      }

      if (newValue == null && setterMethod.getParameterTypes()[0].isPrimitive()) {
        // formatting
        logger.debug("Unable to set null as a value on XML bean " + onBean + "; it requires a(n) "
                     + setterMethod.getParameterTypes()[0] + ", which is a primitive type. Returning false.");
        return false;
      }

      if (newValue != null && (!assignmentCompatible(setterMethod.getParameterTypes()[0], newValue))) {
        // formatting
        logger.debug("Unable to set value " + newValue + " on XML bean " + onBean + " via method " + setterMethod
                     + "; it requires an object of " + setterMethod.getParameterTypes()[0] + ", and this value is of "
                     + newValue.getClass() + " instead. Returning false.");
        return false;
      }

      try {
        logger.debug("Setting value " + newValue + " on " + onBean + " via method " + setterMethod + ".");
        setterMethod.invoke(onBean, new Object[] { newValue });
        return true;
      } catch (IllegalArgumentException iae) {
        throw Assert.failure("Unable to invoke " + setterMethod, iae);
      } catch (IllegalAccessException iae) {
        throw Assert.failure("Unable to invoke " + setterMethod, iae);
      } catch (InvocationTargetException ite) {
        throw Assert.failure("Unable to invoke " + setterMethod, ite);
      }
    }

    private void setValueDirectly(Object newValue, XmlObject onBean) {
      Method[] allMethods = onBean.getClass().getMethods();
      Method setterMethod = null;
      boolean isArray = newValue != null && newValue.getClass().isArray();
      String searchDescrip = isArray ? "set...Array(...[])" : "set(...)";

      logger.debug("Looking for " + searchDescrip + " method on " + onBean.getClass() + "...");
      for (int i = 0; i < allMethods.length; ++i) {
        if (isArray) {
          if (allMethods[i].getName().startsWith("set") && allMethods[i].getName().endsWith("Array")
              && allMethods[i].getParameterTypes().length == 1 && allMethods[i].getParameterTypes()[0].isArray()) {
            if (setterMethod != null) {
              // formatting
              throw Assert.failure("There are multiple '" + searchDescrip + "' methods in " + onBean.getClass()
                                   + "; one is " + setterMethod + ", and another is " + allMethods[i]);
            }

            setterMethod = allMethods[i];
          }
        } else {
          Class declaringClass = allMethods[i].getDeclaringClass();
          if (declaringClass.equals(XmlObject.class) || declaringClass.equals(XmlObjectBase.class)) continue;

          if (allMethods[i].getName().equals("set") && allMethods[i].getParameterTypes().length == 1) {
            if (setterMethod != null) {
              // formatting
              throw Assert.failure("There are multiple '" + searchDescrip + "' methods in " + onBean.getClass()
                                   + "; one is " + setterMethod + ", and another is " + allMethods[i]);
            }

            setterMethod = allMethods[i];
          }
        }
      }

      if (setterMethod == null) throw Assert.failure("Can't find '" + searchDescrip + "' method with one argument on "
                                                     + onBean.getClass() + ".");

      if (newValue == null && setterMethod.getParameterTypes()[0].isPrimitive()) {
        // formatting
        throw Assert.failure("You can't set null as a value on XML bean " + onBean + "; it requires a(n) "
                             + setterMethod.getParameterTypes()[0] + ", which is a primitive type.");
      }

      if (newValue != null && (!setterMethod.getParameterTypes()[0].isInstance(newValue))) {
        // formatting
        throw Assert.failure("You can't set value " + newValue + " on XML bean " + onBean + " via method "
                             + setterMethod + "; it requires an object of " + setterMethod.getParameterTypes()[0]
                             + ", and your value is of " + newValue.getClass() + " instead.");
      }

      try {
        logger.debug("Setting value " + newValue + " on " + onBean + " via method " + setterMethod + ".");
        setterMethod.invoke(onBean, new Object[] { newValue });
      } catch (IllegalArgumentException iae) {
        throw Assert.failure("Unable to invoke " + setterMethod, iae);
      } catch (IllegalAccessException iae) {
        throw Assert.failure("Unable to invoke " + setterMethod, iae);
      } catch (InvocationTargetException ite) {
        throw Assert.failure("Unable to invoke " + setterMethod, ite);
      }
    }

    private String firstComponentOf(String theXPath) {
      while (theXPath.startsWith("/"))
        theXPath = theXPath.substring(1);
      int index = theXPath.indexOf("/");
      if (index < 0) return theXPath;
      Assert.eval(index > 0);
      String out = theXPath.substring(0, index);
      while (out.startsWith("@"))
        out = out.substring(1);
      return out;
    }

    private String allButFirstComponentOf(String theXPath) {
      while (theXPath.startsWith("/"))
        theXPath = theXPath.substring(1);
      int index = theXPath.indexOf("/");
      if (index < 0) return null;
      Assert.eval(index > 0);
      return theXPath.substring(index + 1);
    }

    public void setValue(boolean newValue) {
      setValue(new Boolean(newValue));
    }

    public void setValue(int newValue) {
      setValue(new Integer(newValue));
    }

    public boolean getBoolean() {
      return ((Boolean) getObject()).booleanValue();
    }

    public String getString() {
      return (String) getObject();
    }

    public String[] getStringArray() {
      return (String[]) getObject();
    }

    public File getFile() {
      return (File) getObject();
    }

    public int getInt() {
      return ((Integer) getObject()).intValue();
    }

    public Object[] getObjects() {
      return (Object[]) getObject();
    }
  }

  private final Class       theInterface;
  private final XmlObject[] beansToSetOn;
  private final Object      realImplementation;
  private final String      xpathPrefix;

  private final Map         configItems;

  // The XPath prefix is because when we grab XPaths out of XPathBasedConfigItems, they're relative to the bean that
  // that config object is based on, which might be a child of the one in the repository (using the ChildBeanRepository
  // stuff). This path needs to be the XPath from the root of the repository to the bean used as the root of the config
  // object.
  public TestConfigObjectInvocationHandler(Class theInterface, XmlObject[] beansToSetOn, Object realImplementation,
                                           String xpathPrefix) {
    Assert.assertNotNull(theInterface);
    Assert.assertNoNullElements(beansToSetOn);
    Assert.assertNotNull(realImplementation);

    Assert.eval(theInterface.isInstance(realImplementation));

    this.theInterface = theInterface;
    this.beansToSetOn = beansToSetOn;
    this.realImplementation = realImplementation;
    this.xpathPrefix = xpathPrefix;

    this.configItems = new HashMap();
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Assert.assertNotNull(proxy);
    Assert.assertNotNull(method);

    // It's a getter method
    if (ConfigItem.class.isAssignableFrom(method.getReturnType())) {
      String propertyName = method.getName();
      return itemForProperty(propertyName, method);
    } else if (NewConfig.class.isAssignableFrom(method.getReturnType())) {
      Object newRealObject = method.invoke(this.realImplementation, new Object[0]);
      String subXPath = fetchSubXPathFor(method.getName());
      String xpath;
      if (xpathPrefix != null && subXPath != null) xpath = xpathPrefix + "/" + subXPath;
      else if (xpathPrefix != null) xpath = xpathPrefix;
      else xpath = subXPath;
      System.err.println("Creating new sub-config object of " + method.getReturnType());
      System.err.println("XPath: " + xpath);
      System.err.println("New real object: " + newRealObject);
      return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { method.getReturnType() },
                                    new TestConfigObjectInvocationHandler(method.getReturnType(), this.beansToSetOn,
                                                                          newRealObject, xpath));
    } else {
      throw Assert.failure("This method, '" + method.getName() + "', has a return type of "
                           + method.getReturnType().getName() + ", which isn't a descendant of ConfigItem. "
                           + "We don't know how to support this for test config objects yet.");
    }
  }

  private String fetchSubXPathFor(String methodName) {
    String fieldName = camelToUnderscores(methodName) + "_SUB_XPATH";

    try {
      Field theField = this.realImplementation.getClass().getField(fieldName);
      Assert.eval(Modifier.isPublic(theField.getModifiers()));
      Assert.eval(Modifier.isStatic(theField.getModifiers()));
      Assert.eval(Modifier.isFinal(theField.getModifiers()));
      Assert.eval(String.class.equals(theField.getType()));
      return (String) theField.get(null);
    } catch (IllegalAccessException iae) {
      throw Assert.failure("Can't fetch field '" + fieldName + "'?", iae);
    } catch (NoSuchFieldException nsfe) {
      return null;
    }
  }

  private String camelToUnderscores(String methodName) {
    StringBuffer out = new StringBuffer();
    char[] source = methodName.toCharArray();
    boolean lastWasLowercase = isLowercase(source[0]);
    out.append(toUppercase(source[0]));

    for (int i = 1; i < source.length; ++i) {
      boolean thisIsLowercase = isLowercase(source[i]);

      if (lastWasLowercase && (!thisIsLowercase)) {
        // transition
        out.append("_");
      }

      lastWasLowercase = thisIsLowercase;
      out.append(toUppercase(source[i]));
    }

    return out.toString();
  }

  private boolean isLowercase(char ch) {
    return Character.isLowerCase(ch);
  }

  private char toUppercase(char ch) {
    return Character.toUpperCase(ch);
  }

  private synchronized OurSettableConfigItem itemForProperty(String propertyName, Method theMethod) {
    OurSettableConfigItem out = (OurSettableConfigItem) this.configItems.get(propertyName);
    if (out == null) {
      try {
        ConfigItem realItem = (ConfigItem) theMethod.invoke(realImplementation, new Object[0]);
        Assert.eval("The base item we found was a " + realItem.getClass() + ", not an XPathBasedConfigItem",
                    realItem instanceof XPathBasedConfigItem);

        String effectiveXPath = (this.xpathPrefix == null ? "" : this.xpathPrefix + "/")
                                + ((XPathBasedConfigItem) realItem).xpath();
        logger.debug("For property '" + propertyName + "' on proxied config-object implementation of interface "
                     + this.theInterface.getName() + ", returning a config item with effective XPath '"
                     + effectiveXPath + "'.");

        out = new OurSettableConfigItem(effectiveXPath);
        this.configItems.put(propertyName, out);
      } catch (IllegalAccessException iae) {
        throw Assert.failure("Unable to retrieve the real ConfigItem so we can get its XPath.", iae);
      } catch (InvocationTargetException ite) {
        throw Assert.failure("Unable to retrieve the real ConfigItem so we can get its XPath.", ite);
      }
    }
    return out;
  }

}
