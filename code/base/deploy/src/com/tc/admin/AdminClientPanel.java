/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.dijon.CheckBox;
import org.dijon.Container;
import org.dijon.Dialog;
import org.dijon.FrameResource;
import org.dijon.Label;
import org.dijon.ScrollPane;
import org.dijon.Separator;
import org.dijon.TextArea;
import org.dijon.TextPane;
import org.dijon.UndoMonger;

import com.tc.admin.common.AboutDialog;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.ContactTerracottaAction;
import com.tc.admin.common.IComponentProvider;
import com.tc.admin.common.PrefsHelper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XMenu;
import com.tc.admin.common.XMenuBar;
import com.tc.admin.common.XMenuItem;
import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTextField;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IServer;
import com.tc.admin.model.ServerVersion;
import com.tc.util.ProductInfo;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
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
  private AdminClientContext           m_acc;
  private NavTree                      m_tree;
  private XContainer                   m_nodeView;
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

  public static final String           UNDO = "Undo";
  public static final String           REDO = "Redo";

  public AdminClientPanel() {
    super();

    m_acc = AdminClient.getContext();
    m_acc.setController(this);

    FrameResource frameRes = m_acc.getTopRes().getFrame("MyFrame");
    load(frameRes.getContentPane());

    XSplitPane mainSplitter = (XSplitPane) findComponent("MainSplitter");
    mainSplitter.setPreferences(getPreferences().node(mainSplitter.getName()));

    XSplitPane leftSplitter = (XSplitPane) findComponent("LeftSplitter");
    leftSplitter.setPreferences(getPreferences().node(leftSplitter.getName()));

    m_tree = (NavTree) findComponent("Tree");
    m_nodeView = (XContainer) findComponent("NodeView");
    m_bottomPane = (XTabbedPane) findComponent("BottomPane");
    m_logArea = (LogPane) m_bottomPane.findComponent("LogArea");
    m_statusLine = (XTextField) findComponent("StatusLine");
    m_activityArea = (Container) findComponent("ActivityArea");

    m_nodeView.setLayout(new BorderLayout());

    m_tree.addMouseListener(new NavTreeMouseListener());
    m_tree.addTreeSelectionListener(new NavTreeSelectionListener());

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

    StatusCleaner statusCleaner = new StatusCleaner();
    addMouseListener(statusCleaner);
    addKeyListener(statusCleaner);

    getActionMap().put(UNDO, m_undoCmd = new UndoAction());
    getActionMap().put(REDO, m_redoCmd = new RedoAction());

    initNavTreeMenu();
  }

  private class NavTreeMouseListener extends MouseAdapter {
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
  }

  private class NavTreeSelectionListener implements TreeSelectionListener {
    public void valueChanged(TreeSelectionEvent tse) {
      TreePath path = tse.getNewLeadSelectionPath();

      m_nodeView.removeAll();

      if (path != null) {
        m_tree.requestFocus();
        XTreeNode node = (XTreeNode) path.getLastPathComponent();
        if (node != null) {
          node.nodeSelected(tse);
          if (node instanceof IComponentProvider) {
            java.awt.Component comp = ((IComponentProvider) node).getComponent();
            if (comp != null) {
              m_nodeView.add(comp);
            }
          }
        }
      }

      m_nodeView.revalidate();
      m_nodeView.repaint();
    }
  }

  private class StatusCleaner implements MouseListener, KeyListener, Serializable {
    public void mouseClicked(MouseEvent e) {
      setStatus(null);
    }

    public void keyPressed(KeyEvent e) {
      setStatus(null);
    }

    public void keyTyped(KeyEvent e) {
      /**/
    }

    public void keyReleased(KeyEvent e) {
      /**/
    }

    public void mouseEntered(MouseEvent e) {
      /**/
    }

    public void mouseExited(MouseEvent e) {
      /**/
    }

    public void mousePressed(MouseEvent e) {
      /**/
    }

    public void mouseReleased(MouseEvent e) {
      /**/
    }
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

  public void initMenubar(XMenuBar menuBar) {
    XMenu menu = new XMenu(m_acc.getMessage("file.menu.label"));

    menu.add(m_newClusterAction = new NewClusterAction());
    menu.add(new JSeparator());
    menu.add(new QuitAction());

    menuBar.add(menu);

    menu = new XMenu(m_acc.getMessage("help.menu.label"));
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
      if (kitID == null || com.tc.util.ProductInfo.UNKNOWN_VALUE.equals(kitID)) {
        if ((kitID = System.getProperty("com.tc.kitID")) == null) {
          kitID = "42.0";
        }
      }
      url = "http://www.terracotta.org/kit/reflector?kitID=" + kitID + "&pageID=ConsoleGuide";
    }

    public void actionPerformed(ActionEvent ae) {
      block();
      BrowserLauncher.openURL(url);
      unblock();
    }
  }

  public boolean isExpanded(XTreeNode node) {
    return node != null && node.getParent() != null && m_tree.isExpanded(new TreePath(node.getPath()));
  }

  public void expand(XTreeNode node) {
    if (node != null && node.getParent() != null) {
      m_tree.expandPath(new TreePath(node.getPath()));
    }
  }

  public boolean isSelected(XTreeNode node) {
    return node != null && node.getParent() != null && m_tree.isPathSelected(new TreePath(node.getPath()));
  }

  public void select(XTreeNode node) {
    if (node != null && node.getParent() != null) {
      m_tree.requestFocus();
      m_tree.setSelectionPath(new TreePath(node.getPath()));
    }
  }

  public void remove(XTreeNode node) {
    XTreeNode origNode = node;
    if (node != null && node.getParent() != null) {
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
    return m_acc.getPrefs().node("AdminClientFrame");
  }

  protected void storePreferences() {
    AdminClient.getContext().storePrefs();
  }

  public void updateServerPrefs() {
    XRootNode root = m_tree.getRootNode();
    int count = root.getChildCount();
    AdminClientContext acc = AdminClient.getContext();
    PrefsHelper helper = PrefsHelper.getHelper();
    Preferences prefs = acc.getPrefs().node("AdminClient");
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
          clusterNode.disconnect();
        }
      }
    }

    root.tearDown();
    storePreferences();
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

  class NewClusterAction extends XAbstractAction {
    NewClusterAction() {
      super(m_acc.getMessage("new.cluster.action.label"));
    }

    public void actionPerformed(ActionEvent ae) {
      AdminClientContext acc = AdminClient.getContext();
      XTreeModel model = (XTreeModel) m_tree.getModel();
      XTreeNode root = (XTreeNode) model.getRoot();
      int index = root.getChildCount();
      ClusterNode clusterNode = acc.getNodeFactory().createClusterNode();

      model.insertNodeInto(clusterNode, root, index);
      TreePath path = new TreePath(clusterNode.getPath());
      m_tree.makeVisible(path);
      m_tree.setSelectionPath(path);

      PrefsHelper helper = PrefsHelper.getHelper();
      Preferences prefs = acc.getPrefs().node("AdminClient");
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
      String msg = m_acc.getMessage("stats.active.recording.msg");
      Frame frame = (Frame) getAncestorOfClass(Frame.class);
      int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
      return answer == JOptionPane.OK_OPTION;
    }

    return true;
  }

  class QuitAction extends XAbstractAction {
    QuitAction() {
      super(m_acc.getMessage("quit.action.label"));
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_SHORTCUT_KEY_MASK, true));
    }

    public void actionPerformed(ActionEvent ae) {
      m_acc.storePrefs();
      if (testWarnCurrentRecordingSessions()) {
        Runtime.getRuntime().exit(0);
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

    ProductInfo consoleInfo = ProductInfo.getInstance();
    String consoleVersion = consoleInfo.version();
    ServerVersion serverInfo = clusterNode.getProductInfo();
    if (serverInfo == null) return true; // something went wrong, move on
    String serverVersion = serverInfo.version();
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

    ProductInfo consoleInfo = ProductInfo.getInstance();
    String consoleVersion = consoleInfo.version();
    String serverVersion = null;
    try {
      serverVersion = serverNode.getProductVersion();
    } catch (Exception e) {
      // connection probably lost...
    }
    // ...don't interfere with connection messaging
    if (serverVersion == null) return true;

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
    Label label = new Label(m_acc.format("version.check.message", clusterNode, serverVersion, consoleVersion));
    Container panel = new Container();
    panel.setLayout(new BorderLayout());
    panel.add(label);
    CheckBox versionCheckToggle = new CheckBox(m_acc.getMessage("version.check.disable.label"));
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
    Label label = new Label(m_acc.format("version.check.message", serverNode, serverVersion, consoleVersion));
    Container panel = new Container();
    panel.setLayout(new BorderLayout());
    panel.add(label);
    CheckBox versionCheckToggle = new CheckBox(m_acc.getMessage("version.check.disable.label"));
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
      super(m_acc.getMessage("update-checker.control.label"));
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
    sb.append(URLEncoder.encode(productInfo.version()));
    sb.append("&tc-product=");
    sb.append(productInfo.license().equals(ProductInfo.DEFAULT_LICENSE) ? "oss" : "ee");
    sb.append("&source=console");

    return new URL(sb.toString());
  }

  Preferences getUpdateCheckerPrefs() {
    return getPreferences().node("update-checker");
  }

  class UpdateCheckerAction extends XAbstractAction {
    ProductInfo m_productInfo;

    UpdateCheckerAction() {
      super(m_acc.getMessage("update-checker.action.label"));

      if (isEnabled()) {
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

    public void actionPerformed(ActionEvent e) {
      UpdateCheckerAction.this.startUpdateCheck();
    }

    ProductInfo getProductInfo() {
      if (m_productInfo == null) {
        m_productInfo = ProductInfo.getInstance();
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
      textArea.setLineWrap(true);
      textArea.setWrapStyleWord(true);
      ScrollPane scrollPane = new ScrollPane(textArea);
      JOptionPane.showMessageDialog(AdminClientPanel.this, scrollPane, m_acc.getMessage("update-checker.action.title"),
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
            String version = getProductInfo().version();

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
                    sb.append(m_acc.getMessage("update-checker.release-notes.label"));
                    sb.append("</a>");
                  }
                  sb.append("</li>\n");
                }
                sb.append("</ol>");
              }
            }
            if (sb.length() > 0) {
              sb.insert(0, "<html><body><p>" + m_acc.getMessage("update-checker.updates.available.msg") + "</p>");
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
          } catch (RuntimeException re) {
            // whatever the problem, don't bother user
            result = null;
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
            JOptionPane.showMessageDialog(AdminClientPanel.this, msg, m_acc.getMessage("update-checker.action.title"),
                                          JOptionPane.INFORMATION_MESSAGE);
          }
        });
      }
    }
  }

  class VersionCheckControlAction extends XAbstractAction {
    VersionCheckControlAction() {
      super(m_acc.getMessage("version.check.enable.label"));
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
      super(m_acc.getMessage("about.action.label"));
    }

    public void actionPerformed(ActionEvent ae) {
      Frame frame = getFrame();
      if (m_aboutDialog == null) {
        m_aboutDialog = new AboutDialog(frame);
      }

      m_aboutDialog.pack();
      m_aboutDialog.center(frame);
      m_aboutDialog.setVisible(true);
    }
  }

  public void addServerLog(IServer server) {
    if (hasServerLog(server)) return;
    ServerLog log = new ServerLog(server);
    JScrollPane scroller = new JScrollPane(log);
    int index = m_bottomPane.getTabCount();

    m_bottomPane.addTab(server.toString(), (Icon) null, scroller, null);
    m_bottomPane.setSelectedIndex(index);

    LogDocumentListener ldl = new LogDocumentListener(log);
    log.getDocument().addDocumentListener(ldl);
    m_logListeners.add(ldl);
  }

  private boolean hasServerLog(IServer server) {
    JScrollPane scroller;
    ServerLog log;
    for (int i = 1; i < m_bottomPane.getTabCount(); i++) {
      scroller = (JScrollPane) m_bottomPane.getComponentAt(i);
      log = (ServerLog) scroller.getViewport().getView();
      if (server.equals(log.getServer())) { return true; }
    }
    return false;
  }

  public void removeServerLog(IServer server) {
    JScrollPane scroller;
    ServerLog log;
    LogDocumentListener ldl;

    for (int i = 1; i < m_bottomPane.getTabCount(); i++) {
      scroller = (JScrollPane) m_bottomPane.getComponentAt(i);
      log = (ServerLog) scroller.getViewport().getView();

      if (server.equals(log.getServer())) {
        ldl = (LogDocumentListener) m_logListeners.remove(i);
        log.getDocument().removeDocumentListener(ldl);
        m_bottomPane.removeTabAt(i);
        m_bottomPane.setIconAt(m_bottomPane.getSelectedIndex(), null);
        log.tearDown();
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

  private static class MyUndoManager extends UndoManager {
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
