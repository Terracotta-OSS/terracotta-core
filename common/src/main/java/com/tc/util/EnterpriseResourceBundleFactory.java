/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.ResourceBundle;

public class EnterpriseResourceBundleFactory extends StandardResourceBundleFactory {
  @Override
  public ResourceBundle createBundle(Class<?> c) {
    try {
      return ResourceBundle.getBundle(c.getName()+"EnterpriseResourceBundle");
    } catch(Exception e) {
      return super.createBundle(c);
    }
  }
}
