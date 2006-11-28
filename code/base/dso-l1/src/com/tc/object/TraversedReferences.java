/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object;

import java.util.Iterator;

public interface TraversedReferences {
  public void addAnonymousReference(Object o);
  public void addNamedReference(String className, String fieldName, Object value);
  public void addNamedReference(String fullyQualifiedFieldname, Object value);
  public Iterator iterator();
}
