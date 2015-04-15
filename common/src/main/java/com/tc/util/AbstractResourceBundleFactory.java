/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
  public abstract ResourceBundle createBundle(Class clas);
  
  public static ResourceBundle getBundle(Class clas) {
    if(bundleFactory == null) {
      bundleFactory = getFactory();
    }
    return bundleFactory.createBundle(clas);
  }
}
