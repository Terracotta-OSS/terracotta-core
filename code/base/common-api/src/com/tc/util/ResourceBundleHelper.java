/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ResourceBundleHelper {
  private ResourceBundle bundle;

  public ResourceBundleHelper(Class clas) {
    bundle = AbstractResourceBundleFactory.getBundle(clas);
  }

  public ResourceBundleHelper(Object instance) {
    Class clas = instance.getClass();
    while (true) {
      try {
        bundle = AbstractResourceBundleFactory.getBundle(clas);
        break;
      } catch (MissingResourceException e) {
        if ((clas = clas.getSuperclass()) == null) { throw new RuntimeException("Missing bundle for type '"
                                                                                + instance.getClass() + "'"); }
      }
    }
  }

  public Object getObject(final String key) {
    Assert.assertNotNull(key);
    return bundle.getObject(key);
  }

  public String getString(final String key) {
    Assert.assertNotNull(key);
    return bundle.getString(key);
  }

  public String format(final String key, Object[] args) {
    Assert.assertNotNull(key);
    String fmt = getString(key);
    Assert.assertNotNull(fmt);
    return MessageFormat.format(fmt, args);
  }
}
