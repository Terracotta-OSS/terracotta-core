/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.injection;

import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class InjectionInstrumentationRegistry {
  private final Map<String, InjectionInstrumentation> registry = new HashMap<String, InjectionInstrumentation>();

  public synchronized void registerInstrumentation(final String type, final InjectionInstrumentation instrumentation) {
    Assert.assertNotNull(type);
    Assert.assertNotNull(instrumentation);

    registry.put(type, instrumentation);
  }

  public synchronized InjectionInstrumentation lookupInstrumentation(final String type) {
    return registry.get(type);
  }
}