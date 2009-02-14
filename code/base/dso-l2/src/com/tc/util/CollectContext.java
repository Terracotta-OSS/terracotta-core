/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.HashMap;
import java.util.Map;

public class CollectContext {

  public static final CollectContext EMPTY_CONTEXT = new CollectContext();

  protected Map                      context       = new HashMap();

  public void collect(String key, Object value) {
    context.put(key, value);
  }

  public Object getValue(String key) {
    return context.get(key);
  }
}
