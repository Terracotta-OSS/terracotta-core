/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.apache.xmlbeans.XmlObject;

import com.terracottatech.configV2.ClassExpression;
import com.terracottatech.configV2.Include;

public abstract class Rule {
  public static final int INCLUDE_RULE = 0;
  public static final int EXCLUDE_RULE = 1;
  public static final int DEFAULT_TYPE = INCLUDE_RULE;
  
  protected XmlObject m_xmlObject;
  
  public static Rule create(XmlObject xmlObject) {
    validate(xmlObject);
    
    if(xmlObject instanceof Include) {
      return new IncludeRule((Include)xmlObject);
    }
    else {
      return new ExcludeRule((ClassExpression)xmlObject);
    }
  }
  
  protected Rule(Include include) {
    setXmlObject(include);
  }

  protected Rule(ClassExpression exclude) {
    setXmlObject(exclude);
  }
  
  public int getType() {
    return m_xmlObject instanceof Include ? INCLUDE_RULE : EXCLUDE_RULE;
  }

  public boolean isIncludeRule() {
    return getType() == INCLUDE_RULE;
  }
  
  public boolean isExcludeRule() {
    return !isIncludeRule();
  }
  
  private static void validate(XmlObject xmlObject) {
    if(xmlObject == null) {
      throw new AssertionError("xmlObject is null");
    }
    
    if(!(xmlObject instanceof Include) && !(xmlObject instanceof ClassExpression)) {
      throw new AssertionError("xmlObject of wrong type");
    }
   }
  
  public void setXmlObject(XmlObject xmlObject) {
    validate(xmlObject);
    m_xmlObject = xmlObject;
  }
  
  protected XmlObject getXmlObject() {
    return m_xmlObject;
  }
  
  public abstract String getExpression();
  public abstract void setExpression(String expr);
  
  public abstract RuleDetail getDetails();
  public abstract void setDetails(RuleDetail details);
}
