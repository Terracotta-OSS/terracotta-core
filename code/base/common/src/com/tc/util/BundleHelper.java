/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class BundleHelper {
  private ResourceBundle bundle;
  
  public BundleHelper(Class clas) {
    this(clas.getName()+"Bundle");
  }
  
  public BundleHelper(String name) {
    bundle = ResourceBundle.getBundle(name);
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
