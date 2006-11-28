/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.config;

public interface Visitable {
  public ConfigVisitor visit(ConfigVisitor visitor, DSOApplicationConfig config);
}
