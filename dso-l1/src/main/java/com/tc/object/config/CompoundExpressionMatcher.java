/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.reflect.ClassInfo;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompoundExpressionMatcher implements ClassExpressionMatcher {

  private final Collection matchers = new CopyOnWriteArrayList();
  
  public boolean match(ClassInfo classInfo) {
    for(Iterator i = matchers.iterator(); i.hasNext();) {
      if (((ClassExpressionMatcher)i.next()).match(classInfo)) {
        return true;
      }
    }
    return false;
  }

  public void add(ClassExpressionMatcher matcher) {
    matchers.add(matcher);
  }

}
