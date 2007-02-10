/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import com.terracottatech.config.ClassExpression;

public class ExcludeRule extends Rule {
  public ExcludeRule(ClassExpression expression) {
    super(expression);
  }
  
  public ClassExpression getClassExpression() {
    return (ClassExpression)getXmlObject();
  }
  
  public String getExpression() {
    return getClassExpression().getStringValue();
  }
  
  public void setExpression(String expr) {
    getClassExpression().setStringValue(expr);
  }
  
  public void setDetails(RuleDetail details) {/**/}
  
  public RuleDetail getDetails() {
    return null;
  }
}
