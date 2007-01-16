/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.util.factory.AbstractFactory;

import java.util.ResourceBundle;

public abstract class AbstractBundleFactory extends AbstractFactory implements BundleFactory {
  private static BundleFactory bundleFactory;
  private static String FACTORY_SERVICE_ID = "com.tc.util.BundleFactory";
  private static Class STANDARD_BUNDLE_FACTORY_CLASS = StandardBundleFactory.class;
  
  public static AbstractBundleFactory getFactory() {
    return (AbstractBundleFactory)getFactory(FACTORY_SERVICE_ID, STANDARD_BUNDLE_FACTORY_CLASS);
  }

  public abstract ResourceBundle createBundle(Class clas);
  
  public static ResourceBundle getBundle(Class clas) {
    if(bundleFactory == null) {
      bundleFactory = getFactory();
    }
    return bundleFactory.createBundle(clas);
  }
}
