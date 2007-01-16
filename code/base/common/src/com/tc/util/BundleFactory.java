/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.ResourceBundle;

public interface BundleFactory {
  ResourceBundle createBundle(Class c);
}
