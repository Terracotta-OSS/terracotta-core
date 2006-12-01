/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

public class SpringApplicationContextPathConfigBuilderImpl extends BaseConfigBuilder implements SpringApplicationContextPathConfigBuilder {

  public SpringApplicationContextPathConfigBuilderImpl(String path) {
    super(7, new String[]{"path"});
    setProperty("path", path);
  }

  public String toString() {
    return elements(new String[] { "path" });
  }
}
