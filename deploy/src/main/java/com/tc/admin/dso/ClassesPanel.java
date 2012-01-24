/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso;

import org.apache.xmlbeans.XmlOptions;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.common.XTree;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.IServerGroup;
import com.tc.stats.api.DSOClassInfo;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

public class ClassesPanel extends XContainer {
  private final ApplicationContext    appContext;
  private final IClusterModel         clusterModel;
  private final ClusterListener       clusterListener;

  private ClassesTable                table;
  private XTree                       tree;
  private ClassesTreeMap              treeMap;
  private XTextArea                   configText;
  private static XmlOptions           xmlOpts;
  private final XContainer            mainPanel;
  private final XContainer            messagePanel;
  private XLabel                      messageLabel;

  private static final String         REFRESH    = "Refresh";

  private static final DSOClassInfo[] EMPTY_INFO = {};

  static {
    xmlOpts = new XmlOptions();
    xmlOpts.setSavePrettyPrint();
    xmlOpts.setSavePrettyPrintIndent(2);
  }

  public ClassesPanel(ApplicationContext appContext, IClusterModel clusterModel) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;

    mainPanel = createMainPanel();
    messagePanel = createMessagePanel();

    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      init();
    } else {
      add(messagePanel);
    }
  }

  private XContainer createMainPanel() {
    XContainer panel = new XContainer(new BorderLayout());
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
    configText.setEditable(false);
    tabbedPane.addTab(appContext.getString("classes.config.snippet"), configPanel);

    KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true);
    getActionMap().put(REFRESH, new RefreshAction());
    getInputMap().put(ks, REFRESH);

    panel.add(tabbedPane);
    return panel;
  }

  private XContainer createMessagePanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(messageLabel = new XLabel());
    messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
    messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    messageLabel.setFont((Font) appContext.getObject("message.label.font"));
    return panel;
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    public void handleReady() {
      if (clusterModel.isReady()) {
        init();
      } else {
        removeAll();
        add(messagePanel);
      }
    }

    @Override
    protected void handleUncaughtError(Exception e) {
      appContext.log(e);
    }
  }

  private void init() {
    removeAll();
    add(mainPanel);
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

    @Override
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

  private DSOClassInfo[] getClassInfos() {
    Map<String, Integer> map = new HashMap<String, Integer>();

    for (IServerGroup group : clusterModel.getServerGroups()) {
      IServer server = group.getActiveServer();

      if (server != null) {
        DSOClassInfo[] classInfo = server.getClassInfo();
        if (classInfo != null) {
          for (DSOClassInfo info : classInfo) {
            String className = info.getClassName();
            if (className.startsWith("com.tcclient")) {
              continue;
            }
            if (className.startsWith("[")) {
              int i = 0;
              while (className.charAt(i) == '[') {
                i++;
              }
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
            Integer instanceCount = map.get(className);
            int currentCount = instanceCount != null ? instanceCount.intValue() : 0;
            map.put(className, Integer.valueOf(info.getInstanceCount() + currentCount));
          }
        }
      }
    }

    ArrayList<DSOClassInfo> list = new ArrayList<DSOClassInfo>();
    Iterator<Map.Entry<String, Integer>> entries = map.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry<String, Integer> entry = entries.next();
      list.add(new DSOClassInfo(entry.getKey(), entry.getValue()));
    }

    return list.toArray(EMPTY_INFO);
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
    TcConfigDocument configDoc = TcConfigDocument.Factory.newInstance();
    TcConfig config = configDoc.addNewTcConfig();
    configText.setText(config.xmlText(xmlOpts));
  }

  public class RefreshAction extends XAbstractAction {
    public void actionPerformed(ActionEvent ae) {
      refresh();
    }
  }

  public void refresh() {
    init();
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    super.tearDown();
  }
}
