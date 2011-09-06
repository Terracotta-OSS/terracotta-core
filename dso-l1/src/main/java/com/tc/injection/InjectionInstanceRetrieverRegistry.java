/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.injection;

import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class InjectionInstanceRetrieverRegistry {
  private final Map<Class, InjectionInstanceRetriever> registry = new HashMap<Class, InjectionInstanceRetriever>();

  public synchronized void registerRetriever(final Class type, final InjectionInstanceRetriever retriever) {
    Assert.assertNotNull(type);
    Assert.assertNotNull(retriever);

    registry.put(type, retriever);
  }

  public synchronized InjectionInstanceRetriever lookupRetriever(final Class type) {
    return registry.get(type);
  }
}