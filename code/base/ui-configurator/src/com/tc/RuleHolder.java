/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.terracottatech.config.ClassExpression;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;

public class RuleHolder {
  private InstrumentedClasses m_instrumentedClasses;
  private Rule                m_rule;
  
  public RuleHolder(InstrumentedClasses instrumentedClasses, Rule rule) {
    m_instrumentedClasses = instrumentedClasses;
    m_rule                = rule;
  }
  
  public Rule getRule() {
    return m_rule;
  }
  
  public void replace(Rule newRule) {
    Node     newRuleNode = newRule.getXmlObject().getDomNode();
    Node     ruleNode    = m_rule.getXmlObject().getDomNode();
    Node     topNode     = m_instrumentedClasses.getDomNode();
    NodeList topNodeList = topNode.getChildNodes();
    int      listSize    = topNodeList.getLength();

    m_rule = newRule;
    topNode.removeChild(newRuleNode);
    
    for(int i = 0; i < listSize; i++) {
      if(ruleNode == topNodeList.item(i)) {
        topNode.insertBefore(newRuleNode, ruleNode);
        topNode.removeChild(ruleNode);
        return;
      }
    }
  }
  
  public int getType() {
    return m_rule.getType();
  }
  
  public void toggleRuleType() {
    String    expr = getExpression();
    XmlObject xmlObject;
    
    if(m_rule.isIncludeRule()) {
      ClassExpression ce = m_instrumentedClasses.addNewExclude();
      ce.setStringValue(expr);
      xmlObject = ce;
    }
    else {
      Include include = m_instrumentedClasses.addNewInclude();
      include.setClassExpression(expr);
      xmlObject = include;
    }
    
    replace(Rule.create(xmlObject));
  }

  public void setType(int type) {
    if(type != getType()) {
      toggleRuleType();
    }
  }
  
  public String getExpression() {
    return m_rule.getExpression();
  }
  
  public void setExpression(String expr) {
    m_rule.setExpression(expr);
  }
  
  public RuleDetail getDetails() {
    return m_rule.getDetails();
  }
  
  public void setDetails(RuleDetail details) {
    m_rule.setDetails(details);
  }
}
