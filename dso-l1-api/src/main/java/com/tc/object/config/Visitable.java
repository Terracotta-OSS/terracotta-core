/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

public interface Visitable {
  public ConfigVisitor visit(ConfigVisitor visitor, DSOApplicationConfig config);
}
