/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.apache.xmlbeans.XmlOptions;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.common.XTree;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.object.LiteralValues;
import com.tc.stats.DSOClassInfo;
import com.terracottatech.config.InstrumentedClasses;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;

import javax.swing.KeyStroke;

public class ClassesPanel extends XContainer {
  private ApplicationContext    appContext;
  private IClusterModel         clusterModel;
  private ClassesTable          table;
  private XTree                 tree;
  private ClassesTreeMap        treeMap;
  private XTextArea             configText;
  private static XmlOptions     xmlOpts;

  private static final String   REFRESH           = "Refresh";

  private static final String[] IGNORE_CLASS_LIST = { "com.tcclient", "java." };

  private static LiteralValues  LITERALS          = new LiteralValues();

  static {
    xmlOpts = new XmlOptions();
    xmlOpts.setSavePrettyPrint();
    xmlOpts.setSavePrettyPrintIndent(2);
  }

  public ClassesPanel(ApplicationContext appContext, IClusterModel clusterModel) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;

    XTabbedPane tabbedPane = new XTabbedPane();

    table = new ClassesTable(new ClassTableModel(appContext));
    tabbedPane.addTab(appContext.getString("classes.tabular"), new XScrollPane(table));

    tabbedPane.addTab(appContext.getString("classes.hierarchical"), new XScrollPane(tree = new XTree()));
    tree.setShowsRootHandles(true);
    tree.setModel(new ClassTreeModel(appContext, new DSOClassInfo[] {}));

    tabbedPane.addTab(appContext.getString("classes.map"), treeMap = new ClassesTreeMap());
    treeMap.setModel((ClassTreeModel) tree.getModel());

    XContainer configPanel = new XContainer(new BorderLayout());
    XTextArea configDescriptionText = new XTextArea();
    configDescriptionText.setText(appContext.getString("dso.classes.config.desc"));
    configDescriptionText.setEditable(false);
    configPanel.add(configDescriptionText, BorderLayout.NORTH);
    configPanel.add(new XScrollPane(configText = new XTextArea()));
    tabbedPane.addTab(appContext.getString("classes.config.snippet"), configPanel);

    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true);
    getActionMap().put(REFRESH, new RefreshAction());
    getInputMap().put(ks, REFRESH);

    add(tabbedPane);

    init();
  }

  private void init() {
    if (appContext == null) return;
    appContext.execute(new InitWorker());
  }

  private class InitWorker extends BasicWorker<DSOClassInfo[]> {
    private InitWorker() {
      super(new Callable<DSOClassInfo[]>() {
        public DSOClassInfo[] call() throws Exception {
          return getClassInfos();
        }
      });
    }

    protected void finished() {
      Exception e = getException();
      if (e == null) {
        DSOClassInfo[] classInfo = getResult();
        table.setClassInfo(classInfo);
        ((ClassTreeModel) tree.getModel()).setClassInfo(classInfo);
        treeMap.setModel((ClassTreeModel) tree.getModel());
        updateConfigText();
      }
    }
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  private synchronized IServer getActiveCoordinator() {
    IClusterModel theClusterModel = getClusterModel();
    return theClusterModel != null ? theClusterModel.getActiveCoordinator() : null;
  }

  private DSOClassInfo[] getClassInfos() {
    ArrayList<DSOClassInfo> list = new ArrayList<DSOClassInfo>();
    IServer activeCoord = getActiveCoordinator();

    if (activeCoord != null) {
      DSOClassInfo[] classInfo = activeCoord.getClassInfo();
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
            for (int j = 0; j < i; j++) {
              sb.append("[]");
            }
            className = sb.toString();
          }
          list.add(new DSOClassInfo(className, info.getInstanceCount()));
        }
      }
    }

    return list.toArray(new DSOClassInfo[0]);
  }

  private void updateConfigText() {
    DSOClassInfo[] classInfo = ((ClassTreeModel) tree.getModel()).getClassInfo();
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
    configText.setText(config.xmlText(xmlOpts));
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
    init();
  }

  public void tearDown() {
    synchronized (this) {
      appContext = null;
      clusterModel = null;
      table = null;
      tree = null;
      treeMap = null;
      configText = null;
    }
    super.tearDown();
  }
}
