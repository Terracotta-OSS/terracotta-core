/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.reflect.ClassInfo;

public interface ClassExpressionMatcher {
  public boolean match(ClassInfo classInfo);

}
