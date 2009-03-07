/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlError;

import com.tc.admin.ConnectionContext;
import com.tc.admin.ConnectionListener;
import com.tc.admin.ServerConnectionManager;
import com.tc.admin.common.AboutDialog;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.ContactTerracottaAction;
import com.tc.admin.common.FastFileChooser;
import com.tc.admin.common.OutputStreamListener;
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XFrame;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XMenu;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTree;
import com.tc.config.Directories;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.object.tools.BootJarSignature;
import com.tc.object.tools.UnsupportedVMException;
import com.tc.util.ProductInfo;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class SessionIntegratorFrame extends XFrame implements PropertyChangeListener {
  private static final boolean           debug                       = Boolean
                                                                         .getBoolean("SessionIntegratorFrame.debug");

  private final SessionIntegratorContext sessionIntegratorContext;
  private final ConfigHelper             configHelper;
  private SplashDialog                   splashDialog;

  private final XTabbedPane              tabbedPane;
  private int                            lastSelectedTabIndex;
  private WebAppTreeModel                webAppTreeModel;
  private XButton                        startButton;
  private XButton                        stopButton;
  private XCheckBox                      dsoEnabledToggle;
  private boolean                        dsoEnabled;
  private XTree                          webAppTree;
  private WebAppLinkNode                 lastArmedLink;
  private XTabbedPane                    configTabbedPane;
  private ConfigTextPane                 xmlPane;
  private XmlChangeListener              xmlChangeListener;
  private ConfigProblemTable             configProblemTable;
  private ConfigProblemTableModel        configProblemTableModel;

  private ProcessOutputView              l2OutView;
  private XLabel                         l2Label;
  private ProcessStatus                  l2Status;
  private boolean                        handlingAppEvent;
  private L2StartupListener              l2StartupListener;
  private L2ShutdownListener             l2ShutdownListener;
  private L2ShutdownMonitor              l2Monitor;
  private final L2ConnectListener        l2ConnectListener;
  private final ServerConnectionManager  l2ConnectManager;

  private XCheckBox                      webServer1EnabledToggle;
  private boolean                        webServer1Enabled;
  private ProcessOutputView              webServer1OutView;
  private XLabel                         webServer1Label;
  private ProcessStatus                  webServer1Status;
  private XButton                        webServer1Control;
  private WebServer1StartupListener      webServer1StartupListener;
  private WebServer1ShutdownListener     webServer1ShutdownListener;
  private WebServerShutdownMonitor       webServer1Monitor;

  private XCheckBox                      webServer2EnabledToggle;
  private boolean                        webServer2Enabled;
  private ProcessOutputView              webServer2OutView;
  private XLabel                         webServer2Label;
  private ProcessStatus                  webServer2Status;
  private XButton                        webServer2Control;
  private WebServer2StartupListener      webServer2StartupListener;
  private WebServer2ShutdownListener     webServer2ShutdownListener;
  private WebServerShutdownMonitor       webServer2Monitor;

  private Icon                           waitingIcon;
  private Icon                           readyIcon;
  private Icon                           stoppedIcon;

  private Icon                           startIcon;
  private Icon                           stopIcon;

  private InstrumentedClassesPanel       instrumentedClassesPanel;
  private TransientFieldsPanel           transientFieldsPanel;
  private BootClassesPanel               bootClassesPanel;
  private ModulesPanel                   modulesPanel;

  private ImportWebAppAction             importAction;
  private HelpAction                     helpAction;

  private boolean                        askRestart;
  private boolean                        restarting;
  private boolean                        quitting;

  private static String                  SHOW_SPLASH_PREF_KEY        = "ShowSplash";
  private static String                  LAST_DIR_PREF_KEY           = "LastDirectory";
  private static String                  DSO_ENABLED_PREF_KEY        = "DsoEnabled";
  private static String                  WEBSERVER1_ENABLED_PREF_KEY = "WebServer1Enabled";
  private static String                  WEBSERVER2_ENABLED_PREF_KEY = "WebServer2Enabled";

  private static final String            BAT_EXTENSION               = ".bat";
  private static final String            SH_EXTENSION                = ".sh";
  private static final String            SCRIPT_EXTENSION            = getScriptExtension();
  private static final String            FS                          = System.getProperty("file.separator");
  private static final String            DEFAULT_TC_INSTALL_DIR      = getDefaultInstallDir();
  private static final String            TC_INSTALL_DIR              = System.getProperty("tc.install.dir",
                                                                                          DEFAULT_TC_INSTALL_DIR);
  private static final String            DEFAULT_SANDBOX_ROOT        = TC_INSTALL_DIR + FS + "tools" + FS + "sessions"
                                                                       + FS + "configurator-sandbox";
  private static final String            SANDBOX_ROOT                = System.getProperty("configurator.sandbox",
                                                                                          DEFAULT_SANDBOX_ROOT);
  private static final String            L2_LABEL                    = "Terracotta Server instance";
  private static final String            L2_STARTUP_SCRIPT           = "start-tc-server" + SCRIPT_EXTENSION;
  private static final String            L2_SHUTDOWN_SCRIPT          = "stop-tc-server" + SCRIPT_EXTENSION;
  private static final String            L2_STARTUP_TRIGGER          = "Terracotta Server instance has started up";
  private static final int               SERVER1_PORT                = 9081;
  // private static final String WEBSERVER_STARTUP_SCRIPT = "start-web-server" + SCRIPT_EXTENSION;
  // private static final String WEBSERVER_SHUTDOWN_SCRIPT = "stop-web-server" + SCRIPT_EXTENSION;
  private static final int               SERVER2_PORT                = 9082;
  private static final String            HELP_DOC                    = TC_INSTALL_DIR + FS + "docs" + FS
                                                                       + "TerracottaSessionsQuickStart.html";

  private static final Cursor            LINK_CURSOR                 = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
  private static final Cursor            STANDARD_CURSOR             = Cursor.getDefaultCursor();

  private static final int               CONTROL_TAB_INDEX           = 0;
  private static final int               CONFIG_TAB_INDEX            = 1;
  private static final int               MONITOR_TAB_INDEX           = 2;

  private static final int               XML_TAB_INDEX               = 4;
  private static final String            XML_TAB_LABEL               = "tc-config.xml";

  private static final String            QUERY_START_MSG             = "Start the system?";
  private static final String            QUERY_RESTART_MSG           = "Restart the system?";

  private static final String            WAITING_LABEL               = " [Waiting...]";
  private static final String            STARTING_LABEL              = " [Starting...]";
  private static final String            STOPPING_LABEL              = " [Stopping...]";
  private static final String            READY_LABEL                 = " [Ready]";
  private static final String            STOPPED_LABEL               = " [Stopped]";
  private static final String            FAILED_LABEL                = " [Failed]";
  private static final String            DISABLED_LABEL              = " [Disabled]";

  // private Process m_jetty1Proc;
  // private Process m_jetty2Proc;

  public SessionIntegratorFrame(SessionIntegratorContext sessionIntegratorContext) {
    super();

    this.sessionIntegratorContext = sessionIntegratorContext;

    setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/com/tc/admin/icons/logo_small.gif")));
    setTitle(getBundleString("title"));
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    configHelper = createConfigHelper();

    initMenubar();
    loadIcons();

    tabbedPane = new XTabbedPane();
    tabbedPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        if (lastSelectedTabIndex == CONFIG_TAB_INDEX && isXmlModified()) {
          if (querySaveConfig(JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            askRestart = isL2Ready();
          }
        }
        if (askRestart) {
          String msg = "Configuration has been modified.\n\n" + QUERY_RESTART_MSG;
          queryRestart(msg);
        }
        askRestart = false;
        lastSelectedTabIndex = tabbedPane.getSelectedIndex();
      }
    });

    getContentPane().add(tabbedPane);

    addTab(tabbedPane, "Control", "/com/tc/admin/icons/thread_obj.gif", createControlPanel());
    addTab(tabbedPane, "Configure", "/com/tc/admin/icons/text_edit.gif", createConfigurePanel());
    addTab(tabbedPane, "Monitor", "/com/tc/admin/icons/monitor_obj.gif", createMonitorPanel());

    initXmlPane();

    startButton.setEnabled(isWebServer1Enabled() || isWebServer2Enabled() || isDsoEnabled());

    Preferences prefs = getPreferences();
    if (prefs.getBoolean(SHOW_SPLASH_PREF_KEY, true)) {
      addComponentListener(new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent e) {
          openSplashDialog(this);
        }
      });
    }

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent we) {
        quit();
      }
    });

    l2ConnectListener = new L2ConnectListener();
    l2ConnectManager = new ServerConnectionManager("localhost", configHelper.getJmxPort(), false, l2ConnectListener);
    testShutdownL2();
    testShutdownWebServer1();
    testShutdownWebServer2();

    setDsoEnabled(dsoEnabled = prefs.getBoolean(DSO_ENABLED_PREF_KEY, false));
  }

  private XContainer createControlPanel() {
    XContainer leftPanel = new XContainer(new BorderLayout());
    webAppTree = new XTree();
    webAppTree.setModel(webAppTreeModel = new WebAppTreeModel(this, getWebApps()));
    webAppTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    webAppTree.addMouseListener(new TreeMouseListener());
    webAppTree.addMouseMotionListener(new TreeMouseMotionListener());

    leftPanel.add(new XScrollPane(webAppTree));
    XContainer topLeftPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.WEST;

    dsoEnabledToggle = new XCheckBox("Terracotta Sessions enabled");
    dsoEnabledToggle.setSelected(false);
    dsoEnabledToggle.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setDsoEnabled(dsoEnabledToggle.isSelected());
      }
    });
    gbc.gridwidth = 2;
    topLeftPanel.add(dsoEnabledToggle, gbc);
    gbc.gridy++;

    startButton = new XButton("Start all");
    startIcon = newIcon("/com/tc/admin/icons/run_exc.gif");
    startButton.setIcon(startIcon);
    startButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        startSystem();
      }
    });
    gbc.gridwidth = 1;
    topLeftPanel.add(startButton, gbc);
    gbc.gridx++;

    stopButton = new XButton("Stop all");
    stopIcon = newIcon("/com/tc/admin/icons/terminate_co.gif");
    stopButton.setIcon(stopIcon);
    stopButton.setEnabled(false);
    stopButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        stopSystem();
      }
    });
    topLeftPanel.add(stopButton, gbc);

    // filler
    gbc.weightx = 1.0;
    topLeftPanel.add(new XLabel(), gbc);

    leftPanel.add(topLeftPanel, BorderLayout.NORTH);

    XContainer rightPanel = new XContainer(new GridLayout(3, 1));
    rightPanel.add(createWebServer1Panel());
    rightPanel.add(createWebServer2Panel());
    rightPanel.add(createL2Panel());

    XSplitPane splitter = new XSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
    splitter.setPreferences(getPreferences().node("ControlSplitter"));
    splitter.setResizeWeight(0.1);

    XContainer panel = new XContainer(new BorderLayout());
    panel.add(splitter, BorderLayout.CENTER);

    return panel;
  }

  private JComponent createWebServer1Panel() {
    WebServerPanel webServer1Panel = new WebServerPanel();
    webServer1EnabledToggle = webServer1Panel.enabledToggle;
    webServer1Enabled = getPreferences().getBoolean(WEBSERVER1_ENABLED_PREF_KEY, true);
    webServer1EnabledToggle.setSelected(webServer1Enabled);
    webServer1EnabledToggle.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setWebServer1Enabled(webServer1EnabledToggle.isSelected());
      }
    });

    webServer1Label = webServer1Panel.label;
    webServer1Label.setText(getWebServer1Label());

    webServer1Control = webServer1Panel.controlButton;
    webServer1Control.setIcon(newIcon("/com/tc/admin/icons/run_exc.gif"));
    webServer1Control.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        disableControls();
        webAppTreeModel.updateLinks(false, isWebServer2Ready());
        toggleWebServer1();
      }
    });

    webServer1OutView = webServer1Panel.outputView;
    webServer1Status = new ProcessStatus(getWebServer1Label());
    webServer1StartupListener = new WebServer1StartupListener();
    webServer1ShutdownListener = new WebServer1ShutdownListener();

    return webServer1Panel;
  }

  private JComponent createWebServer2Panel() {
    WebServerPanel webServer2Panel = new WebServerPanel();
    webServer2EnabledToggle = webServer2Panel.enabledToggle;
    webServer2Enabled = getPreferences().getBoolean(WEBSERVER2_ENABLED_PREF_KEY, true);
    webServer2EnabledToggle.setSelected(webServer2Enabled);
    webServer2EnabledToggle.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setWebServer2Enabled(webServer2EnabledToggle.isSelected());
      }
    });

    webServer2Label = webServer2Panel.label;
    webServer2Label.setText(getWebServer2Label());

    webServer2Control = webServer2Panel.controlButton;
    webServer2Control.setIcon(newIcon("/com/tc/admin/icons/run_exc.gif"));
    webServer2Control.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        disableControls();
        webAppTreeModel.updateLinks(isWebServer1Ready(), false);
        toggleWebServer2();
      }
    });

    webServer2OutView = webServer2Panel.outputView;
    webServer2Status = new ProcessStatus(getWebServer2Label());
    webServer2StartupListener = new WebServer2StartupListener();
    webServer2ShutdownListener = new WebServer2ShutdownListener();

    return webServer2Panel;
  }

  private static class WebServerPanel extends XContainer {
    XCheckBox         enabledToggle;
    XLabel            label;
    XButton           controlButton;
    ProcessOutputView outputView;

    private WebServerPanel() {
      super(new GridBagLayout());

      XContainer topPanel = new XContainer(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = gbc.gridy = 0;
      gbc.insets = new Insets(1, 3, 1, 3);
      gbc.anchor = GridBagConstraints.WEST;

      topPanel.add(enabledToggle = new XCheckBox(), gbc);
      gbc.gridx++;

      topPanel.add(label = new XLabel(), gbc);
      gbc.gridx++;

      controlButton = new XButton();
      controlButton.setVisible(false);
      controlButton.setMargin(new Insets(0, 0, 0, 0));
      topPanel.add(controlButton, gbc);

      // filler
      gbc.weightx = 1.0;
      topPanel.add(new XLabel(), gbc);

      gbc.gridx = gbc.gridy = 0;

      add(topPanel, gbc);
      gbc.gridy++;

      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = gbc.weighty = 1.0;
      add(new XScrollPane(outputView = new ProcessOutputView()), gbc);
    }
  }

  private JComponent createL2Panel() {
    XContainer l2Panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.WEST;

    l2Panel.add(l2Label = new XLabel("Terracotta Server"), gbc);
    gbc.gridx++;

    // filler
    l2Panel.add(new XLabel(), gbc);
    gbc.gridx--;
    gbc.gridy++;

    gbc.weightx = gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.CENTER;
    l2OutView = new ProcessOutputView();
    l2Panel.add(new XScrollPane(l2OutView), gbc);

    l2Status = new ProcessStatus("L2");
    l2StartupListener = new L2StartupListener();
    l2ShutdownListener = new L2ShutdownListener();

    return l2Panel;
  }

  private void addTab(XTabbedPane theTabbedPane, String title, String iconPath, JComponent component) {
    Icon icon = null;
    if (iconPath != null) {
      icon = newIcon(iconPath);
    }
    theTabbedPane.addTab(title, icon, component);
  }

  private JComponent createConfigurePanel() {
    configTabbedPane = new XTabbedPane(SwingConstants.BOTTOM);
    addTab(configTabbedPane, "Instrumented classes", "/com/tc/admin/icons/class_obj.gif",
           instrumentedClassesPanel = new InstrumentedClassesPanel());
    addTab(configTabbedPane, "Transient fields", "/com/tc/admin/icons/transient.gif",
           transientFieldsPanel = new TransientFieldsPanel());
    addTab(configTabbedPane, "Boot classes", "/com/tc/admin/icons/jar_obj.gif",
           bootClassesPanel = new BootClassesPanel());
    addTab(configTabbedPane, "Modules", "/com/tc/admin/icons/plugin_obj.gif", modulesPanel = new ModulesPanel());
    configTabbedPane.addTab("tc-config.xml", createConfigTextPanel());
    return configTabbedPane;
  }

  private JComponent createConfigTextPanel() {
    xmlPane = new ConfigTextPane();
    xmlChangeListener = new XmlChangeListener();
    XTabbedPane problemsTabbedPane = new XTabbedPane();
    configProblemTable = new ConfigProblemTable();
    configProblemTableModel = new ConfigProblemTableModel();
    configProblemTable.setModel(configProblemTableModel);
    configProblemTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() == 2) {
          int row = configProblemTable.getSelectedRow();
          XmlError error = configProblemTableModel.getError(row);
          xmlPane.selectError(error);
        }
      }
    });
    problemsTabbedPane.addTab("Problems", new XScrollPane(configProblemTable));

    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    topPanel.add(createCommandButton(xmlPane.getSaveAction()), gbc);
    gbc.gridx++;

    topPanel.add(createCommandButton(xmlPane.getUndoAction()), gbc);
    gbc.gridx++;

    topPanel.add(createCommandButton(xmlPane.getRedoAction()), gbc);
    gbc.gridx++;

    topPanel.add(createCommandButton(xmlPane.getCutAction()), gbc);
    gbc.gridx++;

    topPanel.add(createCommandButton(xmlPane.getCopyAction()), gbc);
    gbc.gridx++;

    topPanel.add(createCommandButton(xmlPane.getPasteAction()), gbc);
    gbc.gridx++;

    // filler
    gbc.weightx = 1.0;
    topPanel.add(new XLabel(), gbc);

    XContainer panel = new XContainer(new BorderLayout());
    XSplitPane splitter = new XSplitPane(JSplitPane.VERTICAL_SPLIT, new XScrollPane(xmlPane), problemsTabbedPane);
    panel.add(topPanel, BorderLayout.NORTH);
    panel.add(splitter, BorderLayout.CENTER);

    return panel;
  }

  private XButton createCommandButton(Action action) {
    XButton button = new XButton();
    button.setAction(action);
    button.setText(action == null ? "Null Action" : null);
    button.setFocusable(false);
    button.setMargin(new Insets(1, 1, 1, 1));
    return button;
  }

  private JComponent createMonitorPanel() {
    return new SessionIntegratorAdminPanel(sessionIntegratorContext);
  }

  private class TreeMouseListener extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent me) {
      if (me.getClickCount() == 1) {
        TreePath path = webAppTree.getPathForLocation(me.getX(), me.getY());
        if (path != null) {
          Object leaf = path.getLastPathComponent();
          if (leaf instanceof WebAppLinkNode) {
            WebAppLinkNode node = (WebAppLinkNode) leaf;
            if (node.isReady()) {
              openPage(node.getLink());
            }
          }
        }
      }
    }
  }

  private class TreeMouseMotionListener extends MouseMotionAdapter {
    @Override
    public void mouseMoved(MouseEvent me) {
      TreePath path = webAppTree.getPathForLocation(me.getX(), me.getY());
      if (path != null) {
        Object leaf = path.getLastPathComponent();
        if (leaf instanceof WebAppLinkNode) {
          WebAppLinkNode node = (WebAppLinkNode) leaf;
          Cursor c = webAppTree.getCursor();
          if (lastArmedLink != node) {
            if (lastArmedLink != null) {
              lastArmedLink.setArmed(false);
            }
            node.setArmed(true);
            lastArmedLink = node;
          }
          if (node.isReady() && c != LINK_CURSOR) {
            webAppTree.setCursor(LINK_CURSOR);
          }
          return;
        }
      }
      if (lastArmedLink != null) {
        lastArmedLink.setArmed(false);
        lastArmedLink = null;
      }
      webAppTree.setCursor(null);
    }
  }

  private ConfigHelper createConfigHelper() {
    ConfigHelper result = new ConfigHelper();
    result.addPropertyChangeListener(this);
    return result;
  }

  public void propertyChange(PropertyChangeEvent evt) {
    if (evt.getPropertyName().equals(ConfigHelper.PROP_CONFIG)) {
      setupEditorPanels();
    }
  }

  private String getBundleString(String key) {
    return sessionIntegratorContext.getMessage(key);
  }

  private String formatBundleString(String key, Object... args) {
    return sessionIntegratorContext.format(key, args);
  }

  static String getTCInstallDir() {
    return TC_INSTALL_DIR;
  }

  public static String getSandBoxRoot() {
    return SANDBOX_ROOT;
  }

  ConfigHelper getConfigHelper() {
    return configHelper;
  }

  private static String getScriptExtension() {
    return Os.isWindows() ? BAT_EXTENSION : SH_EXTENSION;
  }

  private static String getDefaultInstallDir() {
    try {
      return Directories.getInstallationRoot().getAbsolutePath();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void openSplashDialog(ComponentAdapter splashListener) {
    splashDialog = new SplashDialog(this, true);

    splashDialog.getHelpButton().addActionListener(helpAction);
    splashDialog.getImportButton().addActionListener(importAction);
    splashDialog.getSkipButton().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        checkShowSplashToggle();
        splashDialog.setVisible(false);
      }
    });
    WindowHelper.center(splashDialog, this);
    splashDialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent we) {
        checkShowSplashToggle();
      }
    });
    if (splashListener != null) {
      removeComponentListener(splashListener);
    }
    splashDialog.setVisible(true);
  }

  private void checkShowSplashToggle() {
    if (splashDialog != null) {
      JCheckBox toggle = splashDialog.getNoSplashToggle();
      Preferences prefs = getPreferences();
      prefs.putBoolean(SHOW_SPLASH_PREF_KEY, !toggle.isSelected());
      storePreferences();
    }
  }

  private void loadIcons() {
    waitingIcon = newIcon("/com/tc/admin/icons/progress_task_yellow.gif");
    readyIcon = newIcon("/com/tc/admin/icons/progress_task_green.gif");
    stoppedIcon = newIcon("/com/tc/admin/icons/progress_task_red.gif");
  }

  private void initMenubar() {
    JMenuBar menuBar = new JMenuBar();
    XMenu menu = new XMenu(getBundleString("file.menu.label"));

    menu.add(importAction = new ImportWebAppAction());
    menu.add(new ExportConfigurationAction());
    menu.add(new QuitAction());
    menuBar.add(menu);

    menu = new XMenu(getBundleString("output.menu.label"));
    menu.add(new ClearOutputAction());
    menuBar.add(menu);

    menu = new XMenu(getBundleString("help.menu.label"));
    menu.add(helpAction = new HelpAction());
    menu.add(new ShowSplashAction());
    menu.addSeparator();

    String kitID = ProductInfo.getInstance().kitID();
    menu
        .add(new ContactTerracottaAction(getBundleString("visit.forums.title"), formatBundleString("forums.url", kitID)));
    menu.add(new ContactTerracottaAction(getBundleString("contact.support.title"), formatBundleString("support.url",
                                                                                                      kitID)));

    menu.addSeparator();
    menu.add(new AboutAction());
    menuBar.add(menu);

    setJMenuBar(menuBar);
  }

  class ClearOutputAction extends XAbstractAction {
    ClearOutputAction() {
      super(getBundleString("clear.all.action.name"));
      setSmallIcon(newIcon("/com/tc/admin/icons/clear_co.gif"));
    }

    public void actionPerformed(ActionEvent e) {
      l2OutView.setText("");
      webServer1OutView.setText("");
      webServer2OutView.setText("");
    }
  }

  class HelpAction extends XAbstractAction {
    HelpAction() {
      super(getBundleString("help.action.name"));
      setSmallIcon(newIcon("/com/tc/admin/icons/help.gif"));
    }

    public void actionPerformed(ActionEvent e) {
      showHelp();
    }
  }

  class ShowSplashAction extends XAbstractAction {
    ShowSplashAction() {
      super(getBundleString("show.splash.action.name"));
    }

    public void actionPerformed(ActionEvent e) {
      if (splashDialog != null) {
        WindowHelper.center(splashDialog, SessionIntegratorFrame.this);
        splashDialog.setVisible(true);
      } else {
        openSplashDialog(null);
      }
    }
  }

  class AboutAction extends XAbstractAction {
    AboutDialog aboutDialog;

    AboutAction() {
      super(getBundleString("about.action.label"));
    }

    public void actionPerformed(ActionEvent ae) {
      if (aboutDialog == null) {
        if (aboutDialog == null) {
          aboutDialog = new AboutDialog(SessionIntegratorFrame.this);
        }

        aboutDialog.pack();
        WindowHelper.center(aboutDialog, SessionIntegratorFrame.this);
        aboutDialog.setVisible(true);
      }
    }
  }

  private void showHelp() {
    try {
      openPage("file://" + StringUtils.replace(HELP_DOC, FS, "/"));
    } catch (Exception e) {
      configHelper.openError(getBundleString("show.help.error"), e);
    }
  }

  class ImportWebAppAction extends XAbstractAction {
    ImportWebAppAction() {
      super(getBundleString("import.webapp.action.name"));
      setSmallIcon(newIcon("/com/tc/admin/icons/import_wiz.gif"));
    }

    public void actionPerformed(ActionEvent e) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          importWebApp();
        }
      });
    }
  }

  void exportConfiguration() {
    FastFileChooser chooser = new FastFileChooser();
    File currentDir = getLastDirectory();

    chooser.setMultiSelectionEnabled(false);
    if (currentDir != null) {
      chooser.setCurrentDirectory(currentDir);
    }

    int returnVal = chooser.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      configHelper.saveAs(file, xmlPane.getText());
    }
  }

  class ExportConfigurationAction extends XAbstractAction {
    ExportConfigurationAction() {
      super(getBundleString("export.configuration.action.name"));
      setSmallIcon(newIcon("/com/tc/admin/icons/export_wiz.gif"));
    }

    public void actionPerformed(ActionEvent e) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          exportConfiguration();
        }
      });
    }
  }

  class QuitAction extends XAbstractAction {
    QuitAction() {
      super(getBundleString("quit.action.name"));
    }

    public void actionPerformed(ActionEvent ae) {
      quit();
    }
  }

  private void quit() {
    if (quitting) { return; }

    if (isXmlModified()) {
      if (querySaveConfig() == JOptionPane.CANCEL_OPTION) { return; }
    }

    if (anyWaiting()) {
      quitting = true;
      showQuittingDialog();
    } else if (anyReady()) {
      quitting = true;
      showQuittingDialog();

      try {
        webAppTreeModel.updateLinks(false, false);
        disableControls();
        stopAll();
      } catch (Exception e) {
        shutdown(-1);
      }
    } else {
      shutdown(0);
    }
  }

  private void shutdown() {
    shutdown(0);
  }

  private void shutdown(int exitCode) {
    System.exit(exitCode);
  }

  void showQuittingDialog() {
    JDialog dialog = new JDialog(this, getTitle());
    XLabel label = new XLabel(getBundleString("quitting.dialog.msg"));

    label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    dialog.getContentPane().add(label);
    dialog.pack();
    WindowHelper.center(dialog, this);
    dialog.setVisible(true);
  }

  class XmlChangeListener extends DocumentAdapter {
    @Override
    public void insertUpdate(DocumentEvent e) {
      setXmlModified(true);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      setXmlModified(true);
    }
  }

  void setXmlModified(boolean xmlModified) {
    this.xmlModified = xmlModified;

    if (configTabbedPane != null) {
      String label = XML_TAB_LABEL + (xmlModified ? "*" : "");
      configTabbedPane.setTitleAt(XML_TAB_INDEX, label);
    }
  }

  private boolean xmlModified;

  private boolean isXmlModified() {
    return xmlModified;
  }

  private File getLastDirectory() {
    String lastDir = getPreferences().get(LAST_DIR_PREF_KEY, null);
    return lastDir != null ? new File(lastDir) : null;
  }

  private void setLastDirectory(File dir) {
    getPreferences().put(LAST_DIR_PREF_KEY, dir.getAbsolutePath());
    storePreferences();
  }

  private void importWebApp() {
    FastFileChooser chooser = new FastFileChooser();
    File currentDir = getLastDirectory();

    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    chooser.setMultiSelectionEnabled(false);
    chooser.setFileFilter(WebAppFileFilter.getInstance());
    if (currentDir != null) {
      chooser.setCurrentDirectory(currentDir);
    }

    int returnVal = chooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();

      setLastDirectory(chooser.getCurrentDirectory());
      if ((file.isDirectory() && !isWebAppDir(file)) || (file.isFile() && !file.getName().endsWith(".war"))) {
        showPlainMessage(getBundleString("not.war.msg"));
        return;
      }

      installWebApp(file);
    }
  }

  private static boolean isWebAppDir(File dir) {
    File webInf = new File(dir, "WEB-INF");
    return webInf.exists() && new File(webInf, "web.xml").exists();
  }

  private String getSelectedServerName() {
    return "jetty6.1";
  }

  private String getSelectedServerLabel() {
    return "Jetty";
  }

  private String getSelectedServerStartupTrigger() {
    return "Started ";
  }

  private String getSelectedServerApplicationPath() {
    return "webapps";
  }

  private Map getenv() {
    try {
      Method method = System.class.getMethod("getenv", new Class[] {});
      if (method != null) { return (Map) method.invoke(null, new Object[] {}); }
    } catch (Throwable e) {/**/
    }

    return null;
  }

  private String[] getSelectedServerEnvironment() {
    Map sysEnv = getenv();

    if (sysEnv != null) {
      ArrayList list = new ArrayList();
      Iterator iter = sysEnv.keySet().iterator();

      while (iter.hasNext()) {
        String key = (String) iter.next();
        String val = (String) sysEnv.get(key);

        list.add(key + "=" + val);
      }

      try {
        File tmpfile = File.createTempFile("terracotta", null);
        list.add("TMPFILE=" + tmpfile.getAbsolutePath());
        tmpfile.delete();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }

      return (String[]) list.toArray(new String[0]);
    }

    return null;
  }

  private String getWebServer1Area() {
    return SANDBOX_ROOT + FS + getSelectedServerName() + FS + SERVER1_PORT + FS + getSelectedServerApplicationPath();
  }

  private String getWebServer2Area() {
    return SANDBOX_ROOT + FS + getSelectedServerName() + FS + SERVER2_PORT + FS + getSelectedServerApplicationPath();
  }

  private void installWebApp(File file) {
    try {
      boolean exists = new File(getWebServer1Area(), file.getName()).exists();

      installWebAppFile(file);
      addToModel(file);

      String msg = formatBundleString("install.webapp.success.msg", new Object[] { file });
      if (exists && anyReady()) {
        String explainMsg = getBundleString("install.webapp.restart.msg");
        queryRestart(msg + "\n\n" + explainMsg + "\n\n" + QUERY_RESTART_MSG);
      } else {
        showPlainMessage(msg);
      }

      if (splashDialog != null) {
        splashDialog.setVisible(false);
        splashDialog = null;
      }
    } catch (Exception e) {
      String msg = formatBundleString("install.webapp.failure.msg", new Object[] { file });
      configHelper.openError(msg, e);
    }
  }

  private static String contextFile(String warFile, String contextPath) {
    String s = "<?xml version=\"1.0\"  encoding=\"ISO-8859-1\"?>\n";
    s += "<Configure class=\"org.mortbay.jetty.webapp.WebAppContext\">\n";
    s += "  <Set name=\"contextPath\">/" + contextPath + "</Set>\n";
    s += "  <Set name=\"war\"><SystemProperty name=\"user.dir\"/>/webapps/" + warFile + "</Set>\n";
    s += "\n";
    s += "  <Property name=\"Server\">\n";
    s += "    <Call id=\"tcIdMgr\" name=\"getAttribute\">\n";
    s += "      <Arg>tcIdMgr</Arg>\n";
    s += "    </Call>\n";
    s += "  </Property>\n";
    s += "\n";
    s += "  <New id=\"tcmgr\" class=\"org.mortbay.terracotta.servlet.TerracottaSessionManager\">\n";
    s += "    <Set name=\"idManager\">\n";
    s += "      <Ref id=\"tcIdMgr\"/>\n";
    s += "    </Set>\n";
    s += "  </New>\n";
    s += "\n";
    s += "  <Set name=\"sessionHandler\">\n";
    s += "    <New class=\"org.mortbay.terracotta.servlet.TerracottaSessionHandler\">\n";
    s += "      <Arg><Ref id=\"tcmgr\"/></Arg>\n";
    s += "    </New>\n";
    s += "  </Set>\n";
    s += "  \n";
    s += "</Configure>\n";
    return s;
  }

  private void createJettyContext(File file, File webAppsDir) throws Exception {
    File contextDir = new File(webAppsDir.getParentFile(), "contexts");
    String name = file.getName();
    String contextPath = name;
    if (!file.isDirectory()) {
      int dot = name.indexOf('.');
      if (dot != -1) {
        contextPath = name.substring(0, dot);
      }
    }
    FileWriter fw = new FileWriter(new File(contextDir, contextPath + ".xml"));
    IOUtils.copy(new StringReader(contextFile(file.getName(), contextPath)), fw);
    fw.close();
  }

  private void installWebAppFile(File file) throws Exception {
    File webServer1Area = new File(getWebServer1Area());
    File webServer2Area = new File(getWebServer2Area());

    if (file.isFile()) {
      copyFileToDirectory(file, webServer1Area, false);
      copyFileToDirectory(file, webServer2Area, false);
    } else if (file.isDirectory()) {
      copyDirectory(file, webServer1Area);
      copyDirectory(file, webServer2Area);
    }
    createJettyContext(file, webServer1Area);
    createJettyContext(file, webServer2Area);
  }

  private void copyFileToDirectory(File file, File dir, boolean keepDate) throws IOException {
    if (dir.exists() && !dir.isDirectory()) {
      throw new IllegalArgumentException(sessionIntegratorContext.getString("destination.not.directory.msg"));
    } else {
      FileUtils.copyFile(file, new File(dir, file.getName()), keepDate);
    }
  }

  private void removeWebApp(WebApp webApp) throws Exception {
    removeWebAppFile(webApp);
    removeFromModel(webApp);
  }

  private void removeWebAppFile(WebApp webApp) throws Exception {
    File file = new File(webApp.getPath());
    String fileName = file.getName();
    String webServer1Area = getWebServer1Area();
    String webServer2Area = getWebServer2Area();

    safeDeleteFile(new File(webServer1Area, fileName));
    safeDeleteFile(new File(webServer2Area, fileName));

    if (fileName.endsWith(".war")) {
      String name = webApp.getName();
      safeDeleteFile(new File(webServer1Area, name));
      safeDeleteFile(new File(webServer2Area, name));
    }
    removeJettyContexts(webApp);
  }

  private void removeJettyContexts(WebApp webApp) {
    File webServer1Area = new File(getWebServer1Area());
    safeDeleteFile(new File(webServer1Area.getParentFile(), "contexts" + "/" + webApp.getName() + ".xml"));
    File webServer2Area = new File(getWebServer2Area());
    safeDeleteFile(new File(webServer2Area.getParentFile(), "contexts" + "/" + webApp.getName() + ".xml"));
  }

  private static void safeDeleteFile(File file) {
    if (file.exists()) {
      try {
        FileUtils.forceDelete(file);
      } catch (IOException ioe) {/**/
      }
    }
  }

  private static void safeCloseSocket(Socket socket) {
    try {
      socket.close();
    } catch (Exception e) {/**/
    }
  }

  void refresh(WebApp webApp) {
    File srcFile = new File(webApp.getPath());

    if (!srcFile.exists()) {
      Object[] args = { webApp.getName(), srcFile.getAbsolutePath() };
      String msg = formatBundleString("src.webapp.not.found.msg", args);
      int type = JOptionPane.YES_NO_OPTION;
      int answer = showConfirmDialog(msg, type);

      if (answer == JOptionPane.YES_OPTION) {
        remove(webApp);
      }
    } else {
      try {
        removeWebAppFile(webApp);
        installWebAppFile(srcFile);

        String msg = formatBundleString("refresh.success.msg", new Object[] { webApp.getPath() });
        queryRestart(msg + "\n\n" + (anyReady() ? QUERY_RESTART_MSG : QUERY_START_MSG));
      } catch (Exception e) {
        String msg = formatBundleString("refresh.failure.msg", new Object[] { webApp });
        configHelper.openError(msg, e);
      }
    }
  }

  /**
   * Update the timestamp on archived webapps or JSP's from exploded WAR's so they get recompiled. We do this after the
   * user has used the 'Servers' dialog because they may have changed the Java runtime from 1.5+ down to 1.4. If we
   * don't force the JSP's to be recompiled, they'll get a ClassFormatVersion exception when they hit the page.
   */
  void touch(WebApp webApp) {
    long time = System.currentTimeMillis();
    String[] serverAreas = new String[] { getWebServer1Area(), getWebServer2Area() };

    for (int j = 0; j < serverAreas.length; j++) {
      File destFile = new File(serverAreas[j], webApp.getName());

      if (!destFile.isDirectory()) {
        destFile.setLastModified(time);
      } else {
        Collection fileSet = FileUtils.listFiles(destFile, new String[] { "jsp" }, true);
        File[] files = FileUtils.convertFileCollectionToFileArray(fileSet);

        for (int i = 0; i < files.length; i++) {
          files[i].setLastModified(time);
        }
      }
    }
  }

  void remove(WebApp webApp) {
    if (anyReady()) {
      showPlainMessage(getBundleString("cannot.remove.while.running.msg"));
    } else {
      try {
        removeWebApp(webApp);

        File srcFile = new File(webApp.getPath());
        String msg = formatBundleString("remove.success.msg", new Object[] { srcFile.getAbsolutePath() });
        showPlainMessage(msg);
      } catch (Exception e) {
        String msg = formatBundleString("remove.failure.msg", new Object[] { webApp });
        configHelper.openError(msg, e);
      }
    }
  }

  private static void copyDirectory(File fromDir, File toDir) throws Exception {
    File destDir = new File(toDir, fromDir.getName());

    if (destDir.exists()) {
      FileUtils.cleanDirectory(destDir);
    }

    Collection fileSet = FileUtils.listFiles(fromDir, null, true);
    File[] files = FileUtils.convertFileCollectionToFileArray(fileSet);
    String prefix = fromDir.getAbsolutePath() + FS;
    int prefixLen = prefix.length();
    String relPath;

    for (int i = 0; i < files.length; i++) {
      relPath = files[i].getAbsolutePath().substring(prefixLen);
      FileUtils.copyFile(files[i], new File(destDir, relPath));
    }
  }

  private void addToModel(File file) throws Exception {
    String name = file.getName();
    String path = file.getAbsolutePath();
    int dot = name.lastIndexOf('.');

    if (dot != -1) {
      name = name.substring(0, dot);
    }

    path = StringUtils.replace(path, FS, "/");

    Exception err = null;
    try {
      WebAppNode webAppNode = webAppTreeModel.add(new WebApp(name, path));
      TreePath webAppPath = new TreePath(webAppNode.getPath());

      webAppTree.expandPath(webAppPath);
      webAppTree.setSelectionPath(webAppPath);

      if (configHelper.ensureWebApplication(name)) {
        configHelper.save();
        initXmlPane();
      }
      getPreferences().node("WebApps").put(name, path);
      storePreferences();
    } catch (Exception e) {
      err = e;
    }

    if (err != null) { throw err; }
  }

  private void removeFromModel(WebApp webApp) throws Exception {
    File file = new File(webApp.getPath());
    String name = file.getName();
    int dot = name.lastIndexOf('.');

    if (dot != -1) {
      name = name.substring(0, dot);
    }

    Exception err = null;
    try {
      webAppTreeModel.remove(name);
      if (configHelper.removeWebApplication(name)) {
        configHelper.save();
        initXmlPane();
      }
      getPreferences().node("WebApps").remove(name);
      storePreferences();
    } catch (Exception e) {
      err = e;
    }

    if (err != null) { throw err; }
  }

  void saveAndStart() {
    configHelper.save();
    setXmlModified(false);
    startButton.doClick();
  }

  private boolean isDsoEnabled() {
    return dsoEnabled;
  }

  private void setDsoEnabled(boolean enabled) {
    getPreferences().putBoolean(DSO_ENABLED_PREF_KEY, dsoEnabled = enabled);
    storePreferences();

    dsoEnabledToggle.setSelected(enabled);

    l2Label.setIcon(null);
    l2Label.setText(L2_LABEL + (enabled ? "" : DISABLED_LABEL));
    l2Label.setEnabled(dsoEnabled);
    l2OutView.setEnabled(dsoEnabled);

    setMonitorTabEnabled(enabled);

    startButton.setEnabled(enabled || isWebServer1Enabled() || isWebServer2Enabled());

    if (isL2Ready()) {
      queryRestart();
    }
  }

  private void selectControlTab() {
    tabbedPane.setSelectedIndex(CONTROL_TAB_INDEX);
  }

  private boolean isConfigTabSelected() {
    return (tabbedPane.getSelectedIndex() == CONFIG_TAB_INDEX);
  }

  private void setConfigTabEnabled(boolean enabled) {
    tabbedPane.setEnabledAt(CONFIG_TAB_INDEX, enabled);
  }

  private void setConfigTabForeground(Color fg) {
    tabbedPane.setForegroundAt(CONFIG_TAB_INDEX, fg);
  }

  private void setMonitorTabEnabled(boolean enabled) {
    tabbedPane.setEnabledAt(MONITOR_TAB_INDEX, enabled);
  }

  private int querySaveConfig() {
    return querySaveConfig(JOptionPane.YES_NO_CANCEL_OPTION);
  }

  private int querySaveConfig(int msgType) {
    String msg = getBundleString("query.save.config.msg");
    int answer = showConfirmDialog(msg, msgType);

    if (answer == JOptionPane.YES_OPTION) {
      saveConfig();
    }

    return answer;
  }

  private int queryRestart() {
    return queryRestart(QUERY_RESTART_MSG);
  }

  private int queryRestart(String msg) {
    int type = JOptionPane.YES_NO_OPTION;
    int answer = showConfirmDialog(msg, type);

    if (answer == JOptionPane.YES_OPTION) {
      startButton.doClick();
    }

    return answer;
  }

  private WebApp[] getWebApps() {
    Preferences webAppsPref = getPreferences().node("WebApps");
    String[] webAppNames = {};

    try {
      webAppNames = webAppsPref.keys();
    } catch (Exception e) {/**/
    }

    if (webAppNames.length == 0) {
      webAppsPref.put("Cart", "");
      webAppsPref.put("DepartmentTaskList", "");
      webAppsPref.put("Townsend", "");
    }
    storePreferences();

    try {
      webAppNames = webAppsPref.keys();
    } catch (Exception e) {/**/
    }

    ArrayList appList = new ArrayList();
    for (String name : webAppNames) {
      appList.add(new WebApp(name, webAppsPref.get(name, "")));
    }

    return WebAppComparable.sort((WebApp[]) appList.toArray(new WebApp[0]));
  }

  private void disableEditorPanels() {
    instrumentedClassesPanel.setEnabled(false);
    transientFieldsPanel.setEnabled(false);
    bootClassesPanel.setEnabled(false);
    modulesPanel.setEnabled(false);
  }

  private void setupEditorPanels() {
    try {
      TcConfig config = configHelper.getConfig();

      if (config == ConfigHelper.BAD_CONFIG) {
        disableEditorPanels();
      } else {
        DsoApplication dsoApp = config.getApplication().getDso();

        instrumentedClassesPanel.setup(dsoApp);
        transientFieldsPanel.setup(dsoApp);
        bootClassesPanel.setup(dsoApp);
        modulesPanel.setup(config.getClients());
      }
    } catch (Exception e) {
      configHelper.openError(getBundleString("configuration.load.failure.msg"), e);
    }
  }

  private void initXmlPane() {
    Document xmlDocument = xmlPane.getDocument();

    xmlDocument.removeDocumentListener(xmlChangeListener);
    xmlPane.load(configHelper.getConfigFilePath());
    xmlDocument.addDocumentListener(xmlChangeListener);
    setXmlModified(false);
  }

  private void updateXmlPane() {
    Document xmlDocument = xmlPane.getDocument();

    xmlDocument.removeDocumentListener(xmlChangeListener);
    xmlPane.set(configHelper.getConfigText());
    xmlDocument.addDocumentListener(xmlChangeListener);
    setXmlModified(true);
  }

  void setConfigErrors(List errorList) {
    configProblemTableModel.setErrors(errorList);
    setConfigTabForeground(errorList.size() > 0 ? Color.RED : null);
  }

  public static void openPath(String path) throws IOException {
    if (Os.isWindows()) {
      String[] cmd = { "cmd.exe", "/C", path };
      Runtime.getRuntime().exec(cmd);
    } else {
      openPage(path);
    }
  }

  public static void openPage(String page) {
    BrowserLauncher.openURL(page);
  }

  void startSystem() {
    try {
      if (isXmlModified()) {
        if (querySaveConfig() == JOptionPane.CANCEL_OPTION) { return; }
      }
      webAppTreeModel.updateLinks(false, false);
      disableControls();
      startAll();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void startAll() throws Exception {
    restarting = anyReady();
    startServers();
  }

  private void startServers() {
    trace("startServers");

    l2Label.setIcon(l2Status.isReady() ? waitingIcon : null);

    if (restarting) {
      if (isL2Ready()) {
        l2Label.setIcon(waitingIcon);
        l2Label.setText(L2_LABEL + WAITING_LABEL);

        if (webServer1Status.isReady() || webServer2Status.isReady()) {
          stopWebServers();
        } else {
          startL2();
        }
      } else {
        startWebServers();
      }
    } else {
      if (isDsoEnabled()) {
        if (isWebServer1Enabled()) {
          webServer1Label.setIcon(waitingIcon);
          webServer1Label.setText(getWebServer1Label() + WAITING_LABEL);
        }
        if (isWebServer2Enabled()) {
          webServer2Label.setIcon(waitingIcon);
          webServer2Label.setText(getWebServer2Label() + WAITING_LABEL);
        }
        startL2();
      } else {
        startWebServers();
      }
    }
  }

  private boolean isWebServer1Enabled() {
    return webServer1Enabled;
  }

  private void setWebServer1Enabled(boolean enabled) {
    getPreferences().putBoolean(WEBSERVER1_ENABLED_PREF_KEY, webServer1Enabled = enabled);
    storePreferences();

    startButton.setEnabled(enabled || isWebServer2Enabled() || isDsoEnabled());
  }

  private boolean isWebServer2Enabled() {
    return webServer2Enabled;
  }

  private void setWebServer2Enabled(boolean enabled) {
    getPreferences().putBoolean(WEBSERVER2_ENABLED_PREF_KEY, webServer2Enabled = enabled);
    storePreferences();

    startButton.setEnabled(enabled || isWebServer1Enabled() || isDsoEnabled());
  }

  private String getWebServer1Label() {
    return getSelectedServerLabel() + "-" + SERVER1_PORT;
  }

  private String getWebServer2Label() {
    return getSelectedServerLabel() + "-" + SERVER2_PORT;
  }

  private void startWebServers() {
    if (isWebServer1Enabled()) {
      startWebServer1();
    }
    if (isWebServer2Enabled()) {
      startWebServer2();
    }
  }

  private void stopWebServers() {
    trace("stopWebServers");

    if (isWebServer1Ready()) {
      stopWebServer1();
    }
    if (isWebServer2Ready()) {
      stopWebServer2();
    }
  }

  // Begin -- L2 process control support

  private boolean isL2Accessible() {
    try {
      if (l2ConnectManager.testIsConnected()) { return true; }
    } catch (Exception e) {/**/
    }

    return false;
  }

  private void testShutdownL2() {
    if (isL2Accessible()) {
      stopL2();
    }
  }

  private void startL2() {
    if (System.getProperty("tc.server") != null) {
      l2OutView.append("Using external Terracotta servers: " + System.getProperty("tc.server"));
      startWebServers();
      return;
    }

    trace("startL2");

    if (l2Monitor != null) {
      l2Monitor.cancel();
      while (true) {
        try {
          l2Monitor.join(0);
          break;
        } catch (InterruptedException ie) {/**/
        }
      }
      l2Monitor = null;
    }

    if (isL2Ready()) {
      l2Status.setRestarting(true);
      restartL2();
      return;
    }

    if (isDsoEnabled()) {
      _startL2();
    }
  }

  private void _startL2() {
    trace("_startL2");

    l2Status.setWaiting();
    l2Label.setIcon(waitingIcon);
    l2Label.setText(L2_LABEL + STARTING_LABEL);
    l2OutView.setListener(l2StartupListener);
    l2OutView.setListenerTrigger(L2_STARTUP_TRIGGER);
    startL2AndNotify(L2_STARTUP_SCRIPT, l2OutView, l2StartupListener);
  }

  private void startL2AndNotify(final String startScript, final ProcessOutputView outView,
                                final StartupListener startupListener) {
    trace("Starting L2");

    Process process;
    try {
      process = invokeScript(startScript, new String[] { getSelectedServerName() });
      IOUtils.closeQuietly(process.getOutputStream());
      new ProcessMonitor(process, new ProcessTerminationListener() {
        public void processTerminated(int exitCode) {
          if (debug) {
            outView.append("L2 terminated with exitCode=" + exitCode);
          }
          if (exitCode != 0) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                startupListener.processFailed();
              }
            });
          }
        }
      });
    } catch (Exception e) {
      startupListener.startupError(e);
      return;
    }

    l2OutView.start(process);

    new L2StartupMonitor(process, startupListener).start();
  }

  class L2StartupMonitor extends Thread {
    private Process               process;
    private final StartupListener startupListener;

    L2StartupMonitor(Process process, StartupListener listener) {
      super();
      this.process = process;
      startupListener = listener;
    }

    @Override
    public void run() {
      while (true) {
        if (process != null) {
          try {
            int exitCode = process.exitValue();

            if (exitCode != 0) {
              SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  startupListener.processFailed();
                }
              });
              return;
            } else {
              process = null;
            }
          } catch (IllegalThreadStateException itse) {/**/
          }

          if (isL2Accessible()) { return; }

          ThreadUtil.reallySleep(1000);
        }
      }
    }
  }

  class L2StartupListener implements StartupListener, OutputStreamListener {
    public void processFailed() {
      trace("L2.processFailed");

      if (webServer1Status.isReady()) {
        stopWebServer1();
      } else {
        webServer1Label.setIcon(null);
        webServer1Label.setText(getWebServer1Label());
      }

      if (webServer2Status.isReady()) {
        stopWebServer2();
      } else {
        webServer2Label.setIcon(null);
        webServer2Label.setText(getWebServer2Label());
      }

      l2Label.setIcon(stoppedIcon);
      l2Label.setText(L2_LABEL + FAILED_LABEL);
      l2Status.setFailed();

      testEnableControls();
    }

    public void startupError(Exception e) {
      trace("L2.startupError exception=" + e.getMessage());
      if (debug) e.printStackTrace();

      webServer1Label.setIcon(null);
      webServer1Label.setText(getWebServer1Label());
      webServer2Label.setIcon(null);
      webServer2Label.setText(getWebServer2Label());

      l2Label.setIcon(stoppedIcon);
      l2Label.setText(L2_LABEL + FAILED_LABEL);
      l2Status.setFailed();

      testEnableControls();
    }

    public void triggerEncountered() {
      l2OutView.setListener(null);
      processReady();
    }

    public void processReady() {
      trace("L2.processReady");

      l2Label.setIcon(readyIcon);
      l2Label.setText(L2_LABEL + READY_LABEL);
      l2Status.setReady();

      startWebServers();
      waitForMBean();

      l2Monitor = new L2ShutdownMonitor(l2ShutdownListener);
      l2Monitor.start();

      testEnableControls();
    }
  }

  private void restartL2() {
    stopL2(isDsoEnabled());
  }

  private void stopL2() {
    stopL2(false);
  }

  private void stopL2(boolean restart) {
    if (System.getProperty("tc.server") != null) {
      if (webServer1Status.isReady() || webServer2Status.isReady()) {
        stopWebServers();
      }
      return;
    }

    if (l2Monitor != null) {
      l2Monitor.cancel();
      while (true) {
        try {
          l2Monitor.join(0);
          break;
        } catch (InterruptedException ie) {/**/
        }
      }
      l2Monitor = null;
    }

    l2Status.setWaiting();
    l2Label.setIcon(waitingIcon);
    l2Label.setText(L2_LABEL + STOPPING_LABEL);
    l2ShutdownListener.setRestart(restart);

    stopL2AndNotify(L2_SHUTDOWN_SCRIPT, l2OutView, configHelper.getJmxPort(), l2ShutdownListener);
  }

  private void stopL2AndNotify(final String stopScript, final ProcessOutputView outView, final int port,
                               final ShutdownListener shutdownListener) {
    trace("Stopping L2");

    Process process;
    try {
      process = invokeScript(stopScript, new String[] { getSelectedServerName() });
    } catch (Exception e) {
      shutdownListener.processError(e);
      return;
    }

    l2Monitor = new L2ShutdownMonitor(process, shutdownListener);
    l2Monitor.start();
  }

  class L2ShutdownListener implements ShutdownListener {
    boolean restart = false;

    void setRestart(boolean restart) {
      this.restart = restart;
    }

    public void processError(Exception e) {
      trace("L2.processError");
      if (debug) e.printStackTrace();

      if (quitting) {
        l2Label.setIcon(readyIcon);
        l2Label.setText(L2_LABEL + READY_LABEL);
        l2Status.setReady();
      } else {
        l2Label.setIcon(stoppedIcon);
        l2Label.setText(L2_LABEL + FAILED_LABEL);
        l2Status.setFailed();
      }

      testEnableControls();
    }

    public void processFailed(String errorBuf) {
      trace("L2.processFailed");

      l2OutView.append(errorBuf);

      if (quitting) {
        l2Label.setIcon(readyIcon);
        l2Label.setText(L2_LABEL + READY_LABEL);
        l2Status.setReady();
      } else {
        l2Label.setIcon(stoppedIcon);
        l2Label.setText(L2_LABEL + FAILED_LABEL);
        l2Status.setFailed();
      }

      testEnableControls();
    }

    public void processStopped() {
      l2Monitor = null;
      l2ConnectManager.getConnectionContext().reset();
      l2Status.setInactive();

      if (restart) {
        startL2();
        restart = false;
      } else {
        if (webServer1Status.isReady() || webServer2Status.isReady()) {
          stopWebServers();
        }

        l2Label.setIcon(stoppedIcon);
        l2Label.setText(L2_LABEL + STOPPED_LABEL);
      }

      testEnableControls();
    }
  }

  class L2ShutdownMonitor extends Thread {
    private Process                process;
    private final ShutdownListener shutdownListener;
    private boolean                stop;

    L2ShutdownMonitor(ShutdownListener listener) {
      this(null, listener);
    }

    L2ShutdownMonitor(Process process, ShutdownListener listener) {
      super();
      this.process = process;
      shutdownListener = listener;
    }

    @Override
    public void run() {
      ProcessWaiter waiter = null;

      if (process != null) {
        waiter = new ProcessWaiter(process);
        waiter.start();
      }

      while (!stop) {
        if (process != null) {
          try {
            int exitCode = process.exitValue();

            if (exitCode != 0) {
              final String errorBuf = waiter.getErrorBuffer();
              SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  shutdownListener.processFailed(errorBuf);
                }
              });
              return;
            } else {
              process = null;
            }
          } catch (IllegalThreadStateException itse) {/**/
          }
        }

        if (!stop) {
          if (!isL2Accessible()) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                shutdownListener.processStopped();
              }
            });
            return;
          }
        }

        try {
          sleep(1000);
        } catch (InterruptedException ignore) {/**/
        }
      }
    }

    void cancel() {
      stop = true;
    }
  }

  class DSOAppEventListener implements NotificationListener {
    public void handleNotification(Notification notification, Object handback) {
      final Object event = notification.getSource();

      if (event instanceof NonPortableObjectEvent) {
        handlingAppEvent = true;

        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            SessionIntegratorFrame.this.toFront();
            handleNonPortableReason((NonPortableObjectEvent) event);
          }
        });
      }
    }
  }

  private static final String NON_PORTABLE_DIALOG_SIZE = "NonPortableDialogSize";

  private void handleNonPortableReason(NonPortableObjectEvent event) {
    Preferences prefs = getPreferences();
    NonPortableObjectPanel panel = new NonPortableObjectPanel(this);
    JDialog dialog = new JDialog(this, this.getTitle(), true);
    Container cp = dialog.getContentPane();
    cp.setLayout(new BorderLayout());
    cp.add(panel);
    panel.setEvent(event);

    XSplitPane splitter = panel.getIssuesSplitter();
    splitter.setPreferences(prefs.node("IssuesSplitter"));

    String s;
    if ((s = prefs.get(NON_PORTABLE_DIALOG_SIZE, null)) != null) {
      dialog.setSize(parseSizeString(s));
    } else {
      dialog.pack();
    }

    WindowHelper.center(dialog, this);
    dialog.setVisible(true);
    prefs.put(NON_PORTABLE_DIALOG_SIZE, getSizeString(dialog));
    storePreferences();
    handlingAppEvent = false;
    return;
  }

  private int showConfirmDialog(Object msg, int msgType) {
    return JOptionPane.showConfirmDialog(this, msg, getTitle(), msgType);
  }

  private void showPlainMessage(Object msg) {
    JOptionPane.showConfirmDialog(this, msg, getTitle(), JOptionPane.PLAIN_MESSAGE);
  }

  class L2ConnectListener implements ConnectionListener {
    public void handleConnection() {/**/
    }

    public void handleException() {/**/
    }
  }

  private void waitForMBean() {
    new Thread() {
      @Override
      public void run() {
        while (true) {
          try {
            if (l2ConnectManager.testIsConnected()) {
              ConnectionContext cc = l2ConnectManager.getConnectionContext();
              ObjectName on = cc.queryName(L2MBeanNames.DSO_APP_EVENTS.getCanonicalName());

              if (on != null) {
                cc.addNotificationListener(on, new DSOAppEventListener());
                return;
              }
            }
          } catch (Exception e) {/**/
          }

          try {
            sleep(500);
          } catch (InterruptedException ie) {/**/
          }
        }
      }
    }.start();
  }

  private boolean isL2Ready() {
    return l2Status.isReady();
  }

  // End -- L2 process control support

  // Begin -- Server1 process control support

  private void testShutdownWebServer1() {
    try {
      safeCloseSocket(new Socket("localhost", SERVER1_PORT));
      stopWebServer1();
    } catch (IOException ioe) {/**/
    }
  }

  private void startWebServer1() {
    if (webServer1Monitor != null) {
      webServer1Monitor.cancel();
      webServer1Monitor = null;
    }

    if (isWebServer1Ready()) {
      webServer1Status.setRestarting(true);
      restartWebServer1();
      return;
    }

    webServer1Label.setIcon(waitingIcon);
    webServer1Label.setText(getWebServer1Label() + STARTING_LABEL);
    webServer1Status.setWaiting();
    webServer1OutView.setListener(webServer1StartupListener);
    webServer1OutView.setListenerTrigger(getSelectedServerStartupTrigger());
    startWebServerAndNotify(webServer1OutView, SERVER1_PORT, webServer1StartupListener);
  }

  class WebServer1StartupListener implements StartupListener, OutputStreamListener {
    public void startupError(Exception e) {
      trace(getSelectedServerLabel() + "1.startupError exception=" + e.getMessage());

      webServer1Label.setIcon(stoppedIcon);
      webServer1Label.setText(getWebServer1Label() + FAILED_LABEL);
      webServer1Status.setFailed();

      testEnableControls();
    }

    public void processFailed() {
      trace(getSelectedServerLabel() + ".processFailed");

      webServer1Label.setIcon(stoppedIcon);
      webServer1Label.setText(getWebServer1Label() + FAILED_LABEL);
      webServer1Status.setFailed();

      testEnableControls();
    }

    public void triggerEncountered() {
      webServer1OutView.setListener(null);
      processReady();
    }

    public void processReady() {
      trace(getSelectedServerLabel() + "1.processReady");

      webServer1Status.setReady();
      webServer1Label.setIcon(readyIcon);
      webServer1Label.setText(getWebServer1Label() + READY_LABEL);

      webServer1Monitor = new WebServerShutdownMonitor(SERVER1_PORT, webServer1ShutdownListener);
      webServer1Monitor.start();

      testEnableControls();
    }
  }

  private void restartWebServer1() {
    stopWebServer1(true);
  }

  private boolean isWebServer1Ready() {
    return webServer1Status.isReady();
  }

  private void stopWebServer1() {
    stopWebServer1(false);
  }

  private void stopWebServer1(boolean restart) {
    if (webServer1Monitor != null) {
      webServer1Monitor.cancel();
      webServer1Monitor = null;
    }

    webServer1Label.setIcon(waitingIcon);
    webServer1Label.setText(getWebServer1Label() + STOPPING_LABEL);
    webServer1Status.setWaiting();
    webServer1ShutdownListener.setRestart(restart);

    stopWebServerAndNotify(webServer1OutView, SERVER1_PORT, webServer1ShutdownListener);
    // if (m_jetty1Proc != null) {
    // m_webServer1OutView.append("Terminating jetty:9081...");
    // try {
    // stopJetty(9082);
    // } catch (Exception e) {
    // m_webServer1OutView.append(e.getMessage());
    // }
    // // m_jetty1Proc.destroy();
    // }
    // m_jetty1Proc = null;
  }

  class WebServer1ShutdownListener implements ShutdownListener {
    boolean restart = false;

    void setRestart(boolean restart) {
      this.restart = restart;
    }

    public void processError(Exception e) {
      trace(getSelectedServerLabel() + "1.processError exception=" + e.getMessage());
      if (debug) e.printStackTrace();

      if (!quitting) {
        webServer1Status.setReady();
        webServer1Label.setIcon(readyIcon);
        webServer1Label.setText(getWebServer1Label() + READY_LABEL);
      } else {
        webServer1Status.setFailed();
        webServer1Label.setIcon(stoppedIcon);
        webServer1Label.setText(getWebServer1Label() + FAILED_LABEL);
      }

      testEnableControls();
    }

    public void processFailed(String errorBuf) {
      trace(getSelectedServerLabel() + "1.processFailed");

      webServer1OutView.append(errorBuf);

      if (!quitting) {
        webServer1Status.setReady();
        webServer1Label.setIcon(readyIcon);
        webServer1Label.setText(getWebServer1Label() + READY_LABEL);
      } else {
        webServer1Status.setFailed();
        webServer1Label.setIcon(stoppedIcon);
        webServer1Label.setText(getWebServer1Label() + FAILED_LABEL);
      }

      testEnableControls();
    }

    public void processStopped() {
      trace(getSelectedServerLabel() + "1.processStopped");

      webServer1Monitor = null;
      webServer1Status.setInactive();
      if (restarting && isDsoEnabled()) {
        webServer1Label.setText(getWebServer1Label() + WAITING_LABEL);
        if (webServer2Status.isInactive()) {
          startL2();
        }
      } else {
        if (restart) {
          startWebServer1();
        } else {
          webServer1Label.setIcon(stoppedIcon);
          webServer1Label.setText(getWebServer1Label() + STOPPED_LABEL);
        }
      }

      testEnableControls();
    }
  }

  private void toggleWebServer1() {
    if (isWebServer1Ready()) {
      stopWebServer1();
    } else {
      startWebServer1();
    }
  }

  // End -- WebServer1 process control support

  // Begin -- WebServer2 process control support

  private void testShutdownWebServer2() {
    try {
      safeCloseSocket(new Socket("localhost", SERVER2_PORT));
      stopWebServer2();
    } catch (IOException ioe) {/**/
    }
  }

  private void startWebServer2() {
    if (webServer2Monitor != null) {
      webServer2Monitor.cancel();
      webServer2Monitor = null;
    }

    if (isWebServer2Ready()) {
      webServer2Status.setRestarting(true);
      restartWebServer2();
      return;
    }

    webServer2Label.setIcon(waitingIcon);
    webServer2Label.setText(getWebServer2Label() + STARTING_LABEL);
    webServer2Status.setWaiting();
    webServer2OutView.setListener(webServer2StartupListener);
    webServer2OutView.setListenerTrigger(getSelectedServerStartupTrigger());
    startWebServerAndNotify(webServer2OutView, SERVER2_PORT, webServer2StartupListener);
  }

  class WebServer2StartupListener implements StartupListener, OutputStreamListener {
    public void startupError(Exception e) {
      trace(getSelectedServerLabel() + "2.startupError exception=" + e.getMessage());

      webServer2Label.setIcon(stoppedIcon);
      webServer2Label.setText(getWebServer2Label() + FAILED_LABEL);
      webServer2Status.setFailed();

      testEnableControls();
    }

    public void processFailed() {
      trace(getSelectedServerLabel() + "2.processFailed");

      webServer2Label.setIcon(stoppedIcon);
      webServer2Label.setText(getWebServer2Label() + FAILED_LABEL);
      webServer2Status.setFailed();

      testEnableControls();
    }

    public void triggerEncountered() {
      webServer2OutView.setListener(null);
      processReady();
    }

    public void processReady() {
      trace(getSelectedServerLabel() + "2.processReady");

      webServer2Status.setReady();
      webServer2Label.setIcon(readyIcon);
      webServer2Label.setText(getWebServer2Label() + READY_LABEL);

      webServer2Monitor = new WebServerShutdownMonitor(SERVER2_PORT, webServer2ShutdownListener);
      webServer2Monitor.start();

      testEnableControls();
    }
  }

  private void restartWebServer2() {
    stopWebServer2(true);
  }

  private boolean isWebServer2Ready() {
    return webServer2Status.isReady();
  }

  private void stopWebServer2() {
    stopWebServer2(false);
  }

  private void stopWebServer2(boolean restart) {
    if (webServer2Monitor != null) {
      webServer2Monitor.cancel();
      webServer2Monitor = null;
    }

    webServer2Label.setIcon(waitingIcon);
    webServer2Label.setText(getWebServer2Label() + STOPPING_LABEL);
    webServer2Status.setWaiting();
    webServer2ShutdownListener.setRestart(restart);

    stopWebServerAndNotify(webServer2OutView, SERVER2_PORT, webServer2ShutdownListener);
    // if (m_jetty2Proc != null) {
    // m_webServer1OutView.append("Terminating jetty:9082...");
    // try {
    // stopJetty(9083);
    // } catch (Exception e) {
    // m_webServer2OutView.append(e.getMessage());
    // }
    // // m_jetty2Proc.destroy();
    // }
    // m_jetty2Proc = null;
  }

  class WebServer2ShutdownListener implements ShutdownListener {
    boolean restart = false;

    void setRestart(boolean restart) {
      this.restart = restart;
    }

    public void processError(Exception e) {
      trace(getSelectedServerLabel() + "2.processError");
      if (debug) e.printStackTrace();

      if (!quitting) {
        webServer2Status.setReady();
        webServer2Label.setIcon(readyIcon);
        webServer2Label.setText(getWebServer2Label() + READY_LABEL);
      } else {
        webServer2Status.setFailed();
        webServer2Label.setIcon(stoppedIcon);
        webServer2Label.setText(getWebServer2Label() + FAILED_LABEL);
      }

      testEnableControls();
    }

    public void processFailed(String errorBuf) {
      trace(getSelectedServerLabel() + "2.processFailed");

      webServer2OutView.append(errorBuf);

      if (!quitting) {
        webServer2Status.setReady();
        webServer2Label.setIcon(readyIcon);
        webServer2Label.setText(getWebServer2Label() + READY_LABEL);
      } else {
        webServer2Status.setFailed();
        webServer2Label.setIcon(stoppedIcon);
        webServer2Label.setText(getWebServer2Label() + FAILED_LABEL);
      }

      testEnableControls();
    }

    public void processStopped() {
      trace(getSelectedServerLabel() + "2.processStopped");

      webServer2Monitor = null;
      webServer2Status.setInactive();
      if (restarting && isDsoEnabled()) {
        webServer2Label.setText(getWebServer2Label() + WAITING_LABEL);
        if (webServer1Status.isInactive()) {
          startL2();
        }
      } else {
        if (restart) {
          startWebServer2();
        } else {
          webServer2Label.setIcon(stoppedIcon);
          webServer2Label.setText(getWebServer2Label() + STOPPED_LABEL);
        }
      }

      testEnableControls();
    }
  }

  private void toggleWebServer2() {
    if (isWebServer2Ready()) {
      stopWebServer2();
    } else {
      startWebServer2();
    }
  }

  // End -- WebServer2 process control support

  // Being -- Process control support

  private Process startWebServerAndNotify(final ProcessOutputView outView, final int port,
                                          final StartupListener startupListener) {
    trace("Starting " + getSelectedServerLabel() + "-" + port);

    Process process;
    try {
      // String dso = isDsoEnabled() ? "dso" : "nodso";
      // String[] args = new String[] { getSelectedServerName(), Integer.toString(port), dso };

      // process = invokeScript(WEBSERVER_STARTUP_SCRIPT, args);
      process = startJetty(port);
      IOUtils.closeQuietly(process.getOutputStream());
      new ProcessMonitor(process, new ProcessTerminationListener() {
        public void processTerminated(int exitCode) {
          if (debug) {
            outView.append(getSelectedServerLabel() + "-" + port + " terminated with exitCode=" + exitCode);
          }
          if (exitCode != 0) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                startupListener.processFailed();
              }
            });
          }
        }
      });
    } catch (Exception e) {
      startupListener.startupError(e);
      return null;
    }

    outView.start(process);

    new WebServerStartupMonitor(process, port, startupListener).start();

    return process;
  }

  private static final int STOP_PORT_OFFSET = 100;

  private File findJettyTerracottaJar(File jettyHome) {
    File tcLibDir = new File(jettyHome, "lib/terracotta");
    for (File file : tcLibDir.listFiles()) {
      if (file.getName().endsWith(".jar")) { return file; }
    }
    throw new RuntimeException("Can't find Jetty Terracotta Library in '" + tcLibDir + "'");
  }

  private Process startJetty(int port) throws Exception {
    String[] env = getSelectedServerEnvironment();
    File wd = new File(SANDBOX_ROOT, "jetty6.1" + File.separatorChar + port);
    String bootPath = getBootPath();
    File jettyHome = getJettyHome();
    File startJar = new File(jettyHome, "start.jar");
    String[] cmdarray;
    int stopPort = port + STOP_PORT_OFFSET;
    if (isDsoEnabled()) {
      File jettyTerracottaJar = findJettyTerracottaJar(jettyHome);
      cmdarray = new String[] { getJavaCmd().getAbsolutePath(),
          "-Djetty.class.path=" + jettyTerracottaJar.getAbsolutePath(), "-Dtc.config=../tc-config.xml",
          "-Dtc.install-root=" + getInstallRoot().getAbsolutePath(), "-Xbootclasspath/p:" + bootPath,
          "-Djetty.home=" + jettyHome.getAbsolutePath(), "-DSTOP.PORT=" + stopPort, "-DSTOP.KEY=secret", "-jar",
          startJar.getAbsolutePath(), "tc-conf.xml" };
    } else {
      cmdarray = new String[] { getJavaCmd().getAbsolutePath(), "-Djetty.home=" + jettyHome.getAbsolutePath(),
          "-DSTOP.PORT=" + stopPort, "-DSTOP.KEY=secret", "-jar", startJar.getAbsolutePath(), "conf.xml" };
    }
    return Runtime.getRuntime().exec(cmdarray, env, wd);
  }

  private Process stopJetty(int port) throws Exception {
    String[] env = getSelectedServerEnvironment();
    File wd = new File(SANDBOX_ROOT, "jetty6.1" + File.separatorChar + port);
    File jettyHome = getJettyHome();
    File startJar = new File(jettyHome, "start.jar");
    int stopPort = port + STOP_PORT_OFFSET;
    String[] cmdarray = { getJavaCmd().getAbsolutePath(), "-Djetty.home=" + jettyHome.getAbsolutePath(),
        "-DSTOP.PORT=" + stopPort, "-DSTOP.KEY=secret", "-jar", startJar.getAbsolutePath(), "--stop" };
    return Runtime.getRuntime().exec(cmdarray, env, wd);
  }

  File m_installRoot;

  protected File getInstallRoot() {
    if (m_installRoot == null) {
      m_installRoot = new File(System.getProperty("tc.install-root").trim());
    }
    return m_installRoot;
  }

  File m_jettyHome;

  /**
   * Locate Jetty installation in kit.
   */
  protected File getJettyHome() {
    if (m_jettyHome == null) {
      File vendorsDir = new File(getInstallRoot(), "vendors");
      for (File file : vendorsDir.listFiles()) {
        if (file.isDirectory() && file.getName().startsWith("jetty-")) {
          m_jettyHome = file;
          return m_jettyHome;
        }
      }
      throw new RuntimeException("Can't find Jetty installation under '" + vendorsDir + '"');
    }
    return m_jettyHome;
  }

  File m_bootPath;

  protected String getBootPath() throws UnsupportedVMException {
    if (m_bootPath == null) {
      File bootPath = new File(getInstallRoot(), "lib");
      bootPath = new File(bootPath, "dso-boot");
      bootPath = new File(bootPath, BootJarSignature.getBootJarNameForThisVM());
      m_bootPath = bootPath;
    }

    return m_bootPath.getAbsolutePath();
  }

  protected static String getenv(String key) {
    try {
      Method m = System.class.getMethod("getenv", new Class[] { String.class });

      if (m != null) { return (String) m.invoke(null, new Object[] { key }); }
    } catch (Throwable t) {/**/
    }

    return null;
  }

  static File staticGetJavaCmd() {
    File javaBin = new File(System.getProperty("java.home"), "bin");
    return new File(javaBin, "java" + (Os.isWindows() ? ".exe" : ""));
  }

  File m_javaCmd;

  protected File getJavaCmd() {
    if (m_javaCmd == null) {
      m_javaCmd = staticGetJavaCmd();
    }

    return m_javaCmd;
  }

  class WebServerStartupMonitor extends Thread {
    private final Process         process;
    private final int             port;
    private final StartupListener startupListener;

    WebServerStartupMonitor(Process process, int port, StartupListener startupListener) {
      super();

      this.process = process;
      this.port = port;
      this.startupListener = startupListener;
    }

    @Override
    public void run() {
      while (true) {
        try {
          process.exitValue();
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              startupListener.processFailed();
            }
          });
          return;
        } catch (IllegalThreadStateException itse) {/**/
        }

        try {
          safeCloseSocket(new Socket("localhost", port));
          return;
        } catch (IOException ioe) {/**/
        }

        try {
          sleep(1000);
        } catch (InterruptedException ignore) {/**/
        }
      }
    }
  }

  interface ProcessListener {
    void startupError(Exception e);

    void startupFailed(String errorBuffer);

    void shutdownError(Exception e);

    void shutdownFailed(String errorBuffer);

    void processReady();

    void processTerminated(int exitCode);
  }

  interface StartupListener {
    void startupError(Exception e);

    void processFailed();

    void processReady();
  }

  interface ShutdownListener {
    void processError(Exception e);

    void processFailed(String errorBuffer);

    void processStopped();
  }

  private void stopWebServerAndNotify(final ProcessOutputView outView, final int port,
                                      final ShutdownListener shutdownListener) {
    trace("Stopping " + getSelectedServerLabel() + "-" + port);

    try {
      safeCloseSocket(new Socket("localhost", port));
    } catch (Exception e) {
      shutdownListener.processStopped();
      return;
    }

    Process process;
    try {
      process = stopJetty(port);
      IOUtils.closeQuietly(process.getOutputStream());
    } catch (Exception e) {
      shutdownListener.processError(e);
      return;
    }

    new WebServerShutdownMonitor(process, port, shutdownListener).start();
  }

  class WebServerShutdownMonitor extends Thread {
    private Process                process;
    private final int              port;
    private final ShutdownListener shutdownListener;
    private boolean                stop;

    WebServerShutdownMonitor(int port, ShutdownListener listener) {
      this(null, port, listener);
    }

    WebServerShutdownMonitor(Process process, int port, ShutdownListener shutdownListener) {
      super();

      this.process = process;
      this.port = port;
      this.shutdownListener = shutdownListener;
    }

    @Override
    public void run() {
      ProcessWaiter waiter = null;

      if (process != null) {
        waiter = new ProcessWaiter(process);
        waiter.start();
      }

      while (!stop) {
        if (process != null) {
          try {
            int exitCode = process.exitValue();

            if (exitCode != 0) {
              final String errorBuf = waiter.getErrorBuffer();
              SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  shutdownListener.processFailed(errorBuf);
                }
              });
              return;
            } else {
              process = null;
            }
          } catch (IllegalThreadStateException itse) {/**/
          }
        }

        if (!stop) {
          try {
            safeCloseSocket(new Socket("localhost", port));
          } catch (Exception e) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                shutdownListener.processStopped();
              }
            });
            return;
          }
        }

        try {
          sleep(1000);
        } catch (InterruptedException ignore) {/**/
        }
      }
    }

    void cancel() {
      stop = true;
    }
  }

  // End -- Process control support

  private void stopAll() throws Exception {
    if (webServer1Status.isReady()) {
      stopWebServer1();
    }
    if (webServer2Status.isReady()) {
      stopWebServer2();
    }
    if (l2Status.isReady()) {
      stopL2();
    }
  }

  private String[] append(String[] array1, String[] array2) {
    int array1Len = array1.length;
    int array2Len = array2.length;
    String[] result = new String[array1Len + array2Len];

    for (int i = 0; i < array1Len; i++) {
      result[i] = array1[i];
    }
    for (int i = 0, j = array1Len; i < array2Len; i++, j++) {
      result[j] = array2[i];
    }

    return result;
  }

  private String[] buildScriptCommand(String scriptPath) {
    if (Os.isWindows()) {
      return new String[] { "cmd.exe", "/C", scriptPath };
    } else {
      return new String[] { scriptPath };
    }
  }

  private String[] buildScriptArgs(String[] args) {
    return args;
  }

  private Process invokeScript(String scriptName, String[] args) throws Exception {
    String[] cmd = buildScriptCommand(SANDBOX_ROOT + FS + "bin" + FS + scriptName);
    String[] env = getSelectedServerEnvironment();
    File wd = new File(SANDBOX_ROOT);

    final String[] fullCmd = append(cmd, buildScriptArgs(args));
    trace("Invoking script " + Arrays.asList(fullCmd) + " with working dir " + wd.getAbsolutePath());
    return Runtime.getRuntime().exec(fullCmd, env, wd);
  }

  private void stopSystem() {
    try {
      webAppTreeModel.updateLinks(false, false);
      disableControls();
      stopAll();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean anyReady() {
    return l2Status.isReady() || webServer1Status.isReady() || webServer2Status.isReady();
  }

  private boolean anyRestarting() {
    return l2Status.isRestarting() || webServer1Status.isRestarting() || webServer2Status.isRestarting();
  }

  private boolean anyWaiting() {
    return l2Status.isWaiting() || webServer1Status.isWaiting() || webServer2Status.isWaiting();
  }

  private void disableControls() {
    webServer1EnabledToggle.setEnabled(false);
    webServer2EnabledToggle.setEnabled(false);
    dsoEnabledToggle.setEnabled(false);

    startButton.setEnabled(false);
    stopButton.setEnabled(false);

    webServer1Control.setVisible(false);
    webServer2Control.setVisible(false);

    selectControlTab();
    setConfigTabEnabled(false);
    setMonitorTabEnabled(false);

    importAction.setEnabled(false);
    webAppTreeModel.setRefreshEnabled(false);
    webAppTreeModel.setRemoveEnabled(false);

    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  private synchronized void testEnableControls() {
    boolean anyRestarting = anyRestarting();
    boolean anyWaiting = anyWaiting();

    if (anyRestarting || anyWaiting) { return; }

    restarting = false;

    boolean anyReady = anyReady();

    if (!anyWaiting && !anyRestarting && anyReady && quitting) {
      stopSystem();
    }

    if (!anyWaiting && !anyRestarting && !anyReady) {
      if (quitting) {
        shutdown();
        return;
      } else {
        importAction.setEnabled(true);
        webAppTreeModel.setRefreshEnabled(true);
        webAppTreeModel.setRemoveEnabled(true);
      }
    }

    webServer1EnabledToggle.setEnabled(!anyWaiting && !anyRestarting && !anyReady);
    webServer2EnabledToggle.setEnabled(!anyWaiting && !anyRestarting && !anyReady);
    dsoEnabledToggle.setEnabled(!anyWaiting && !anyRestarting && !anyReady);
    startButton.setEnabled(!anyWaiting && !anyRestarting);
    stopButton.setEnabled(!anyWaiting && !anyRestarting && anyReady);

    if ((!anyWaiting && !anyReady) || anyRestarting) {
      webServer1Control.setVisible(false);
      webServer2Control.setVisible(false);

      startButton.setText(getBundleString("start.all.label"));
    } else {
      testEnableWebServer1Control();
      testEnableWebServer2Control();

      startButton.setText(getBundleString("restart.all.label"));
    }

    if (!anyWaiting && !anyRestarting) {
      updateLinks();
      setConfigTabEnabled(true);
      setMonitorTabEnabled(isDsoEnabled());
      setCursor(STANDARD_CURSOR);
    }
  }

  private void testEnableWebServer1Control() {
    boolean webServer1NotWaiting = !webServer1Status.isWaiting();
    webServer1Control.setVisible(webServer1NotWaiting);
    webServer1Control.setEnabled(webServer1NotWaiting);
    if (webServer1NotWaiting) {
      boolean webServer1Ready = isWebServer1Ready();

      webServer1Control.setIcon(webServer1Ready ? stopIcon : startIcon);

      String tip = (webServer1Ready ? getBundleString("stop.label") : getBundleString("start.label")) + " "
                   + getWebServer1Label();
      webServer1Control.setToolTipText(tip);
    }
  }

  private void testEnableWebServer2Control() {
    boolean webServer2NotWaiting = !webServer2Status.isWaiting();
    webServer2Control.setVisible(webServer2NotWaiting);
    webServer2Control.setEnabled(webServer2NotWaiting);
    if (webServer2NotWaiting) {
      boolean webServer2Ready = isWebServer2Ready();

      webServer2Control.setIcon(webServer2Ready ? stopIcon : startIcon);

      String tip = (webServer2Ready ? getBundleString("stop.label") : getBundleString("start.label")) + " "
                   + getWebServer2Label();
      webServer2Control.setToolTipText(tip);
    }
  }

  private void updateLinks() {
    webAppTreeModel.updateLinks(isWebServer1Ready(), isWebServer2Ready());
  }

  private void saveConfig() {
    xmlPane.save();
  }

  public void modelChanged() {
    updateXmlPane();

    if (false && isL2Ready() && !handlingAppEvent) {
      queryRestart();
    }
  }

  public void saveXML(String xmlText) {
    configHelper.save(xmlText);
    setXmlModified(false);

    if (isConfigTabSelected() && isL2Ready()) {
      askRestart = true;
    }
  }

  private static void trace(String msg) {
    if (debug) {
      System.out.println(msg);
      System.out.flush();
    }
  }

  @Override
  protected Preferences getPreferences() {
    return sessionIntegratorContext.getPrefs().node("SessionIntegratorFrame");
  }

  @Override
  protected void storePreferences() {
    sessionIntegratorContext.storePrefs();
  }

  // Everything belows goes into org.terracotta.ui.session.ui.common.Frame

  private Icon newIcon(String iconPath) {
    return new ImageIcon(getClass().getResource(iconPath));
  }
}
