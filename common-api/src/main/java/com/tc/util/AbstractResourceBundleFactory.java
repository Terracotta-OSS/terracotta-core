/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.util.factory.AbstractFactory;

import java.util.ResourceBundle;

public abstract class AbstractResourceBundleFactory extends AbstractFactory implements ResourceBundleFactory {
  private static ResourceBundleFactory bundleFactory;
  private static String FACTORY_SERVICE_ID = "com.tc.util.ResourceBundleFactory";
  private static Class STANDARD_BUNDLE_FACTORY_CLASS = StandardResourceBundleFactory.class;
  
  public static AbstractResourceBundleFactory getFactory() {
    return (AbstractResourceBundleFactory)getFactory(FACTORY_SERVICE_ID, STANDARD_BUNDLE_FACTORY_CLASS);
  }

  public abstract ResourceBundle createBundle(Class clas);
  
  public static ResourceBundle getBundle(Class clas) {
    if(bundleFactory == null) {
      bundleFactory = getFactory();
    }
    return bundleFactory.createBundle(clas);
  }
}
