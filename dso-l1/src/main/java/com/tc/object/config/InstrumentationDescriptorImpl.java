/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.object.config.schema.InstrumentedClass;

final class InstrumentationDescriptorImpl implements InstrumentationDescriptor {
  private final InstrumentedClass      classDescriptor;
  private final ClassExpressionMatcher expressionMatcher;

  public InstrumentationDescriptorImpl(InstrumentedClass classDescriptor, ClassExpressionMatcher expressionMatcher) {
    this.classDescriptor = classDescriptor;
    this.expressionMatcher = expressionMatcher;
  }

  public String getOnLoadMethodIfDefined() {
    if (isExclude()) { return null; }
    return classDescriptor.onLoad().isCallMethodOnLoadType() ? classDescriptor.onLoad().getMethod() : null;
  }

  public String getOnLoadScriptIfDefined() {
    if (isExclude()) { return null; }
    return classDescriptor.onLoad().isExecuteScriptOnLoadType() ? classDescriptor.onLoad().getExecuteScript() : null;
  }

  public boolean isCallConstructorOnLoad() {
    return isExclude() ? false : classDescriptor.onLoad().isCallConstructorOnLoad();
  }

  public boolean isHonorTransient() {
    return isExclude() ? false : classDescriptor.honorTransient();
  }
  
  public boolean isHonorVolatile() {
    return isExclude() ? false : classDescriptor.honorVolatile();
  }

  public boolean matches(ClassInfo classInfo) {
    return expressionMatcher.match(classInfo);
  }

  public boolean isInclude() {
    return classDescriptor.isInclude();
  }

  public boolean isExclude() {
    return !isInclude();
  }

  public String toString() {
    return "InstrumentationDescriptorImpl[" + classDescriptor.classExpression() + "]";
  }

}
