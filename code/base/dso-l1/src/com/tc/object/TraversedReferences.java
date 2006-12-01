/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import java.util.Iterator;

public interface TraversedReferences {
  public void addAnonymousReference(Object o);
  public void addNamedReference(String className, String fieldName, Object value);
  public void addNamedReference(String fullyQualifiedFieldname, Object value);
  public Iterator iterator();
}
