/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

/**
 * provides porable objects directly referenced by this object/class combo
 */
public interface PortableObjectProvider {
  public TraversedReferences getPortableObjects(Class clazz, Object start, TraversedReferences addTo);
}