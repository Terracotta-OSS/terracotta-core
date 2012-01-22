/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.util.ResourceBundle;

public interface ResourceBundleFactory {
  ResourceBundle createBundle(Class c);
}
