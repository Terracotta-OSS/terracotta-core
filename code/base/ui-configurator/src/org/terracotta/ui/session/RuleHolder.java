/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.terracottatech.config.ClassExpression;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;

public class RuleHolder {
  private InstrumentedClasses instrumentedClasses;
  private Rule                rule;

  public RuleHolder(InstrumentedClasses instrumentedClasses, Rule rule) {
    this.instrumentedClasses = instrumentedClasses;
    this.rule = rule;
  }

  public Rule getRule() {
    return rule;
  }

  public void replace(Rule newRule) {
    Node newRuleNode = newRule.getXmlObject().getDomNode();
    Node ruleNode = rule.getXmlObject().getDomNode();
    Node topNode = instrumentedClasses.getDomNode();
    NodeList topNodeList = topNode.getChildNodes();
    int listSize = topNodeList.getLength();

    rule = newRule;
    topNode.removeChild(newRuleNode);

    for (int i = 0; i < listSize; i++) {
      if (ruleNode == topNodeList.item(i)) {
        topNode.insertBefore(newRuleNode, ruleNode);
        topNode.removeChild(ruleNode);
        return;
      }
    }
  }

  public int getType() {
    return rule.getType();
  }

  public void toggleRuleType() {
    String expr = getExpression();
    XmlObject xmlObject;

    if (rule.isIncludeRule()) {
      ClassExpression ce = instrumentedClasses.addNewExclude();
      ce.setStringValue(expr);
      xmlObject = ce;
    } else {
      Include include = instrumentedClasses.addNewInclude();
      include.setClassExpression(expr);
      xmlObject = include;
    }

    replace(Rule.create(xmlObject));
  }

  public void setType(int type) {
    if (type != getType()) {
      toggleRuleType();
    }
  }

  public String getExpression() {
    return rule.getExpression();
  }

  public void setExpression(String expr) {
    rule.setExpression(expr);
  }

  public RuleDetail getDetails() {
    return rule.getDetails();
  }

  public void setDetails(RuleDetail details) {
    rule.setDetails(details);
  }
}
