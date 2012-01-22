/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.util.ListResourceBundle;

public class ProductInfoBundle extends ListResourceBundle {
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {
    {"moniker", "Terracotta"},
    {"invalid.timestamp", "The build timestamp string ''{0}'' does not appear to be valid."},
    {"load.properties.failure", "Unable to load build properties from ''{0}''."},
    {"copyright", "Copyright (c) 2003-2008 Terracotta, Inc. All rights reserved."},
    {"option.verbose", "Produces more detailed information."},
    {"option.raw", "Produces raw information."},
    {"option.help", "Shows this text."}
  };
}
