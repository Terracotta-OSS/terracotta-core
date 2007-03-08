/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.reflect.ClassInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class CompoundExpressionMatcher implements ClassExpressionMatcher {

  private final Collection matchers = new ArrayList();
  
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
