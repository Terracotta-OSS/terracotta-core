/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import org.apache.xmlbeans.XmlObject;
import org.terracotta.ui.session.Rule;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tc.admin.common.XObjectTableModel;
import com.terracottatech.config.ClassExpression;
import com.terracottatech.config.Include;
import com.terracottatech.config.InstrumentedClasses;

import java.util.ArrayList;

public class RuleModel extends XObjectTableModel {
  private InstrumentedClasses   instrumentedClasses;

  private static final String[] INCLUDE_FIELDS  = { "Type", "Expression", "Details" };

  private static final String[] INCLUDE_HEADERS = { "Rule", "Expression", "Details" };

  public RuleModel() {
    super(RuleHolder.class, INCLUDE_FIELDS, INCLUDE_HEADERS);
  }

  public void setInstrumentedClasses(InstrumentedClasses instrumentedClasses) {
    clear();
    if ((this.instrumentedClasses = instrumentedClasses) != null) {
      setRules(instrumentedClasses.selectPath("*"));
    }
  }

  public void setRules(XmlObject[] objects) {
    ArrayList list = new ArrayList();
    for (int i = 0; i < objects.length; i++) {
      list.add(new RuleHolder(instrumentedClasses, Rule.create(objects[i])));
    }
    set(list.toArray(new RuleHolder[0]));
  }

  private void updateRules() {
    clear();
    setRules(instrumentedClasses.selectPath("*"));
  }

  public void setValueAt(Object value, int row, int col) {
    if (col == 0) {
      getRuleHolderAt(row).toggleRuleType();
      updateRules();
      return;
    }
    super.setValueAt(value, row, col);
  }

  public RuleHolder getRuleHolderAt(int i) {
    return (RuleHolder) getObjectAt(i);
  }

  public Rule getRuleAt(int i) {
    return ((RuleHolder) getObjectAt(i)).getRule();
  }

  public void removeRule(int index) {
    Rule rule = getRuleAt(index);
    Node ruleNode = rule.getXmlObject().getDomNode();
    Node topNode = instrumentedClasses.getDomNode();
    topNode.removeChild(ruleNode);
  }

  public void moveRuleUp(int index) {
    Rule rule = getRuleAt(index);
    Node ruleNode = rule.getXmlObject().getDomNode();
    Node topNode = instrumentedClasses.getDomNode();
    NodeList topNodeList = topNode.getChildNodes();
    int listSize = topNodeList.getLength();
    Node prevNode;

    for (int i = 0; i < listSize; i++) {
      if (ruleNode == topNodeList.item(i)) {
        while (--i >= 0) {
          prevNode = topNodeList.item(i);
          if (prevNode.getNodeType() == Node.ELEMENT_NODE) {
            topNode.removeChild(ruleNode);
            topNode.insertBefore(ruleNode, prevNode);
            return;
          }
        }
      }
    }
  }

  public void moveRuleDown(int index) {
    Rule rule = getRuleAt(index);
    Node ruleNode = rule.getXmlObject().getDomNode();
    Node topNode = instrumentedClasses.getDomNode();
    NodeList topNodeList = topNode.getChildNodes();
    int listSize = topNodeList.getLength();
    Node nextNode;

    for (int i = 0; i < listSize; i++) {
      if (ruleNode == topNodeList.item(i)) {
        while (++i < listSize) {
          nextNode = topNodeList.item(i);
          if (nextNode.getNodeType() == Node.ELEMENT_NODE) {
            while (++i < listSize) {
              nextNode = topNodeList.item(i);
              if (nextNode.getNodeType() == Node.ELEMENT_NODE) {
                topNode.removeChild(ruleNode);
                topNode.insertBefore(ruleNode, nextNode);
                return;
              }
            }
            topNode.removeChild(ruleNode);
            topNode.appendChild(ruleNode);
            return;
          }
        }
      }
    }
  }

  public int size() {
    return getRowCount();
  }

  public void addExclude(String expr) {
    ClassExpression classExpr = instrumentedClasses.addNewExclude();
    classExpr.setStringValue(expr);
    add(new RuleHolder(instrumentedClasses, new ExcludeRule(classExpr)));
  }

  public void removeRuleAt(int i) {
    removeRule(i);
  }

  public void addInclude(String expr) {
    Include include = instrumentedClasses.addNewInclude();
    include.setClassExpression(expr);
    add(new RuleHolder(instrumentedClasses, new IncludeRule(include)));
  }

  public boolean hasEditor(Class type) {
    return type.equals(RuleDetail.class);
  }
}
