/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.tickertoken;

import java.util.HashMap;
import java.util.Iterator;
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

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for(Iterator iter = context.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry)iter.next();
      sb.append(" key : " + entry.getKey() + " value: " + entry.getValue() + " ");
    }
    return sb.toString();
  }
  
  
}
