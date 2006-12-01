/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.dynamic;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.listen.ConfigurationChangeListener;
import com.tc.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A {@link ConfigItem} that uses XPaths to find its data. Caches the current value for efficiency, and provides for
 * specification of a default, which it will use if the value is <code>null</code>.
 * </p>
 * <p>
 * Subclasses must take care of extracting the actual required value from the {@link XmlObject}.
 * </p>
 * <p>
 * Normally, this class would be doing too much stuff &mdash; it handles defaults, caching, and data extraction, all at
 * once. However, because of the restrictions of Java's type system, doing it any other way seems to lead to an
 * explosion of classes. If you can figure out a way to factor it that splits up this class but doesn't lead to a class
 * explosion, by all means, do so.
 */
public abstract class XPathBasedConfigItem implements ConfigItem, ConfigurationChangeListener {

  private final ConfigContext              context;
  private final String                     xpath;

  private Object                           defaultValue;
  private boolean                          defaultInitialized;

  private final CompoundConfigItemListener listener;

  private boolean                          haveCurrentValue;
  private Object                           currentValue;

  public XPathBasedConfigItem(ConfigContext context, String xpath) {
    Assert.assertNotNull(context);
    Assert.assertNotBlank(xpath);

    this.context = context;
    this.xpath = xpath;

    this.defaultInitialized = false;

    this.listener = new CompoundConfigItemListener();

    this.haveCurrentValue = false;
    this.currentValue = null;

    this.context.itemCreated(this);
  }

  private synchronized void initializeDefaultIfNecessary() {
    if (!this.defaultInitialized) {
      try {
        if (this.context.hasDefaultFor(xpath)) {
          this.defaultValue = fetchDataFromXmlObject(this.context.defaultFor(this.xpath));
        } else {
          this.defaultValue = null;
        }
        this.defaultInitialized = true;
      } catch (XmlException xmle) {
        throw Assert.failure("Couldn't use XPath '" + this.xpath + "' to fetch a default value", xmle);
      }
    }
  }

  /**
   * Generally, you <strong>SHOULD NOT</strong> use this constructor. Instead, you should let the schema specify the
   * default, as we want that to be the canonical repository for default values. However, certain things &mdash; like
   * attributes, arrays, and other complex structures &mdash; can't have defaults provided in a schema, so we must
   * provide this method instead.
   */
  public XPathBasedConfigItem(ConfigContext context, String xpath, Object defaultValue) {
    Assert.assertNotNull(context);
    Assert.assertNotBlank(xpath);

    this.context = context;
    this.xpath = xpath;

    this.defaultValue = defaultValue;
    this.defaultInitialized = true;

    this.listener = new CompoundConfigItemListener();

    this.haveCurrentValue = false;
    this.currentValue = null;

    this.context.itemCreated(this);
  }

  public synchronized Object getObject() {
    if (!this.haveCurrentValue) {
      synchronized (this.context.syncLockForBean()) {
        this.currentValue = fetchDataFromTopLevelBean(this.context.bean());
        this.haveCurrentValue = true;
      }
    }

    return this.currentValue;
  }

  private Object fetchDataFromTopLevelBean(XmlObject bean) {
    Object out = null;

    initializeDefaultIfNecessary();

    if (bean != null) {
      XmlObject[] targetList;

      // We synchronize on the bean for test code; test code might be changing it while we're selecting from it.
      synchronized (bean) {
        targetList = bean.selectPath(this.xpath);
      }

      if (targetList == null || targetList.length == 0 || (targetList.length == 1 && targetList[0] == null)) out = fetchDataFromXmlObject(null);
      else if (targetList.length == 1) out = fetchDataFromXmlObject(targetList[0]);
      else throw Assert.failure("From " + bean + ", XPath '" + this.xpath + "' selected " + targetList.length
                                + " nodes, not " + "just 1. This should never happen; there is a bug in the software.");
    }

    if (out == null) out = this.defaultValue;

    return out;
  }

  protected abstract Object fetchDataFromXmlObject(XmlObject xmlObject);

  protected final Object fetchDataFromXmlObjectByReflection(XmlObject xmlObject, String methodName) {
    if (xmlObject == null) return null;

    Method method = getMethodWithNoParametersByName(xmlObject.getClass(), methodName);

    if (method == null) {
      // formatting
      throw Assert.failure("There is no method named '" + methodName + "' on object " + xmlObject + " (of class "
                           + xmlObject.getClass().getName() + ") with no parameters.");
    }

    try {
      return method.invoke(xmlObject, new Object[0]);
    } catch (IllegalArgumentException iae) {
      throw Assert.failure("Unable to invoke method " + method + ".", iae);
    } catch (IllegalAccessException iae) {
      throw Assert.failure("Unable to invoke method " + method + ".", iae);
    } catch (InvocationTargetException ite) {
      throw Assert.failure("Unable to invoke method " + method + ".", ite);
    }
  }

  protected final Method getMethodWithNoParametersByName(Class theClass, String methodName) {
    Method[] allMethods = theClass.getMethods();
    for (int i = 0; i < allMethods.length; ++i) {
      if (allMethods[i].getName().equals(methodName) && allMethods[i].getParameterTypes().length == 0) { return allMethods[i]; }
    }

    return null;
  }

  public synchronized void addListener(ConfigItemListener changeListener) {
    Assert.assertNotNull(changeListener);
    this.listener.addListener(changeListener);
  }

  public synchronized void removeListener(ConfigItemListener changeListener) {
    Assert.assertNotNull(changeListener);
    this.listener.removeListener(changeListener);
  }

  public synchronized void configurationChanged(XmlObject oldConfig, XmlObject newConfig) {
    Object oldValue, newValue;

    synchronized (this.context.syncLockForBean()) {
      if (this.haveCurrentValue) oldValue = this.currentValue;
      else oldValue = fetchDataFromTopLevelBean(oldConfig);

      newValue = fetchDataFromTopLevelBean(newConfig);

      this.currentValue = newValue;
      this.haveCurrentValue = true;
    }

    if (((oldValue == null) != (newValue == null)) || ((oldValue != null) && (!oldValue.equals(newValue)))) {
      this.listener.valueChanged(oldValue, newValue);
    }
  }

  public String toString() {
    return "configuration item at XPath '" + this.xpath + "'";
  }

  /**
   * For <strong>TESTS ONLY</strong>.
   */
  public ConfigContext context() {
    return this.context;
  }

  /**
   * For <strong>TESTS ONLY</strong>.
   */
  public String xpath() {
    return this.xpath;
  }

  /**
   * For <strong>TESTS ONLY</strong>.
   */
  public Object defaultValue() {
    initializeDefaultIfNecessary();
    return this.defaultValue;
  }

}
