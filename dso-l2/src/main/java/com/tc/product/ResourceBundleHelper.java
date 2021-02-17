/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.product;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

public class ResourceBundleHelper {
  private ResourceBundle bundle;

  public ResourceBundleHelper(Class<?> clas) {
    bundle = AbstractResourceBundleFactory.getBundle(clas);
  }

  public ResourceBundleHelper(Object instance) {
    Class<?> clas = instance.getClass();
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

  public Object getObject(String key) {
    Objects.requireNonNull(key);
    return bundle.getObject(key);
  }

  public String getString(String key) {
    Objects.requireNonNull(key);
    return bundle.getString(key);
  }

  public String format(String key, Object[] args) {
    Objects.requireNonNull(key);
    String fmt = getString(key);
    Objects.requireNonNull(fmt);
    return MessageFormat.format(fmt, args);
  }
}
