/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.ListResourceBundle;
import java.util.ResourceBundle;

public class ProductInfoEnterpriseBundle extends ListResourceBundle {
  public ProductInfoEnterpriseBundle() {
    super();
    setParent(ResourceBundle.getBundle("com.tc.util.ProductInfoBundle"));
  }
  
  @Override
  protected Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {{"moniker", "Terracotta"}};
}
