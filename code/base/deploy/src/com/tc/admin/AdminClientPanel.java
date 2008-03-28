/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dijon.Button;
import org.dijon.CheckBox;
import org.dijon.Container;
import org.dijon.Dialog;
import org.dijon.DialogResource;
import org.dijon.FrameResource;
import org.dijon.Label;
import org.dijon.ScrollPane;
import org.dijon.Separator;
import org.dijon.TextArea;
import org.dijon.TextPane;
import org.dijon.UndoMonger;

import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.ContactTerracottaAction;
import com.tc.admin.common.PrefsHelper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XMenu;
import com.tc.admin.common.XMenuBar;
import com.tc.admin.common.XMenuItem;
import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTextField;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.util.Assert;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTML;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class AdminClientPanel extends XContainer implements AdminClientController, UndoMonger {
  private NavTree                      m_tree;
  private XContainer                   m_nodeView;
  private JSplitPane                   m_mainSplitter;
  private Integer                      m_mainDivLoc;
  private JSplitPane                   m_leftSplitter;
  private Integer                      m_leftDivLoc;
  private DividerListener              m_dividerListener;
  private XTabbedPane                  m_bottomPane;
  private LogPane                      m_logArea;
  private ArrayList                    m_logListeners;
  private Icon                         m_infoIcon;
  private XTextField                   m_statusLine;
  private Container                    m_activityArea;
  protected UndoAction                 m_undoCmd;
  protected RedoAction                 m_redoCmd;
  protected UndoManager                m_undoManager;
  protected NewClusterAction           m_newClusterAction;
  protected HelpAction                 m_helpAction;
  protected UpdateCheckerControlAction m_updateCheckerControlAction;
  protected JCheckBoxMenuItem          m_updateCheckerToggle;
  protected UpdateCheckerAction        m_updateCheckerAction;
  protected VersionCheckControlAction  m_versionCheckAction;
  protected JCheckBoxMenuItem          m_versionCheckToggle;
  protected AboutAction                m_aboutAction;

  public static final String           UNDO            = "Undo";
  public static final String           REDO            = "Redo";

  protected MouseAdapter               m_statusCleaner = new MouseAdapter() {
                                                         public void mouseClicked(MouseEvent e) {
                                                           setStatus(null);
                                                         }
                                                       };

  public AdminClientPanel() {
    super();

    AdminClientContext acc = AdminClient.getContext();

    acc.controller = this;

    FrameResource frameRes = acc.topRes.getFrame("MyFrame");
    load(frameRes.getContentPane());

    m_tree = (NavTree) findComponent("Tree");
    m_nodeView = (XContainer) findComponent("NodeView");
    m_bottomPane = (XTabbedPane) findComponent("BottomPane");
    m_logArea = (LogPane) m_bottomPane.findComponent("LogArea");
    m_statusLine = (XTextField) findComponent("StatusLine");
    m_activityArea = (Container) findComponent("ActivityArea");

    m_nodeView.setLayout(new BorderLayout());

    m_tree.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent me) {
        TreePath path = m_tree.getPathForLocation(me.getX(), me.getY());

        if (path != null) {
          m_tree.requestFocus();

          XTreeNode node = (XTreeNode) path.getLastPathComponent();
          if (node != null) {
            select(node);
          }
        }
      }

      public void mouseClicked(MouseEvent me) {
        TreePath path = m_tree.getPathForLocation(me.getX(), me.getY());

        if (path != null) {
          m_tree.requestFocus();

          XTreeNode node = (XTreeNode) path.getLastPathComponent();
          if (node != null) {
            node.nodeClicked(me);
          }
        }
      }
    });

    m_tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent tse) {
        TreePath path = tse.getNewLeadSelectionPath();

        m_nodeView.removeAll();

        if (path != null) {
          m_tree.requestFocus();

          XTreeNode node = (XTreeNode) path.getLastPathComponent();
          if (node != null) {
            node.nodeSelected(tse);

            if (node instanceof ComponentNode) {
              ComponentNode cnode = (ComponentNode) node;
              java.awt.Component comp = (java.awt.Component) cnode.getComponent();

              if (comp != null) {
                m_nodeView.add(comp);
              }
            }
          }
        }

        m_nodeView.revalidate();
        m_nodeView.repaint();
      }
    });

    m_infoIcon = LogHelper.getHelper().getInfoIcon();

    m_logListeners = new ArrayList();
    LogDocumentListener ldl = new LogDocumentListener(m_logArea);
    m_logListeners.add(ldl);
    m_logArea.getDocument().addDocumentListener(ldl);

    m_bottomPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        int index = m_bottomPane.getSelectedIndex();
        m_bottomPane.setIconAt(index, null);
      }
    });

    m_tree.setModel(new NavTreeModel());

    setHelpPath("/com/tc/admin/AdminClient.html");

    addMouseListener(m_statusCleaner);

    addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        setStatus(null);
      }
    });

    getActionMap().put(UNDO, m_undoCmd = new UndoAction());
    getActionMap().put(REDO, m_redoCmd = new RedoAction());

    initNavTreeMenu();
  }

  protected NewClusterAction getNewClusterAction() {
    if (m_newClusterAction == null) {
      m_newClusterAction = new NewClusterAction();
    }
    return m_newClusterAction;
  }

  protected HelpAction getHelpAction() {
    if (m_helpAction == null) {
      m_helpAction = new HelpAction();
    }
    return m_helpAction;
  }

  protected AboutAction getAboutAction() {
    if (m_aboutAction == null) {
      m_aboutAction = new AboutAction();
    }
    return m_aboutAction;
  }

  protected void initNavTreeMenu() {
    JPopupMenu popup = new JPopupMenu("ProjectTree Actions");

    popup.add(getNewClusterAction());
    popup.add(new Separator());
    popup.add(getHelpAction());
    if (shouldAddAboutItem()) {
      popup.add(getAboutAction());
    }

    m_tree.setPopupMenu(popup);
  }

  private String getBundleString(String key) {
    return AdminClient.getContext().getMessage(key);
  }

  private String formatBundleString(String key, Object[] args) {
    return AdminClient.getContext().format(key, args);
  }

  private String formatBundleString(String key, Object arg) {
    return formatBundleString(key, new Object[] { arg });
  }

  public void initMenubar(XMenuBar menuBar) {
    XMenu menu = new XMenu(getBundleString("file.menu.label"));

    menu.add(m_newClusterAction = new NewClusterAction());
    menu.add(new JSeparator());
    menu.add(new QuitAction());

    menuBar.add(menu);

    menu = new XMenu(getBundleString("help.menu.label"));
    XMenuItem mitem = new XMenuItem("AdminConsole Help", HelpHelper.getHelper().getHelpIcon());
    mitem.setAction(m_helpAction = new HelpAction());
    menu.add(mitem);
    menu.addSeparator();
    menu.add(new ContactTerracottaAction("Visit Terracotta Forums", "http://www.terracottatech.com/forums/"));
    menu.add(new ContactTerracottaAction("Contact Terracotta Technical Support",
                                         "http://www.terracottatech.com/support_services.shtml"));
    menu.addSeparator();

    m_updateCheckerControlAction = new UpdateCheckerControlAction();
    m_updateCheckerToggle = new JCheckBoxMenuItem(m_updateCheckerControlAction);
    m_updateCheckerToggle.setSelected(m_updateCheckerControlAction.isUpdateCheckEnabled());
    m_updateCheckerToggle.addActionListener(new UpdateCheckerControlHandler());
    menu.add(m_updateCheckerToggle);
    m_updateCheckerAction = new UpdateCheckerAction();

    m_versionCheckAction = new VersionCheckControlAction();
    m_versionCheckToggle = new JCheckBoxMenuItem(m_versionCheckAction);
    m_versionCheckToggle.setSelected(m_versionCheckAction.isVersionCheckEnabled());
    menu.add(m_versionCheckToggle);
    menu.add(m_aboutAction = new AboutAction());

    menuBar.add(menu);
  }

  class HelpAction extends XAbstractAction {
    String url;

    HelpAction() {
      super("AdminConsole Help");
      String kitID = com.tc.util.ProductInfo.getInstance().kitID();
      Assert.assertNotNull(kitID);
      url = "http://www.terracotta.org/kit/reflector?kitID=" + kitID + "&pageID=ConsoleGuide";
    }

    public void actionPerformed(ActionEvent ae) {
      block();
      BrowserLauncher.openURL(url);
      unblock();
    }
  }

  public boolean isExpanded(XTreeNode node) {
    return node != null && m_tree.isExpanded(new TreePath(node.getPath()));
  }

  public void expand(XTreeNode node) {
    if (node != null) {
      m_tree.expandPath(new TreePath(node.getPath()));
    }
  }

  public boolean isSelected(XTreeNode node) {
    return node != null && m_tree.isPathSelected(new TreePath(node.getPath()));
  }

  public void select(XTreeNode node) {
    if (node != null) {
      m_tree.requestFocus();
      m_tree.setSelectionPath(new TreePath(node.getPath()));
    }
  }

  public void remove(XTreeNode node) {
    XTreeNode origNode = node;
    XTreeModel model = (XTreeModel) m_tree.getModel();
    XTreeNode parent = (XTreeNode) node.getParent();
    int index = parent.getIndex(node);
    TreePath nodePath = new TreePath(node.getPath());
    TreePath selPath = m_tree.getSelectionPath();

    model.removeNodeFromParent(node);

    if (nodePath.isDescendant(selPath)) {
      int count = parent.getChildCount();

      if (count > 0) {
        node = (XTreeNode) parent.getChildAt(index < count ? index : count - 1);
      } else {
        node = parent;
      }

      m_tree.setSelectionPath(new TreePath(node.getPath()));
    }
    origNode.tearDown();
  }

  public void nodeStructureChanged(XTreeNode node) {
    TreeModel treeModel = m_tree.getModel();

    if (treeModel instanceof XTreeModel) {
      ((XTreeModel) treeModel).nodeStructureChanged(node);
    }
  }

  public void nodeChanged(XTreeNode node) {
    TreeModel treeModel = m_tree.getModel();

    if (treeModel instanceof XTreeModel) {
      ((XTreeModel) treeModel).nodeChanged(node);
    }
  }

  protected Preferences getPreferences() {
    AdminClientContext acc = AdminClient.getContext();
    return acc.prefs.node("AdminClientFrame");
  }

  protected void storePreferences() {
    AdminClientContext acc = AdminClient.getContext();
    acc.client.storePrefs();
  }

  private int getSplitPref(JSplitPane splitter) {
    Preferences prefs = getPreferences();
    Preferences splitPrefs = prefs.node(splitter.getName());

    return splitPrefs.getInt("Split", -1);
  }

  private JSplitPane getMainSplitter() {
    if (m_mainSplitter == null) {
      m_mainSplitter = (JSplitPane) findComponent("MainSplitter");
      m_mainDivLoc = new Integer(getSplitPref(m_mainSplitter));

      if (m_dividerListener == null) {
        m_dividerListener = new DividerListener();
      }
    }

    return m_mainSplitter;
  }

  private JSplitPane getLeftSplitter() {
    if (m_leftSplitter == null) {
      m_leftSplitter = (JSplitPane) findComponent("LeftSplitter");
      m_leftDivLoc = new Integer(getSplitPref(m_leftSplitter));

      if (m_dividerListener == null) {
        m_dividerListener = new DividerListener();
      }
      Dimension emptySize = new Dimension();
      java.awt.Component left = m_leftSplitter.getLeftComponent();
      if (left != null) {
        left.setMinimumSize(emptySize);
      }
      java.awt.Component right = m_leftSplitter.getLeftComponent();
      if (right != null) {
        right.setMinimumSize(emptySize);
      }
    }

    return m_leftSplitter;
  }

  public void updateServerPrefs() {
    XRootNode root = m_tree.getRootNode();
    int count = root.getChildCount();
    AdminClientContext acc = AdminClient.getContext();
    PrefsHelper helper = PrefsHelper.getHelper();
    Preferences prefs = acc.prefs.node("AdminClient");
    Preferences serverPrefs = prefs.node(ServersHelper.SERVERS);
    Preferences serverPref;

    helper.clearChildren(serverPrefs);

    for (int i = 0; i < count; i++) {
      TreeNode node = root.getChildAt(i);
      if (node instanceof ClusterNode) {
        serverPref = serverPrefs.node("server-" + i);
        ((ClusterNode) node).setPreferences(serverPref);
      }
    }

    storePreferences();
  }

  public void disconnectAll() {
    XRootNode root = m_tree.getRootNode();
    int count = root.getChildCount();

    for (int i = 0; i < count; i++) {
      TreeNode node = root.getChildAt(i);

      if (node instanceof ClusterNode) {
        ClusterNode clusterNode = (ClusterNode) node;
        if (clusterNode.isConnected()) {
          clusterNode.disconnectOnExit();
        }
      }
    }

    root.tearDown();
    storePreferences();
  }

  private class DividerListener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent pce) {
      JSplitPane splitter = (JSplitPane) pce.getSource();
      String propName = pce.getPropertyName();

      if (splitter.isShowing() == false || JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(propName) == false) { return; }

      int divLoc = splitter.getDividerLocation();
      Integer divLocObj = new Integer(divLoc);
      Preferences prefs = getPreferences();
      String name = splitter.getName();
      Preferences node = prefs.node(name);

      node.putInt("Split", divLoc);
      storePreferences();

      if (m_mainSplitter.getName().equals(name)) {
        m_mainDivLoc = divLocObj;
      } else {
        m_leftDivLoc = divLocObj;
      }
    }
  }

  public void doLayout() {
    super.doLayout();

    JSplitPane splitter = getMainSplitter();
    if (m_mainDivLoc != null) {
      splitter.setDividerLocation(m_mainDivLoc.intValue());
    } else {
      splitter.setDividerLocation(0.7);
    }

    splitter = getLeftSplitter();
    if (m_leftDivLoc != null) {
      splitter.setDividerLocation(m_leftDivLoc.intValue());
    } else {
      splitter.setDividerLocation(0.25);
    }
  }

  public void log(String s) {
    m_logArea.append(s + System.getProperty("line.separator"));
    m_logArea.setCaretPosition(m_logArea.getDocument().getLength() - 1);
  }

  public void log(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    t.printStackTrace(pw);
    pw.close();

    log(sw.toString());
  }

  public void setStatus(String msg) {
    m_statusLine.setText(msg);
  }

  public void clearStatus() {
    setStatus("");
  }

  public Container getActivityArea() {
    return m_activityArea;
  }

  public void addNotify() {
    super.addNotify();

    getMainSplitter().addPropertyChangeListener(m_dividerListener);
    getLeftSplitter().addPropertyChangeListener(m_dividerListener);

    // TODO: what's up with this in the plugin?
    // m_tree.requestFocusInWindow();
  }

  public void removeNotify() {
    getMainSplitter().removePropertyChangeListener(m_dividerListener);
    getLeftSplitter().removePropertyChangeListener(m_dividerListener);

    super.removeNotify();
  }

  class NewClusterAction extends XAbstractAction {
    NewClusterAction() {
      super(getBundleString("new.cluster.action.label"));
    }

    public void actionPerformed(ActionEvent ae) {
      AdminClientContext acc = AdminClient.getContext();
      XTreeModel model = (XTreeModel) m_tree.getModel();
      XTreeNode root = (XTreeNode) model.getRoot();
      int index = root.getChildCount();
      ClusterNode clusterNode = acc.nodeFactory.createClusterNode();

      model.insertNodeInto(clusterNode, root, index);
      TreePath path = new TreePath(clusterNode.getPath());
      m_tree.makeVisible(path);
      m_tree.setSelectionPath(path);

      PrefsHelper helper = PrefsHelper.getHelper();
      Preferences prefs = acc.prefs.node("AdminClient");
      Preferences servers = prefs.node(ServersHelper.SERVERS);
      int count = helper.childrenNames(servers).length;

      clusterNode.setPreferences(servers.node("server-" + count));
      storePreferences();
    }
  }

  /**
   * Returns true if quit should proceed.
   */
  private boolean testWarnCurrentRecordingSessions() {
    XTreeModel model = (XTreeModel) m_tree.getModel();
    XTreeNode root = (XTreeNode) model.getRoot();
    int count = root.getChildCount();
    boolean currentlyRecording = false;

    for (int i = 0; i < count; i++) {
      ClusterNode clusterNode = (ClusterNode) root.getChildAt(i);
      if (clusterNode.haveActiveRecordingSession()) {
        currentlyRecording = true;
        break;
      }
    }

    if (currentlyRecording) {
      String msg = "There are active statistic recording sessions.  Quit anyway?";
      Frame frame = (Frame) getAncestorOfClass(Frame.class);
      int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
      return answer == JOptionPane.OK_OPTION;
    }

    return true;
  }

  class QuitAction extends XAbstractAction {
    QuitAction() {
      super(getBundleString("quit.action.label"));

      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_SHORTCUT_KEY_MASK, true));
    }

    public void actionPerformed(ActionEvent ae) {
      if (testWarnCurrentRecordingSessions()) {
        System.exit(0);
      }
    }
  }

  private java.awt.Frame getFrame() {
    return (java.awt.Frame) SwingUtilities.getAncestorOfClass(java.awt.Frame.class, this);
  }

  protected boolean shouldAddAboutItem() {
    return true;
  }

  /**
   * Returns if the major and minor elements of the passed version strings match.
   */
  private static boolean versionsMatch(String v1, String v2) {
    return v1.equals(v2);
  }

  public boolean testServerMatch(ClusterNode clusterNode) {
    if (com.tc.util.ProductInfo.getInstance().isDevMode() || m_versionCheckAction == null
        || !m_versionCheckAction.isVersionCheckEnabled()) { return true; }

    ProductInfo consoleInfo = new ProductInfo();
    String consoleVersion = consoleInfo.getVersion();
    ProductInfo serverInfo = clusterNode.getProductInfo();
    String serverVersion = serverInfo.getVersion();
    int spaceIndex = serverVersion.lastIndexOf(" ");

    // The version string that comes from the server is of the form "Terracotta 2.4", while
    // the default ProductInfo.getVersion is just the raw version number string: "2.4"

    if (spaceIndex != -1) {
      serverVersion = serverVersion.substring(spaceIndex + 1);
    }

    if (!versionsMatch(consoleVersion, serverVersion)) {
      int answer = showVersionMismatchDialog(clusterNode, consoleVersion, serverVersion);
      return (answer == JOptionPane.YES_OPTION);
    }

    return true;
  }

  public boolean testServerMatch(ServerNode serverNode) {
    if (com.tc.util.ProductInfo.getInstance().isDevMode() || m_versionCheckAction == null
        || !m_versionCheckAction.isVersionCheckEnabled()) { return true; }

    ProductInfo consoleInfo = new ProductInfo();
    String consoleVersion = consoleInfo.getVersion();
    ProductInfo serverInfo = serverNode.getProductInfo();
    String serverVersion = serverInfo.getVersion();
    int spaceIndex = serverVersion.lastIndexOf(" ");

    // The version string that comes from the server is of the form "Terracotta 2.4", while
    // the default ProductInfo.getVersion is just the raw version number string: "2.4"
    if (spaceIndex != -1) {
      serverVersion = serverVersion.substring(spaceIndex + 1);
    }

    if (!versionsMatch(consoleVersion, serverVersion)) {
      int answer = showVersionMismatchDialog(serverNode, consoleVersion, serverVersion);
      return (answer == JOptionPane.YES_OPTION);
    }

    return true;
  }

  public int showVersionMismatchDialog(ClusterNode clusterNode, String consoleVersion, String serverVersion)
      throws HeadlessException {
    Frame frame = getFrame();
    String msg = formatBundleString("version.check.message",
                                    new Object[] { clusterNode, serverVersion, consoleVersion });
    Label label = new Label(msg);
    Container panel = new Container();
    panel.setLayout(new BorderLayout());
    panel.add(label);
    CheckBox versionCheckToggle = new CheckBox(getBundleString("version.check.disable.label"));
    versionCheckToggle.setHorizontalAlignment(SwingConstants.RIGHT);
    panel.add(versionCheckToggle, BorderLayout.SOUTH);
    String title = frame.getTitle();
    JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null, null, null);

    pane.setInitialValue(null);
    pane.setComponentOrientation(frame.getComponentOrientation());

    JDialog dialog = pane.createDialog(frame, title);
    clusterNode.setVersionMismatchDialog(dialog);

    pane.selectInitialValue();
    dialog.show();
    dialog.dispose();
    clusterNode.setVersionMismatchDialog(null);

    Object selectedValue = pane.getValue();

    if (selectedValue == null) return JOptionPane.CLOSED_OPTION;
    m_versionCheckAction.setVersionCheckEnabled(!versionCheckToggle.isSelected());
    if (selectedValue instanceof Integer) { return ((Integer) selectedValue).intValue(); }

    return JOptionPane.CLOSED_OPTION;
  }

  public int showVersionMismatchDialog(ServerNode serverNode, String consoleVersion, String serverVersion)
      throws HeadlessException {
    Frame frame = getFrame();
    String msg = formatBundleString("version.check.message", new Object[] { serverNode, serverVersion, consoleVersion });
    Label label = new Label(msg);
    Container panel = new Container();
    panel.setLayout(new BorderLayout());
    panel.add(label);
    CheckBox versionCheckToggle = new CheckBox(getBundleString("version.check.disable.label"));
    versionCheckToggle.setHorizontalAlignment(SwingConstants.RIGHT);
    panel.add(versionCheckToggle, BorderLayout.SOUTH);
    String title = frame.getTitle();
    JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null, null, null);

    pane.setInitialValue(null);
    pane.setComponentOrientation(frame.getComponentOrientation());

    JDialog dialog = pane.createDialog(frame, title);
    serverNode.setVersionMismatchDialog(dialog);

    pane.selectInitialValue();
    dialog.show();
    dialog.dispose();
    serverNode.setVersionMismatchDialog(null);

    Object selectedValue = pane.getValue();

    if (selectedValue == null) return JOptionPane.CLOSED_OPTION;
    m_versionCheckAction.setVersionCheckEnabled(!versionCheckToggle.isSelected());
    if (selectedValue instanceof Integer) { return ((Integer) selectedValue).intValue(); }

    return JOptionPane.CLOSED_OPTION;
  }

  class UpdateCheckerControlHandler implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      m_updateCheckerControlAction.setUpdateCheckEnabled(m_updateCheckerToggle.isSelected());
    }
  }

  class UpdateCheckerControlAction extends XAbstractAction {
    UpdateCheckerControlAction() {
      super(getBundleString("update-checker.control.label"));
    }

    boolean isUpdateCheckEnabled() {
      return getUpdateCheckerPrefs().getBoolean("checking-enabled", true);
    }

    void setUpdateCheckEnabled(boolean checkEnabled) {
      getUpdateCheckerPrefs().putBoolean("checking-enabled", checkEnabled);
      m_updateCheckerToggle.setSelected(checkEnabled);
      m_updateCheckerAction.setEnabled(checkEnabled);
      storePreferences();
    }

    public void actionPerformed(ActionEvent ae) {
      JCheckBoxMenuItem cbItem = (JCheckBoxMenuItem) ae.getSource();
      getUpdateCheckerPrefs().putBoolean("checking-enabled", cbItem.isSelected());
      storePreferences();
    }
  }

  public static URL constructCheckURL(ProductInfo productInfo, int id) throws MalformedURLException {
    String defaultPropsUrl = "http://www.terracotta.org/kit/reflector?kitID=default&pageID=update.properties";
    String propsUrl = System.getProperty("terracotta.update-checker.url", defaultPropsUrl);
    StringBuffer sb = new StringBuffer(propsUrl);

    sb.append(defaultPropsUrl.equals(propsUrl) ? '&' : '?');

    sb.append("id=");
    sb.append(URLEncoder.encode(Integer.toString(id)));
    sb.append("&os-name=");
    sb.append(URLEncoder.encode(System.getProperty("os.name")));
    sb.append("&jvm-name=");
    sb.append(URLEncoder.encode(System.getProperty("java.vm.name")));
    sb.append("&jvm-version=");
    sb.append(URLEncoder.encode(System.getProperty("java.vm.version")));
    sb.append("&platform=");
    sb.append(URLEncoder.encode(System.getProperty("os.arch")));
    sb.append("&tc-version=");
    sb.append(URLEncoder.encode(productInfo.getVersion()));
    sb.append("&tc-product=");
    sb.append(productInfo.getLicense().equals(ProductInfo.DEFAULT_LICENSE) ? "oss" : "ee");
    sb.append("&source=console");

    return new URL(sb.toString());
  }

  Preferences getUpdateCheckerPrefs() {
    return getPreferences().node("update-checker");
  }

  class UpdateCheckerAction extends XAbstractAction {
    ProductInfo          m_productInfo;
    private final String NEXT_CHECK_TIME_PREF_KEY = "next-check-time";
    private final String LAST_CHECK_TIME_PREF_KEY = "last-check-time";

    UpdateCheckerAction() {
      super(getBundleString("update-checker.action.label"));

      Logger.getLogger("org.apache.commons.httpclient.HttpClient").setLevel(Level.OFF);
      Logger.getLogger("org.apache.commons.httpclient.params.DefaultHttpParams").setLevel(Level.OFF);
      Logger.getLogger("org.apache.commons.httpclient.methods.GetMethod").setLevel(Level.OFF);
      Logger.getLogger("org.apache.commons.httpclient.HttpMethodDirector").setLevel(Level.OFF);
      Logger.getLogger("org.apache.commons.httpclient.HttpConnection").setLevel(Level.OFF);
      Logger.getLogger("org.apache.commons.httpclient.HttpMethodBase").setLevel(Level.OFF);
      Logger.getLogger("org.apache.commons.httpclient.HttpState").setLevel(Level.OFF);
      Logger.getLogger("org.apache.commons.httpclient.HttpParser").setLevel(Level.OFF);
      Logger.getLogger("org.apache.commons.httpclient.cookie.CookieSpec").setLevel(Level.OFF);
      Logger.getLogger("httpclient.wire.header").setLevel(Level.OFF);
      Logger.getLogger("httpclient.wire.content").setLevel(Level.OFF);

      if (!isEnabled()) return;

      Preferences updateCheckerPrefs = getUpdateCheckerPrefs();
      long nextCheckTime = updateCheckerPrefs.getLong(NEXT_CHECK_TIME_PREF_KEY, 0L);

      if (nextCheckTime == 0L) {
        updateCheckerPrefs.putLong(NEXT_CHECK_TIME_PREF_KEY, nextCheckTime());
        storePreferences();
      } else if (nextCheckTime < System.currentTimeMillis()) {
        AdminClientPanel.this.addHierarchyListener(new HierarchyListener() {
          public void hierarchyChanged(HierarchyEvent e) {
            if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0
                && AdminClientPanel.this.isDisplayable()) {
              AdminClientPanel.this.removeHierarchyListener(this);
              SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  Timer t = new Timer(100, UpdateCheckerAction.this);
                  t.setRepeats(false);
                  t.start();
                }
              });
            }
          }
        });
      }
    }

    public void setEnabled(boolean enabled) {
      super.setEnabled(enabled);

      Preferences updateCheckerPrefs = getUpdateCheckerPrefs();
      if (enabled) {
        updateCheckerPrefs.putLong(NEXT_CHECK_TIME_PREF_KEY, nextCheckTime());
      } else {
        updateCheckerPrefs.remove(NEXT_CHECK_TIME_PREF_KEY);
      }
      storePreferences();
    }

    public void actionPerformed(ActionEvent e) {
      UpdateCheckerAction.this.startUpdateCheck();
    }

    ProductInfo getProductInfo() {
      if (m_productInfo == null) {
        m_productInfo = new ProductInfo();
      }
      return m_productInfo;
    }

    URL constructCheckURL() throws MalformedURLException {
      return AdminClientPanel.constructCheckURL(getProductInfo(), getIpAddressHash());
    }

    private int getIpAddressHash() {
      try {
        return InetAddress.getLocalHost().hashCode();
      } catch (UnknownHostException uhe) {
        return 0;
      }
    }

    void showMessage(String msg) {
      TextArea textArea = new TextArea();
      textArea.setText(msg);
      textArea.setRows(8);
      textArea.setColumns(80);
      textArea.setEditable(false);
      ScrollPane scrollPane = new ScrollPane(textArea);
      JOptionPane.showMessageDialog(AdminClientPanel.this, scrollPane, getBundleString("update-checker.action.title"),
                                    JOptionPane.INFORMATION_MESSAGE);
    }

    public Properties getResponseBody(URL url, HttpClient client) throws ConnectException, IOException {
      GetMethod get = new GetMethod(url.toString());

      get.setFollowRedirects(true);
      try {
        int status = client.executeMethod(get);
        if (status != HttpStatus.SC_OK) { throw new ConnectException(
                                                                     "The http client has encountered a status code other than ok for the url: "
                                                                         + url + " status: "
                                                                         + HttpStatus.getStatusText(status)); }
        Properties props = new Properties();
        props.load(get.getResponseBodyAsStream());
        return props;
      } finally {
        get.releaseConnection();
      }
    }

    void startUpdateCheck() {
      Thread t = new Thread() {
        public void run() {
          InputStream is = null;
          Object result = null;

          try {
            StringBuffer sb = new StringBuffer();
            String version = getProductInfo().getVersion();
            if (version.indexOf('.') != -1) {
              URL url = constructCheckURL();
              HttpClient httpClient = new HttpClient();
              Properties props = getResponseBody(url, httpClient);

              String propVal = props.getProperty("general.notice");
              if (propVal != null && (propVal = propVal.trim()) != null && propVal.length() > 0) {
                showMessage(propVal);
              }

              propVal = props.getProperty(version + ".notice");
              if (propVal != null && (propVal = propVal.trim()) != null && propVal.length() > 0) {
                showMessage(propVal);
              }

              propVal = props.getProperty(version + ".updates");
              if (propVal != null && (propVal = propVal.trim()) != null && propVal.length() > 0) {
                sb.append("<ul>");
                StringTokenizer st = new StringTokenizer(propVal, ",");
                while (st.hasMoreElements()) {
                  sb.append("<li>");
                  String newVersion = st.nextToken();
                  sb.append(newVersion);

                  propVal = props.getProperty(newVersion + ".release-notes");
                  if (propVal != null && (propVal = propVal.trim()) != null && propVal.length() > 0) {
                    sb.append(" -- <a href=\"");
                    sb.append(propVal);
                    sb.append("\">");
                    sb.append(getBundleString("update-checker.release-notes.label"));
                    sb.append("</a>");
                  }
                  sb.append("</li>\n");
                }
                sb.append("</ol>");
              }
            }
            if (sb.length() > 0) {
              sb.insert(0, "<html><body><p>" + getBundleString("update-checker.updates.available.msg") + "</p>");
              sb.append("</body></html>");
              TextPane textPane = new TextPane();
              textPane.setEditable(false);
              textPane.setContentType("text/html");
              textPane.setBackground(null);
              textPane.setText(sb.toString());
              textPane.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                  if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    Element elem = e.getSourceElement();
                    AttributeSet a = elem.getAttributes();
                    AttributeSet anchor = (AttributeSet) a.getAttribute(HTML.Tag.A);
                    String action = (String) anchor.getAttribute(HTML.Attribute.HREF);
                    BrowserLauncher.openURL(action);
                  }
                }
              });
              result = textPane;
            }
          } catch (Exception e) {
            // whatever the problem, don't bother user
            result = null;
          } finally {
            IOUtils.closeQuietly(is);
          }
          UpdateCheckerAction.this.finishUpdateCheck(result);
        }
      };
      t.start();
    }

    void finishUpdateCheck(final Object msg) {
      if (msg != null) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            JOptionPane.showMessageDialog(AdminClientPanel.this, msg, getBundleString("update-checker.action.title"),
                                          JOptionPane.INFORMATION_MESSAGE);
          }
        });
      }

      final Preferences updateCheckerPrefs = getUpdateCheckerPrefs();
      updateCheckerPrefs.putLong(NEXT_CHECK_TIME_PREF_KEY, nextCheckTime());
      updateCheckerPrefs.putLong(LAST_CHECK_TIME_PREF_KEY, System.currentTimeMillis());
      storePreferences();
    }

    long nextCheckTime() {
      long currentTime = System.currentTimeMillis();
      Long minutes = Long.getLong("terracotta.update-checker.next-check-minutes");
      long nextCheckTime;

      if (minutes != null) {
        nextCheckTime = currentTime + (minutes.longValue() * 60 * 1000);
      } else {
        nextCheckTime = currentTime + (14 * 24 * 60 * 60 * 1000);
      }

      return nextCheckTime;
    }
  }

  class VersionCheckControlAction extends XAbstractAction {
    VersionCheckControlAction() {
      super(getBundleString("version.check.enable.label"));
    }

    boolean isVersionCheckEnabled() {
      Preferences versionCheckPrefs = getPreferences().node("version-check");
      return versionCheckPrefs.getBoolean("enabled", true);
    }

    void setVersionCheckEnabled(boolean checkEnabled) {
      Preferences versionCheckPrefs = getPreferences().node("version-check");
      versionCheckPrefs.putBoolean("enabled", checkEnabled);
      m_versionCheckToggle.setSelected(checkEnabled);
      storePreferences();
    }

    public void actionPerformed(ActionEvent ae) {
      JCheckBoxMenuItem cbItem = (JCheckBoxMenuItem) ae.getSource();
      Preferences versionCheckPrefs = getPreferences().node("version-check");
      versionCheckPrefs.putBoolean("enabled", cbItem.isSelected());
      storePreferences();
    }
  }

  class AboutAction extends XAbstractAction {
    Dialog m_aboutDialog;

    AboutAction() {
      super(getBundleString("about.action.label"));
    }

    public void actionPerformed(ActionEvent ae) {
      if (m_aboutDialog == null) {
        AdminClientContext acc = AdminClient.getContext();

        m_aboutDialog = new Dialog(getFrame(), true);
        m_aboutDialog.load((DialogResource) acc.topRes.child("AboutDialog"));

        AdminClientInfoPanel info;
        String title = getBundleString("title");
        info = (AdminClientInfoPanel) m_aboutDialog.findComponent("AdminClientInfoPanel");
        info.init(title, new ProductInfo());
        Label monikerLabel = (Label) m_aboutDialog.findComponent("MonikerLabel");
        monikerLabel.setText(title);
        Button okButton = (Button) m_aboutDialog.findComponent("OKButton");
        m_aboutDialog.getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae2) {
            m_aboutDialog.setVisible(false);
          }
        });
      }

      m_aboutDialog.pack();
      m_aboutDialog.center(AdminClientPanel.this);
      m_aboutDialog.setVisible(true);
    }
  }

  public void addServerLog(ConnectionContext cc) {
    ServerLog log = new ServerLog(cc);
    JScrollPane scroller = new JScrollPane(log);
    int index = m_bottomPane.getTabCount();

    m_bottomPane.addTab(cc.toString(), (Icon) null, scroller, null);
    m_bottomPane.setSelectedIndex(index);

    LogDocumentListener ldl = new LogDocumentListener(log);
    log.getDocument().addDocumentListener(ldl);
    m_logListeners.add(ldl);
  }

  public void removeServerLog(ConnectionContext cc) {
    JScrollPane scroller;
    ServerLog log;
    LogDocumentListener ldl;

    for (int i = 1; i < m_bottomPane.getTabCount(); i++) {
      scroller = (JScrollPane) m_bottomPane.getComponentAt(i);
      log = (ServerLog) scroller.getViewport().getView();

      if (cc.equals(log.getConnectionContext())) {
        ldl = (LogDocumentListener) m_logListeners.remove(i);
        log.getDocument().removeDocumentListener(ldl);
        m_bottomPane.removeTabAt(i);

        int index = m_bottomPane.getSelectedIndex();
        m_bottomPane.setIconAt(index, null);

        return;
      }
    }
  }

  class LogDocumentListener implements DocumentListener {
    JTextComponent textComponent;

    LogDocumentListener(JTextComponent textComponent) {
      this.textComponent = textComponent;
    }

    public void insertUpdate(DocumentEvent e) {
      int index = m_logListeners.indexOf(this);

      if (!textComponent.isShowing() && m_bottomPane.getIconAt(index) == null) {
        m_bottomPane.setIconAt(index, m_infoIcon);
      }
    }

    public void removeUpdate(DocumentEvent e) {/**/
    }

    public void changedUpdate(DocumentEvent e) {/**/
    }
  }

  public void block() {
    /**/
  }

  public void unblock() {
    /**/
  }

  public UndoManager getUndoManager() {
    if (m_undoManager == null) {
      m_undoManager = new MyUndoManager();
    }

    return m_undoManager;
  }

  class UndoAction extends XAbstractAction {
    public void actionPerformed(ActionEvent ae) {
      UndoManager undoMan = getUndoManager();
      UndoableEdit next = ((MyUndoManager) undoMan).nextUndoable();

      if (next != null) {
        undoMan.undo();
        setStatus("Undid '" + next.getPresentationName() + "'");
      }
    }

    public boolean isEnabled() {
      return getUndoManager().canUndo();
    }
  }

  class RedoAction extends XAbstractAction {
    public void actionPerformed(ActionEvent ae) {
      UndoManager undoMan = getUndoManager();
      UndoableEdit next = ((MyUndoManager) undoMan).nextRedoable();

      if (next != null) {
        undoMan.redo();
        setStatus("Redid '" + next.getPresentationName() + "'");
      }
    }

    public boolean isEnabled() {
      return getUndoManager().canRedo();
    }
  }

  class MyUndoManager extends UndoManager {
    public UndoableEdit nextUndoable() {
      return editToBeUndone();
    }

    public UndoableEdit nextRedoable() {
      return editToBeRedone();
    }
  }

  public String toString() {
    return getName();
  }

  protected void addEdit(UndoableEdit edit) {
    getUndoManager().addEdit(edit);
  }
}
