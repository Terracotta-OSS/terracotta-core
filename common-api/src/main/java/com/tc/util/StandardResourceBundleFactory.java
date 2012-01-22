/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.util.ResourceBundle;

public class StandardResourceBundleFactory extends AbstractResourceBundleFactory {
  public ResourceBundle createBundle(Class c) {
    return ResourceBundle.getBundle(c.getName()+"Bundle");
  }
}
