/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object;

/**
 * provides porable objects directly referenced by this object/class combo
 */
public interface PortableObjectProvider {
  public TraversedReferences getPortableObjects(Class clazz, Object start, TraversedReferences addTo);
}