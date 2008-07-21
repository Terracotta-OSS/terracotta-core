/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.apache.xmlbeans.XmlOptions;
import org.dijon.ContainerResource;

import com.tc.admin.AdminClient;
import com.tc.admin.AdminClientContext;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTextArea;
import com.tc.admin.common.XTree;
import com.tc.object.LiteralValues;
import com.tc.stats.DSOClassInfo;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.KeyStroke;

public class ClassesPanel extends XContainer {
  private ClassesNode           m_classesNode;
  private ClassesTable          m_table;
  private XTree                 m_tree;
  private ClassesTreeMap        m_treeMap;
  private XTextArea             m_configText;
  private static XmlOptions     m_xmlOpts;

  private static final String   REFRESH           = "Refresh";

  private static final String[] IGNORE_CLASS_LIST = { "com.tcclient", "java." };

  private static LiteralValues  LITERALS          = new LiteralValues();

  static {
    m_xmlOpts = new XmlOptions();
    m_xmlOpts.setSavePrettyPrint();
    m_xmlOpts.setSavePrettyPrintIndent(2);
  }

  public ClassesPanel(ClassesNode classesNode) {
    super();

    load((ContainerResource) AdminClient.getContext().getComponent("ClassesPanel"));
    m_classesNode = classesNode;
    DSOClassInfo[] classInfo = getClassInfos();

    m_table = (ClassesTable) findComponent("ClassTable");
    m_table.setClassInfo(classInfo);

    m_tree = (XTree) findComponent("ClassTree");
    m_tree.setShowsRootHandles(true);
    m_tree.setModel(new ClassTreeModel(classInfo));
    
    m_treeMap = (ClassesTreeMap) findComponent("ClassesTreeMap");
    m_treeMap.setModel((ClassTreeModel) m_tree.getModel());

    m_configText = (XTextArea) findComponent("ClassesConfigTextArea");
    updateConfigText();

    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true);
    getActionMap().put(REFRESH, new RefreshAction());
    getInputMap().put(ks, REFRESH);
  }

  private DSOClassInfo[] getClassInfos() {
    DSOClassInfo[] classInfo = m_classesNode.getClusterModel().getClassInfo();
    ArrayList<DSOClassInfo> list = new ArrayList<DSOClassInfo>();

    if (classInfo != null) {
      for (DSOClassInfo info : classInfo) {
        String className = info.getClassName();
        if (className.startsWith("com.tcclient")) continue;
        if (className.startsWith("[")) {
          int i = 0;
          while (className.charAt(i) == '[')
            i++;
          if (className.charAt(i) == 'L') {
            className = className.substring(i + 1, className.length() - 1);
          } else {
            switch (className.charAt(i)) {
              case 'Z':
                className = "boolean";
                break;
              case 'I':
                className = "int";
                break;
              case 'F':
                className = "float";
                break;
              case 'C':
                className = "char";
                break;
              case 'D':
                className = "double";
                break;
              case 'B':
                className = "byte";
                break;
            }
          }
          StringBuffer sb = new StringBuffer(className);
          for (int j = 0; j < i; j++)
            sb.append("[]");
          className = sb.toString();
        }
        list.add(new DSOClassInfo(className, info.getInstanceCount()));
      }
    }

    return list.toArray(new DSOClassInfo[0]);
  }

  private void updateConfigText() {
    DSOClassInfo[] classInfo = ((ClassTreeModel) m_tree.getModel()).getClassInfo();
    HashMap<String, String> map = new HashMap<String, String>();
    for (DSOClassInfo info : classInfo) {
      String className = info.getClassName();
      int brace = className.indexOf("[");
      if (brace != -1) {
        className = className.substring(0, brace);
      }
      map.put(className, className);
    }
    InstrumentedClasses instrumentedClasses = InstrumentedClasses.Factory.newInstance();
    Iterator<String> iter = map.keySet().iterator();
    while (iter.hasNext()) {
      String className = iter.next();
      if (ignoreClass(className)) {
        continue;
      }
      instrumentedClasses.addNewInclude().setClassExpression(className);
    }
    TcConfigDocument configDoc = TcConfigDocument.Factory.newInstance();
    TcConfig config = configDoc.addNewTcConfig();
    config.addNewApplication().addNewDso().setInstrumentedClasses(instrumentedClasses);
    m_configText.setText(config.xmlText(m_xmlOpts));
  }

  private boolean ignoreClass(String className) {
    if (LITERALS.isLiteral(className)) { return true; }
    for (String pattern : IGNORE_CLASS_LIST) {
      if (className.startsWith(pattern)) return true;
    }
    return false;
  }

  public class RefreshAction extends XAbstractAction {
    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  public void refresh() {
    AdminClientContext acc = AdminClient.getContext();

    acc.setStatus(acc.getMessage("dso.classes.refreshing"));
    acc.block();

    DSOClassInfo[] classInfo = getClassInfos();
    m_table.setClassInfo(classInfo);
    ((ClassTreeModel) m_tree.getModel()).setClassInfo(classInfo);
    m_treeMap.setModel((ClassTreeModel) m_tree.getModel());
    updateConfigText();

    acc.clearStatus();
    acc.unblock();
  }
}
