/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osgi.framework.Version;

import com.tc.admin.common.AboutDialog;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.ContactTerracottaAction;
import com.tc.admin.common.IComponentProvider;
import com.tc.admin.common.PrefsHelper;
import com.tc.admin.common.SyncHTMLEditorKit;
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XFrame;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XMenu;
import com.tc.admin.common.XMenuBar;
import com.tc.admin.common.XMenuItem;
import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTextArea;
import com.tc.admin.common.XTextField;
import com.tc.admin.common.XTextPane;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IProductVersion;
import com.tc.admin.options.OptionsDialog;
import com.tc.admin.options.RuntimeStatsOption;
import com.tc.util.ProductInfo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class AdminClientPanel extends XContainer implements AdminClientController, PropertyChangeListener {
  private final IAdminClientContext          adminClientContext;
  protected NavTree                          tree;
  protected XContainer                       nodeView;
  protected ClusterNode                      selectedClusterNode;
  protected LogsPanel                        logsPanel;
  protected LogPane                          logArea;
  protected XTextField                       statusLine;
  protected UndoAction                       undoCmd;
  protected RedoAction                       redoCmd;
  protected UndoManager                      undoManager;
  protected NewClusterAction                 newClusterAction;
  protected HelpAction                       helpAction;
  protected UpdateCheckerControlAction       updateCheckerControlAction;
  protected JCheckBoxMenuItem                updateCheckerToggle;
  protected UpdateCheckerAction              updateCheckerAction;
  protected VersionCheckControlAction        versionCheckAction;
  protected JCheckBoxMenuItem                versionCheckToggle;
  protected AboutAction                      aboutAction;
  protected OptionsAction                    optionsAction;
  private FeatureSelectorAction              sessionSelectorAction;
  private FeatureSelectorAction              quartzSelectorAction;
  private FeatureSelectorAction              hibernateSelectorAction;
  private FeatureSelectorAction              ehcacheSelectorAction;
  private Map<String, FeatureSelectorAction> selectorActionMap;

  public static final String                 UNDO = "Undo";
  public static final String                 REDO = "Redo";

  public AdminClientPanel(IAdminClientContext adminClientContext) {
    super(new BorderLayout());

    this.adminClientContext = adminClientContext;
    adminClientContext.setAdminClientController(this);

    XContainer leftSide = createLeftSide();
    nodeView = new XContainer(new BorderLayout());
    XSplitPane leftSplitter = new XSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSide, nodeView);
    leftSplitter.setResizeWeight(0.0);
    leftSide.setMinimumSize(leftSide.getPreferredSize());
    leftSplitter.setDefaultDividerLocation(0);
    leftSplitter.setName("LeftSplitter");
    leftSplitter.setPreferences(getPreferences().node(leftSplitter.getName()));

    logsPanel = new LogsPanel(adminClientContext);

    XSplitPane mainSplitter = new XSplitPane(JSplitPane.VERTICAL_SPLIT, leftSplitter, logsPanel);
    mainSplitter.setResizeWeight(1.0);
    mainSplitter.setDefaultDividerLocation(0.72);
    mainSplitter.setName("MainSplitter");
    mainSplitter.setPreferences(getPreferences().node(mainSplitter.getName()));

    add(mainSplitter, BorderLayout.CENTER);

    statusLine = new XTextField();
    statusLine.setEditable(false);

    add(statusLine, BorderLayout.SOUTH);

    tree.addTreeSelectionListener(new NavTreeSelectionListener());

    logArea = new LogPane();
    logsPanel.add(adminClientContext.getString("messages"), new XScrollPane(logArea));

    initNavTreeMenu();
    registerOptions();

    tree.setModel(new NavTreeModel(adminClientContext));

    StatusCleaner statusCleaner = new StatusCleaner();
    addMouseListener(statusCleaner);
    addKeyListener(statusCleaner);

    getActionMap().put(UNDO, undoCmd = new UndoAction());
    getActionMap().put(REDO, redoCmd = new RedoAction());

    XRootNode root = tree.getRootNode();
    int count = root.getChildCount();
    for (int i = 0; i < count; i++) {
      TreeNode node = root.getChildAt(i);
      if (node instanceof ClusterNode) {
        ClusterNode clusterNode = (ClusterNode) node;
        IClusterModel clusterModel = clusterNode.getClusterModel();
        if (clusterModel.isReady()) {
          addClusterLog(clusterModel);
        }
        clusterModel.addPropertyChangeListener(this);
      }
    }

    setStatus("Log file location: " + new File(System.getProperty("user.home"), ".devconsole.log.[x]"));
  }

  private XContainer createLeftSide() {
    XContainer result = new XContainer(new BorderLayout());
    XContainer buttonPanel = new XContainer(new GridLayout(1, 0));

    ehcacheSelectorAction = new FeatureSelectorAction("Ehcache", "/com/tc/admin/icons/ehcache-logo-icon.png",
                                                      "/com/tc/admin/icons/ehcache-logo-icon-disabled.png", "Ehcache");
    buttonPanel.add(ehcacheSelectorAction.createButton());

    hibernateSelectorAction = new FeatureSelectorAction("Hibernate", "/com/tc/admin/icons/hibernate-caching.png",
                                                        "/com/tc/admin/icons/hibernate-caching-disabled.png",
                                                        "Hibernate");
    buttonPanel.add(hibernateSelectorAction.createButton());

    quartzSelectorAction = new FeatureSelectorAction("Quartz", "/com/tc/admin/icons/quartz.png",
                                                     "/com/tc/admin/icons/quartz-disabled.png", "Quartz");
    buttonPanel.add(quartzSelectorAction.createButton());

    sessionSelectorAction = new FeatureSelectorAction("Sessions", "/com/tc/admin/icons/web-sessions.jpg",
                                                      "/com/tc/admin/icons/web-sessions-disabled.jpg", "Sessions");
    buttonPanel.add(sessionSelectorAction.createButton());

    result.add(buttonPanel, BorderLayout.NORTH);
    result.add(new XScrollPane(tree = new NavTree()), BorderLayout.CENTER);
    result.setMinimumSize(result.getPreferredSize());

    selectorActionMap = new HashMap<String, FeatureSelectorAction>();
    selectorActionMap.put(sessionSelectorAction.getName(), sessionSelectorAction);
    selectorActionMap.put(quartzSelectorAction.getName(), quartzSelectorAction);
    selectorActionMap.put(hibernateSelectorAction.getName(), hibernateSelectorAction);
    selectorActionMap.put(ehcacheSelectorAction.getName(), ehcacheSelectorAction);

    return result;
  }

  public void activeFeatureAdded(String featureName) {
    FeatureSelectorAction fse = selectorActionMap.get(featureName);
    if (fse != null) {
      fse.setEnabled(true);
    }
  }

  public void activeFeatureRemoved(String featureName) {
    FeatureSelectorAction fse = selectorActionMap.get(featureName);
    if (fse != null) {
      fse.setEnabled(false);
    }
  }

  private void updateFeatureSelectors() {
    if (selectedClusterNode != null) {
      sessionSelectorAction.setEnabled(selectedClusterNode.findNodeByName("Sessions") != null);
      quartzSelectorAction.setEnabled(selectedClusterNode.findNodeByName("Quartz") != null);
      hibernateSelectorAction.setEnabled(selectedClusterNode.findNodeByName("Hibernate") != null);
      ehcacheSelectorAction.setEnabled(selectedClusterNode.findNodeByName("Ehcache") != null);
    } else {
      sessionSelectorAction.setEnabled(false);
      quartzSelectorAction.setEnabled(false);
      hibernateSelectorAction.setEnabled(false);
      ehcacheSelectorAction.setEnabled(false);
    }
  }

  private void registerOptions() {
    adminClientContext.registerOption(new RuntimeStatsOption(adminClientContext));
  }

  private class NavTreeSelectionListener implements TreeSelectionListener {
    public void valueChanged(TreeSelectionEvent tse) {
      TreePath path = tse.getNewLeadSelectionPath();

      nodeView.removeAll();

      if (path != null) {
        tree.requestFocus();
        XTreeNode node = (XTreeNode) path.getLastPathComponent();
        if (node != null) {
          if (node instanceof IComponentProvider) {
            Component comp = ((IComponentProvider) node).getComponent();
            if (comp != null) {
              nodeView.add(comp);
            }
          }
        }
      }

      nodeView.revalidate();
      nodeView.repaint();

      if (path != null && path.getPathCount() > 1) {
        setSelectedClusterNode((ClusterNode) path.getPathComponent(1));
      }

      updateFeatureSelectors();
    }
  }

  private void setSelectedClusterNode(ClusterNode clusterNode) {
    if (selectedClusterNode != clusterNode) {
      selectedClusterNode = clusterNode;
      logsPanel.select(clusterNode.getClusterModel());
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
    if (newClusterAction == null) {
      newClusterAction = new NewClusterAction();
    }
    return newClusterAction;
  }

  protected HelpAction getHelpAction() {
    if (helpAction == null) {
      helpAction = new HelpAction();
    }
    return helpAction;
  }

  protected AboutAction getAboutAction() {
    if (aboutAction == null) {
      aboutAction = new AboutAction();
    }
    return aboutAction;
  }

  protected void initNavTreeMenu() {
    JPopupMenu popup = new JPopupMenu("ProjectTree Actions");
    popup.add(getNewClusterAction());
    popup.add(new Separator());
    popup.add(getHelpAction());
    if (shouldAddAboutItem()) {
      popup.add(getAboutAction());
    }
    tree.setPopupMenu(popup);
  }

  public void initMenubar(XMenuBar menuBar) {
    XMenu menu = new XMenu(adminClientContext.getMessage("file.menu.label"));

    menu.add(newClusterAction = new NewClusterAction());
    menu.add(new JSeparator());
    menu.add(new QuitAction());

    menuBar.add(menu);

    menu = new XMenu(adminClientContext.getMessage("tools.menu.label"));
    menu.add(new ShowSVTAction());
    menu.add(new JSeparator());
    menu.add(optionsAction = new OptionsAction());
    menuBar.add(menu);

    menu = new XMenu(adminClientContext.getMessage("help.menu.label"));
    XMenuItem mitem = new XMenuItem(helpAction = new HelpAction());
    menu.add(mitem);
    menu.addSeparator();

    String kitID = getKitID();
    menu.add(new ContactTerracottaAction(adminClientContext.getString("visit.forums.title"), adminClientContext
        .format("forums.url", kitID)));
    menu.add(new ContactTerracottaAction(adminClientContext.getString("contact.support.title"), adminClientContext
        .format("support.url", kitID)));
    menu.addSeparator();

    updateCheckerControlAction = new UpdateCheckerControlAction();
    updateCheckerToggle = new JCheckBoxMenuItem(updateCheckerControlAction);
    updateCheckerToggle.setSelected(updateCheckerControlAction.isUpdateCheckEnabled());
    updateCheckerToggle.addActionListener(new UpdateCheckerControlHandler());
    menu.add(updateCheckerToggle);
    updateCheckerAction = new UpdateCheckerAction();

    versionCheckAction = new VersionCheckControlAction();
    versionCheckToggle = new JCheckBoxMenuItem(versionCheckAction);
    versionCheckToggle.setSelected(versionCheckAction.isVersionCheckEnabled());
    menu.add(versionCheckToggle);
    menu.add(aboutAction = new AboutAction());

    menuBar.add(menu);
  }

  private static String getKitID() {
    String kitID = ProductInfo.getInstance().kitID();
    if (kitID == null || ProductInfo.UNKNOWN_VALUE.equals(kitID)) {
      if ((kitID = System.getProperty("com.tc.kitID")) == null) {
        kitID = "2.7";
      }
    }
    return kitID;
  }

  private class HelpAction extends XAbstractAction {
    HelpAction() {
      super(adminClientContext.getString("help.item.label"));
      putValue(SMALL_ICON, HelpHelper.getHelper().getHelpIcon());
    }

    public void actionPerformed(ActionEvent ae) {
      block();
      BrowserLauncher.openURL(adminClientContext.format("console.guide.url", getKitID(), "ConsoleGuide"));
      unblock();
    }
  }

  private class ShowSVTAction extends XAbstractAction {
    ShowSVTAction() {
      super(adminClientContext.getString("show.svt.label"));
    }

    public void actionPerformed(ActionEvent ae) {
      block();
      JFrame frame = getSVTFrame();
      if (frame != null) {
        frame.setVisible(true);
      }
      unblock();
    }
  }

  public void showOption(String optionName) {
    XFrame frame = (XFrame) SwingUtilities.getAncestorOfClass(XFrame.class, this);
    new OptionsDialog(adminClientContext, frame, optionName);
  }

  public void showOptions() {
    XFrame frame = (XFrame) SwingUtilities.getAncestorOfClass(XFrame.class, this);
    new OptionsDialog(adminClientContext, frame);
  }

  private class OptionsAction extends XAbstractAction {
    OptionsAction() {
      super("Options...");
    }

    public void actionPerformed(ActionEvent ae) {
      showOptions();
    }
  }

  public void block() {
    XFrame frame = (XFrame) SwingUtilities.getAncestorOfClass(XFrame.class, this);
    if (frame != null) {
      frame.block();
    }
  }

  public void unblock() {
    XFrame frame = (XFrame) SwingUtilities.getAncestorOfClass(XFrame.class, this);
    if (frame != null) {
      frame.unblock();
    }
  }

  public boolean isExpanded(XTreeNode node) {
    return node != null && node.getParent() != null && tree.isExpanded(new TreePath(node.getPath()));
  }

  public void expand(XTreeNode node) {
    if (node != null) {
      TreeNode parentNode = node.getParent();
      if (parentNode != null) {
        TreePath path = new TreePath(node.getPath());
        tree.expandPath(path.getParentPath());
        tree.expandPath(path);
      }
    }
  }

  public void expandAll(XTreeNode node) {
    expand(node);

    Enumeration children = node.children();
    while (children.hasMoreElements()) {
      Object child = children.nextElement();
      if (child instanceof XTreeNode) {
        expandAll((XTreeNode) child);
      }
    }
  }

  public boolean selectNode(XTreeNode startNode, String nodeName) {
    XTreeNode node = startNode.findNodeByName(nodeName);
    if (node != null) {
      select(node);
    }
    return node != null;
  }

  public boolean isSelected(XTreeNode node) {
    return node != null && node.getParent() != null && tree.isPathSelected(new TreePath(node.getPath()));
  }

  public void select(XTreeNode node) {
    if (node != null && node.getParent() != null) {
      tree.requestFocus();
      tree.setSelectionPath(new TreePath(node.getPath()));
    }
  }

  protected Preferences getPreferences() {
    return adminClientContext.getPrefs().node("AdminClientFrame");
  }

  protected void storePreferences() {
    adminClientContext.storePrefs();
  }

  public void updateServerPrefs() {
    XRootNode root = tree.getRootNode();
    int count = root.getChildCount();
    PrefsHelper helper = PrefsHelper.getHelper();
    Preferences prefs = adminClientContext.getPrefs();
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
    XRootNode root = tree.getRootNode();
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
    logArea.append(s + System.getProperty("line.separator"));
  }

  public void log(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    pw.close();
    log(sw.toString());
  }

  public void setStatus(String msg) {
    statusLine.setText(msg);
  }

  public void clearStatus() {
    setStatus("");
  }

  class NewClusterAction extends XAbstractAction implements Runnable {
    NewClusterAction() {
      super(adminClientContext.getMessage("new.cluster.action.label"));
    }

    public void actionPerformed(ActionEvent ae) {
      SwingUtilities.invokeLater(this);
    }

    public void run() {
      XTreeModel model = (XTreeModel) tree.getModel();
      XTreeNode root = (XTreeNode) model.getRoot();
      int index = root.getChildCount();
      ClusterNode clusterNode = adminClientContext.getNodeFactory().createClusterNode(adminClientContext);

      model.insertNodeInto(clusterNode, root, index);
      TreePath path = new TreePath(clusterNode.getPath());
      tree.makeVisible(path);
      tree.setSelectionPath(path);

      PrefsHelper helper = PrefsHelper.getHelper();
      Preferences prefs = adminClientContext.getPrefs();
      Preferences servers = prefs.node(ServersHelper.SERVERS);
      int count = helper.childrenNames(servers).length;

      clusterNode.setPreferences(servers.node("server-" + count));
      storePreferences();

      clusterNode.getClusterModel().addPropertyChangeListener(AdminClientPanel.this);
      adminClientContext.setStatus(adminClientContext.format("added.server", clusterNode));
    }
  }

  /**
   * Returns true if quit should proceed.
   */
  private boolean testWarnMonitoringActivity() {
    XTreeModel model = (XTreeModel) tree.getModel();
    XTreeNode root = (XTreeNode) model.getRoot();
    int count = root.getChildCount();
    boolean recordingStats = false;
    boolean profilingLocks = false;

    for (int i = 0; i < count; i++) {
      ClusterNode clusterNode = (ClusterNode) root.getChildAt(i);
      if (clusterNode.recordingClusterStats()) {
        recordingStats = true;
      }
      if (clusterNode.isProfilingLocks()) {
        profilingLocks = true;
      }
    }

    if (recordingStats || profilingLocks) {
      String key;
      if (recordingStats && profilingLocks) {
        key = "recording.stats.profiling.locks.msg";
      } else if (recordingStats) {
        key = "recording.stats.msg";
      } else {
        key = "profiling.locks.msg";
      }

      String msg = adminClientContext.format(key, adminClientContext.getMessage("quit.anyway"));
      Frame frame = getFrame();
      int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
      return answer == JOptionPane.OK_OPTION;
    }

    return true;
  }

  public void handleQuit() {
    if (testWarnMonitoringActivity()) {
      storePreferences();
      Runtime.getRuntime().exit(0);
    }
  }

  class QuitAction extends XAbstractAction {
    QuitAction() {
      super(adminClientContext.getMessage("quit.action.label"));
      setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_SHORTCUT_KEY_MASK, true));
    }

    public void actionPerformed(ActionEvent ae) {
      handleQuit();
    }
  }

  private Frame getFrame() {
    return (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
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
    if (versionCheckAction == null || !versionCheckAction.isVersionCheckEnabled()) { return true; }

    ProductInfo consoleInfo = ProductInfo.getInstance();
    String consoleVersion = consoleInfo.version();
    IProductVersion serverInfo = clusterNode.getProductInfo();
    if (serverInfo == null) { return true; // something went wrong, move on
    }
    String serverVersion = serverInfo.mavenArtifactsVersion();

    if (!versionsMatch(consoleVersion, serverVersion)) {
      int answer = showVersionMismatchDialog(clusterNode, consoleVersion, serverVersion);
      return (answer == JOptionPane.YES_OPTION);
    }

    return true;
  }

  public int showVersionMismatchDialog(ClusterNode clusterNode, String consoleVersion, String serverVersion)
      throws HeadlessException {
    Frame frame = getFrame();
    XLabel label = new XLabel(adminClientContext.format("version.check.message", clusterNode, serverVersion,
                                                        consoleVersion));
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(label);
    XCheckBox versionCheckCheckBox = new XCheckBox(adminClientContext.getMessage("version.check.disable.label"));
    versionCheckCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
    panel.add(versionCheckCheckBox, BorderLayout.SOUTH);
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

    if (selectedValue == null) { return JOptionPane.CLOSED_OPTION; }
    versionCheckAction.setVersionCheckEnabled(!versionCheckCheckBox.isSelected());
    if (selectedValue instanceof Integer) { return ((Integer) selectedValue).intValue(); }

    return JOptionPane.CLOSED_OPTION;
  }

  class UpdateCheckerControlHandler implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      updateCheckerControlAction.setUpdateCheckEnabled(updateCheckerToggle.isSelected());
    }
  }

  class UpdateCheckerControlAction extends XAbstractAction {
    UpdateCheckerControlAction() {
      super(adminClientContext.getMessage("update-checker.control.label"));
    }

    boolean isUpdateCheckEnabled() {
      return getUpdateCheckerPrefs().getBoolean("checking-enabled", true);
    }

    void setUpdateCheckEnabled(boolean checkEnabled) {
      getUpdateCheckerPrefs().putBoolean("checking-enabled", checkEnabled);
      updateCheckerToggle.setSelected(checkEnabled);
      updateCheckerAction.setEnabled(checkEnabled);
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
    sb.append(URLEncoder.encode(System.getProperty("java.version")));
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
    ProductInfo productInfo;

    UpdateCheckerAction() {
      super(adminClientContext.getMessage("update-checker.action.label"));

      if (isEnabled() && updateCheckerControlAction.isUpdateCheckEnabled()) {
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
      if (productInfo == null) {
        productInfo = ProductInfo.getInstance();
      }
      return productInfo;
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
      XTextArea textArea = new XTextArea();
      textArea.setText(msg);
      textArea.setRows(8);
      textArea.setColumns(80);
      textArea.setEditable(false);
      textArea.setLineWrap(true);
      textArea.setWrapStyleWord(true);
      XScrollPane scrollPane = new XScrollPane(textArea);
      JOptionPane.showMessageDialog(AdminClientPanel.this, scrollPane,
                                    adminClientContext.getMessage("update-checker.action.title"),
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
        @Override
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
                    sb.append(adminClientContext.getMessage("update-checker.release-notes.label"));
                    sb.append("</a>");
                  }
                  sb.append("</li>\n");
                }
                sb.append("</ol>");
              }
            }
            if (sb.length() > 0) {
              sb.insert(0, "<html><body><p>" + adminClientContext.getMessage("update-checker.updates.available.msg")
                           + "</p>");
              sb.append("</body></html>");
              XTextPane textPane = new XTextPane();
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
            JOptionPane.showMessageDialog(AdminClientPanel.this, msg,
                                          adminClientContext.getMessage("update-checker.action.title"),
                                          JOptionPane.INFORMATION_MESSAGE);
          }
        });
      }
    }
  }

  class VersionCheckControlAction extends XAbstractAction {
    VersionCheckControlAction() {
      super(adminClientContext.getMessage("version.check.enable.label"));
    }

    boolean isVersionCheckEnabled() {
      Preferences versionCheckPrefs = getPreferences().node("version-check");
      return versionCheckPrefs.getBoolean("enabled", true);
    }

    void setVersionCheckEnabled(boolean checkEnabled) {
      Preferences versionCheckPrefs = getPreferences().node("version-check");
      versionCheckPrefs.putBoolean("enabled", checkEnabled);
      versionCheckToggle.setSelected(checkEnabled);
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
    AboutDialog aboutDialog;

    AboutAction() {
      super(adminClientContext.getMessage("about.action.label"));
    }

    public void actionPerformed(ActionEvent ae) {
      Frame frame = getFrame();
      if (aboutDialog == null) {
        aboutDialog = new AboutDialog(frame);
        aboutDialog.setTitle(adminClientContext.getString("about.prefix") + adminClientContext.getString("title"));
      }

      aboutDialog.pack();
      WindowHelper.center(aboutDialog, frame);
      aboutDialog.setVisible(true);
    }
  }

  public void addClusterLog(IClusterModel clusterModel) {
    logsPanel.add(clusterModel);
  }

  public void removeClusterLog(IClusterModel clusterModel) {
    logsPanel.remove(clusterModel);
  }

  class FeatureSelectorAction extends XAbstractAction implements HyperlinkListener {
    private boolean      enabled;
    private final Icon   icon;
    private final Icon   disabledIcon;
    private XButton      btn;
    private final String page;

    FeatureSelectorAction(String name, String iconPath, String disabledIconPath, String page) {
      super(name);
      icon = new ImageIcon(getClass().getResource(iconPath));
      disabledIcon = new ImageIcon(getClass().getResource(disabledIconPath));
      enabled = true;
      this.page = page;
    }

    @Override
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      btn.setIcon(enabled ? icon : disabledIcon);
    }

    XButton createButton() {
      btn = new XButton(this);
      btn.setIcon(icon);
      btn.setVerticalTextPosition(SwingConstants.BOTTOM);
      btn.setHorizontalTextPosition(SwingConstants.CENTER);
      btn.setMargin(new Insets(3, 3, 3, 3));
      return btn;
    }

    public void actionPerformed(ActionEvent ae) {
      if (enabled) {
        XRootNode root = (XRootNode) tree.getModel().getRoot();
        selectNode(root, getName());
      } else {
        showPage();
      }
    }

    private void showPage() {
      final XContainer msg = new XContainer(new BorderLayout());
      XTextPane textPane = new XTextPane();
      textPane.setEditorKit(new SyncHTMLEditorKit());
      msg.add(new XScrollPane(textPane));
      textPane.setPreferredSize(new Dimension(550, 280));
      textPane.setEditable(false);
      textPane.addHyperlinkListener(this);
      textPane.addPropertyChangeListener("page", new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent pce) {
          Frame frame = getFrame();
          JOptionPane.showMessageDialog(AdminClientPanel.this, msg, frame.getTitle(), JOptionPane.INFORMATION_MESSAGE);
        }
      });
      try {
        textPane.setPage(getClass().getResource(page + ".html"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
      XTextPane textPane = (XTextPane) e.getSource();
      HyperlinkEvent.EventType type = e.getEventType();
      Element elem = e.getSourceElement();

      if (elem == null || type == HyperlinkEvent.EventType.ENTERED || type == HyperlinkEvent.EventType.EXITED) { return; }

      if (textPane.getCursor().getType() != Cursor.WAIT_CURSOR) {
        AttributeSet anchor = (AttributeSet) elem.getAttributes().getAttribute(HTML.Tag.A);
        String url = (String) anchor.getAttribute(HTML.Attribute.HREF);
        BrowserLauncher.openURL(url);

        JDialog dialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, textPane);
        if (dialog != null) {
          dialog.setVisible(false);
        }
      }
    }
  }

  public UndoManager getUndoManager() {
    if (undoManager == null) {
      undoManager = new MyUndoManager();
    }
    return undoManager;
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

    @Override
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

    @Override
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

  @Override
  public String toString() {
    return getName();
  }

  protected void addEdit(UndoableEdit edit) {
    getUndoManager().addEdit(edit);
  }

  // SVT

  private static final String SNAPSHOT_VISUALIZER_TYPE = "org.terracotta.tools.SnapshotVisualizer";
  private ClassLoader         svtClassLoader;
  private JFrame              svtFrame;

  private String getSvtUrl() {
    return adminClientContext.format("get.svt.url", getKitID());
  }

  private static class VersionMap implements Comparable {
    final File    versionDir;
    final Version version;
    final String  qualifier;

    VersionMap(File versionDir) {
      this.versionDir = versionDir;
      String name = versionDir.getName();
      int dashIndex = name.indexOf('-');
      if (dashIndex != -1) {
        qualifier = name.substring(dashIndex + 1);
        name = name.substring(0, dashIndex);
      } else {
        qualifier = null;
      }
      this.version = new Version(name);
    }

    public int compareTo(Object o) {
      int result = 0;
      if (o instanceof VersionMap) {
        VersionMap other = (VersionMap) o;
        result = version.compareTo(other.version);
        if (result == 0) {
          result = qualifier != null ? -1 : 1;
        }
      }
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) { return false; }
      if (obj == this) { return true; }
      if (obj.getClass() != getClass()) { return false; }
      VersionMap rhs = (VersionMap) obj;
      return new EqualsBuilder().appendSuper(super.equals(obj)).append(versionDir, rhs.versionDir)
          .append(version, rhs.version).append(qualifier, rhs.qualifier).isEquals();
    }

    @Override
    public int hashCode() {
      HashCodeBuilder builder = new HashCodeBuilder();
      builder.append(versionDir);
      builder.append(version);
      if (qualifier != null) {
        builder.append(qualifier);
      }
      return builder.toHashCode();
    }

    @Override
    public String toString() {
      return "[path=" + versionDir.getAbsolutePath() + ", version=" + version.toString() + ", qualifier=" + qualifier
             + "]";
    }
  }

  /**
   * Inspect the default kit modules area for the set of tim-svt's, determine the newest one, use it to create an SVT
   * frame. This assume the user is running the console from a kit and that the tim-svt was installed using the tim-get
   * script.
   */
  protected ClassLoader getSVTClassLoader() {
    if (svtClassLoader == null) {
      String tcInstallRoot = System.getProperty("tc.install-root");
      if (tcInstallRoot != null) {
        String timSvtPath = getSVTDirectory(tcInstallRoot);
        File timSvtRoot = new File(timSvtPath);
        if (timSvtRoot.exists()) {
          File[] versions = timSvtRoot.listFiles();
          ArrayList<VersionMap> vm = new ArrayList<VersionMap>();
          for (File versionDir : versions) {
            if (versionDir.isDirectory()) {
              String name = versionDir.getName();
              if (name.matches("\\d+\\.\\d+\\.\\d+(-.*+)?")) {
                vm.add(new VersionMap(versionDir));
              }
            }
          }
          VersionMap[] vma = vm.toArray(new VersionMap[vm.size()]);
          if (vma.length > 0) {
            Arrays.sort(vma);
            File newestDir = vma[vma.length - 1].versionDir;
            File newest = new File(newestDir, getTimSvtName() + "-" + newestDir.getName() + ".jar");
            try {
              URL[] source = { newest.toURL() };
              svtClassLoader = URLClassLoader.newInstance(source, getClass().getClassLoader());
            } catch (Exception e) {
              log(e);
            }
          }
        }
      }
    }

    return svtClassLoader;
  }

  protected String getSVTDirectory(String tcInstallRoot) {
    String timSvtPath = new StringBuilder(tcInstallRoot).append(File.separator).append("platform")
        .append(File.separator).append("modules").append(File.separator).append("org").append(File.separator)
        .append("terracotta").append(File.separator).append("modules").append(File.separator).append(getTimSvtName())
        .toString();
    return timSvtPath;
  }

  protected String getTimSvtName() {
    return "tim-svt";
  }

  protected Class getSVTFrameType() throws ClassNotFoundException {
    ClassLoader cl = getSVTClassLoader();
    if (cl != null) {
      return cl.loadClass(SNAPSHOT_VISUALIZER_TYPE);
    } else {
      return Class.forName(SNAPSHOT_VISUALIZER_TYPE);
    }
  }

  public JFrame getSVTFrame() {
    if (svtFrame == null) {
      try {
        Class svtFrameType = getSVTFrameType();
        try {
          Method getOrCreate = svtFrameType.getMethod("getOrCreate");
          svtFrame = (JFrame) getOrCreate.invoke(null);
        } catch (Exception e) {
          log(e);
        }
      } catch (ClassNotFoundException e) {
        BrowserLauncher.openURL(getSvtUrl());
      }
    }
    return svtFrame;
  }

  public void propertyChange(final PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (IClusterModel.PROP_CONNECTED.equals(prop)) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          IClusterModel clusterModel = (IClusterModel) evt.getSource();
          if (clusterModel.isConnected()) {
            addClusterLog(clusterModel);
          } else {
            removeClusterLog(clusterModel);
          }
        }
      });
    }
  }
}
