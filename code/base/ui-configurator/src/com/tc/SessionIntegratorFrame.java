/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlError;
import org.dijon.Button;
import org.dijon.CheckBox;
import org.dijon.Container;
import org.dijon.ContainerResource;
import org.dijon.Dialog;
import org.dijon.DialogResource;
import org.dijon.DictionaryResource;
import org.dijon.EmptyBorder;
import org.dijon.Frame;
import org.dijon.Label;
import org.dijon.Menu;
import org.dijon.MenuBar;
import org.dijon.SplitPane;
import org.dijon.TabbedPane;

import com.tc.admin.ConnectionContext;
import com.tc.admin.ConnectionListener;
import com.tc.admin.ProductInfo;
import com.tc.admin.ServerConnectionManager;
import com.tc.admin.common.BrowserLauncher;
import com.tc.admin.common.ContactTerracottaAction;
import com.tc.admin.common.OutputStreamListener;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XTree;
import com.tc.config.Directories;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.appevent.NonPortableEventContext;
import com.tc.object.appevent.NonPortableFieldSetContext;
import com.tc.object.appevent.NonPortableLogicalInvokeContext;
import com.tc.object.appevent.NonPortableObjectEvent;
import com.tc.servers.ServerSelection;
import com.tc.servers.ServersDialog;
import com.tc.util.NonPortableReason;
import com.tc.util.runtime.Os;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.Preferences;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class SessionIntegratorFrame extends Frame {
  private static final boolean       m_debug                     = Boolean.getBoolean("SessionIntegratorFrame.debug");

  private ConfigHelper               m_configHelper;
  private SplashDialog               m_splashDialog;

  private ServersDialog              m_serversDialog;
  private Properties                 m_properties;
  private SplitPane                  m_controlSplitter;
  private Integer                    m_controlDividerLocation;
  private DividerListener            m_dividerListener;
  private TabbedPane                 m_tabbedPane;
  private int                        m_lastSelectedTabIndex;
  private WebAppTreeModel            m_webAppTreeModel;
  private Button                     m_startButton;
  private Button                     m_stopButton;
  private CheckBox                   m_dsoEnabledToggle;
  private boolean                    m_dsoEnabled;
  private XTree                      m_webAppTree;
  private WebAppLinkNode             m_lastArmedLink;
  private TabbedPane                 m_configTabbedPane;
  private ConfigTextPane             m_xmlPane;
  private XmlChangeListener          m_xmlChangeListener;
  private ConfigProblemTable         m_configProblemTable;
  private ConfigProblemTableModel    m_configProblemTableModel;

  private ProcessOutputView          m_l2OutView;
  private Label                      m_l2Label;
  private ProcessStatus              m_l2Status;
  private boolean                    m_handlingAppEvent;
  private L2StartupListener          m_l2StartupListener;
  private L2ShutdownListener         m_l2ShutdownListener;
  private L2ShutdownMonitor          m_l2Monitor;
  private L2ConnectListener          m_l2ConnectListener;
  private ServerConnectionManager    m_l2ConnectManager;

  private CheckBox                   m_webServer1EnabledToggle;
  private boolean                    m_webServer1Enabled;
  private ProcessOutputView          m_webServer1OutView;
  private Label                      m_webServer1Label;
  private ProcessStatus              m_webServer1Status;
  private Button                     m_webServer1Control;
  private WebServer1StartupListener  m_webServer1StartupListener;
  private WebServer1ShutdownListener m_webServer1ShutdownListener;
  private WebServerShutdownMonitor   m_webServer1Monitor;

  private CheckBox                   m_webServer2EnabledToggle;
  private boolean                    m_webServer2Enabled;
  private ProcessOutputView          m_webServer2OutView;
  private Label                      m_webServer2Label;
  private ProcessStatus              m_webServer2Status;
  private Button                     m_webServer2Control;
  private WebServer2StartupListener  m_webServer2StartupListener;
  private WebServer2ShutdownListener m_webServer2ShutdownListener;
  private WebServerShutdownMonitor   m_webServer2Monitor;

  private ImageIcon                  m_waitingIcon;
  private ImageIcon                  m_readyIcon;
  private ImageIcon                  m_stoppedIcon;

  private Icon                       m_startIcon;
  private Icon                       m_stopIcon;

  private InstrumentedClassesPanel   m_instrumentedClassesPanel;
  private TransientFieldsPanel       m_transientFieldsPanel;
  private BootClassesPanel           m_bootClassesPanel;
  private ModulesPanel               m_modulesPanel;

  private ServersAction              m_serversAction;
  private ImportWebAppAction         m_importAction;
  private HelpAction                 m_helpAction;

  private boolean                    m_askRestart;
  private boolean                    m_restarting;
  private boolean                    m_quitting;

  private ServerSelection            m_serverSelection;

  private static String              SHOW_SPLASH_PREF_KEY        = "ShowSplash";
  private static String              LAST_DIR_PREF_KEY           = "LastDirectory";
  private static String              DSO_ENABLED_PREF_KEY        = "DsoEnabled";
  private static String              WEBSERVER1_ENABLED_PREF_KEY = "WebServer1Enabled";
  private static String              WEBSERVER2_ENABLED_PREF_KEY = "WebServer2Enabled";

  private static final String        BAT_EXTENSION               = ".bat";
  private static final String        SH_EXTENSION                = ".sh";
  private static final String        SCRIPT_EXTENSION            = getScriptExtension();
  private static final String        FS                          = System.getProperty("file.separator");
  private static final String        DEFAULT_TC_INSTALL_DIR      = getDefaultInstallDir();
  private static final String        TC_INSTALL_DIR              = System.getProperty("tc.install.dir",
                                                                                      DEFAULT_TC_INSTALL_DIR);
  private static final String        DEFAULT_SANDBOX_ROOT        = TC_INSTALL_DIR + FS + "tools" + FS + "sessions" + FS
                                                                   + "configurator-sandbox";
  private static final String        SANDBOX_ROOT                = System.getProperty("configurator.sandbox",
                                                                                      DEFAULT_SANDBOX_ROOT);
  private static final String        L2_LABEL                    = "Terracotta Server";
  private static final String        L2_STARTUP_SCRIPT           = "start-tc-server" + SCRIPT_EXTENSION;
  private static final String        L2_SHUTDOWN_SCRIPT          = "stop-tc-server" + SCRIPT_EXTENSION;
  private static final String        L2_STARTUP_TRIGGER          = "Terracotta Server has started up";
  private static final int           SERVER1_PORT                = 9081;
  private static final String        WEBSERVER_STARTUP_SCRIPT    = "start-web-server" + SCRIPT_EXTENSION;
  private static final String        WEBSERVER_SHUTDOWN_SCRIPT   = "stop-web-server" + SCRIPT_EXTENSION;
  private static final int           SERVER2_PORT                = 9082;
  private static final String        HELP_DOC                    = TC_INSTALL_DIR + FS + "docs" + FS
                                                                   + "TerracottaSessionsQuickStart.html";

  private static final Cursor        LINK_CURSOR                 = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
  private static final Cursor        STANDARD_CURSOR             = Cursor.getDefaultCursor();

  private static final int           CONTROL_TAB_INDEX           = 0;
  private static final int           CONFIG_TAB_INDEX            = 1;
  private static final int           MONITOR_TAB_INDEX           = 2;

  private static final int           XML_TAB_INDEX               = 4;
  private static final String        XML_TAB_LABEL               = "tc-config.xml";

  private static final String        QUERY_START_MSG             = "Start the system?";
  private static final String        QUERY_RESTART_MSG           = "Restart the system?";

  private static final String        WAITING_LABEL               = " [Waiting...]";
  private static final String        STARTING_LABEL              = " [Starting...]";
  private static final String        STOPPING_LABEL              = " [Stopping...]";
  private static final String        READY_LABEL                 = " [Ready]";
  private static final String        STOPPED_LABEL               = " [Stopped]";
  private static final String        FAILED_LABEL                = " [Failed]";
  private static final String        DISABLED_LABEL              = " [Disabled]";

  public SessionIntegratorFrame() {
    super(SessionIntegrator.getContext().topRes.getFrame("MainFrame"));

    setTitle(getBundleString("title"));
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    m_serverSelection = ServerSelection.getInstance();
    m_configHelper = new ConfigHelper(m_serverSelection);

    initMenubar();
    loadIcons();

    m_tabbedPane = (TabbedPane) findComponent("MainTabbedPane");
    m_tabbedPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        if (m_lastSelectedTabIndex == CONFIG_TAB_INDEX && isXmlModified()) {
          if (querySaveConfig(JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            m_askRestart = isL2Ready();
          }
        }
        if (m_askRestart) {
          String msg = "Configuration has been modified.\n\n" + QUERY_RESTART_MSG;
          queryRestart(msg);
        }
        m_askRestart = false;
        m_lastSelectedTabIndex = m_tabbedPane.getSelectedIndex();
      }
    });

    m_startButton = (Button) findComponent("StartButton");
    m_startButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        startSystem();
      }
    });
    m_startIcon = m_startButton.getIcon();

    m_stopButton = (Button) findComponent("StopButton");
    m_stopButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        stopSystem();
      }
    });
    m_stopIcon = m_stopButton.getIcon();

    m_dsoEnabledToggle = (CheckBox) findComponent("DSOEnabledToggle");
    m_dsoEnabledToggle.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setDsoEnabled(m_dsoEnabledToggle.isSelected());
      }
    });
    Preferences prefs = getPreferences();
    m_dsoEnabled = prefs.getBoolean(DSO_ENABLED_PREF_KEY, false);
    m_dsoEnabledToggle.setSelected(m_dsoEnabled);

    m_webAppTree = (XTree) findComponent("WebAppTree");
    m_webAppTreeModel = new WebAppTreeModel(this, getWebApps());
    m_webAppTree.setModel(m_webAppTreeModel);
    m_webAppTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    m_webAppTree.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() == 1) {
          TreePath path = m_webAppTree.getPathForLocation(me.getX(), me.getY());

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
    });
    m_webAppTree.addMouseMotionListener(new MouseMotionAdapter() {
      public void mouseMoved(MouseEvent me) {
        TreePath path = m_webAppTree.getPathForLocation(me.getX(), me.getY());

        if (path != null) {
          Object leaf = path.getLastPathComponent();

          if (leaf instanceof WebAppLinkNode) {
            WebAppLinkNode node = (WebAppLinkNode) leaf;
            Cursor c = m_webAppTree.getCursor();

            if (m_lastArmedLink != node) {
              if (m_lastArmedLink != null) {
                m_lastArmedLink.setArmed(false);
              }
              node.setArmed(true);
              m_lastArmedLink = node;
            }

            if (node.isReady() && c != LINK_CURSOR) {
              m_webAppTree.setCursor(LINK_CURSOR);
            }
            return;
          }
        }

        if (m_lastArmedLink != null) {
          m_lastArmedLink.setArmed(false);
          m_lastArmedLink = null;
        }

        m_webAppTree.setCursor(null);
      }
    });

    m_configTabbedPane = (TabbedPane) findComponent("ConfigTabbedPane");
    m_configProblemTable = (ConfigProblemTable) findComponent("ConfigProblemTable");
    m_configProblemTableModel = new ConfigProblemTableModel();
    m_configProblemTable.setModel(m_configProblemTableModel);
    m_configProblemTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() == 2) {
          int row = m_configProblemTable.getSelectedRow();
          XmlError error = m_configProblemTableModel.getError(row);

          m_xmlPane.selectError(error);
        }
      }
    });

    m_xmlPane = (ConfigTextPane) findComponent("XMLPane");
    m_xmlChangeListener = new XmlChangeListener();
    initXmlPane();

    Container configPaneToolbar = (Container) findComponent("ConfigPaneToolbar");
    Button button;
    Insets emptyInsets = new Insets(1,1,1,1);
    button = (Button) configPaneToolbar.findComponent("SaveButton");
    button.setAction(m_xmlPane.getSaveAction());
    button.setText(null);
    button.setMargin(emptyInsets);
    button = (Button) configPaneToolbar.findComponent("UndoButton");
    button.setAction(m_xmlPane.getUndoAction());
    button.setText(null);
    button.setMargin(emptyInsets);
    button = (Button) configPaneToolbar.findComponent("RedoButton");
    button.setAction(m_xmlPane.getRedoAction());
    button.setText(null);
    button.setMargin(emptyInsets);

    button = (Button) configPaneToolbar.findComponent("CutButton");
    button.setAction(m_xmlPane.getCutAction());
    button.setText(null);
    button.setMargin(emptyInsets);
    button = (Button) configPaneToolbar.findComponent("CopyButton");
    button.setAction(m_xmlPane.getCopyAction());
    button.setText(null);
    button.setMargin(emptyInsets);
    button = (Button) configPaneToolbar.findComponent("PasteButton");
    button.setAction(m_xmlPane.getPasteAction());
    button.setText(null);
    button.setMargin(emptyInsets);
    
    m_l2OutView = (ProcessOutputView) findComponent("L2OutView");
    m_l2Label = (Label) findComponent("L2Label");
    m_l2Status = new ProcessStatus("L2");
    m_l2StartupListener = new L2StartupListener();
    m_l2ShutdownListener = new L2ShutdownListener();

    m_webServer1EnabledToggle = (CheckBox) findComponent("Tomcat1EnabledToggle");
    m_webServer1OutView = (ProcessOutputView) findComponent("Tomcat1OutView");
    m_webServer1Label = (Label) findComponent("Tomcat1Label");
    m_webServer1Status = new ProcessStatus(getWebServer1Label());
    m_webServer1Control = (Button) findComponent("Tomcat1Control");
    m_webServer1StartupListener = new WebServer1StartupListener();
    m_webServer1ShutdownListener = new WebServer1ShutdownListener();

    m_webServer1Enabled = prefs.getBoolean(WEBSERVER1_ENABLED_PREF_KEY, true);
    m_webServer1EnabledToggle.setSelected(m_webServer1Enabled);
    m_webServer1EnabledToggle.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setWebServer1Enabled(m_webServer1EnabledToggle.isSelected());
      }
    });
    m_webServer1Label.setText(getWebServer1Label());
    m_webServer1Control.setMargin(new Insets(0, 0, 0, 0));
    m_webServer1Control.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        disableControls();
        m_webAppTreeModel.updateLinks(false, isWebServer2Ready());
        toggleWebServer1();
      }
    });

    m_webServer2EnabledToggle = (CheckBox) findComponent("Tomcat2EnabledToggle");
    m_webServer2OutView = (ProcessOutputView) findComponent("Tomcat2OutView");
    m_webServer2Label = (Label) findComponent("Tomcat2Label");
    m_webServer2Status = new ProcessStatus(getWebServer1Label());
    m_webServer2Control = (Button) findComponent("Tomcat2Control");
    m_webServer2StartupListener = new WebServer2StartupListener();
    m_webServer2ShutdownListener = new WebServer2ShutdownListener();

    m_webServer2Enabled = prefs.getBoolean(WEBSERVER2_ENABLED_PREF_KEY, true);
    m_webServer2EnabledToggle.setSelected(m_webServer2Enabled);
    m_webServer2EnabledToggle.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        setWebServer2Enabled(m_webServer2EnabledToggle.isSelected());
      }
    });
    m_webServer2Label.setText(getWebServer2Label());
    m_webServer2Control.setMargin(new Insets(0, 0, 0, 0));
    m_webServer2Control.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        disableControls();
        m_webAppTreeModel.updateLinks(isWebServer1Ready(), false);
        toggleWebServer2();
      }
    });

    m_instrumentedClassesPanel = (InstrumentedClassesPanel) findComponent("InstrumentedClassesPanel");
    m_transientFieldsPanel = (TransientFieldsPanel) findComponent("TransientFieldsPanel");
    m_bootClassesPanel = (BootClassesPanel) findComponent("BootClassesPanel");
    m_modulesPanel = (ModulesPanel) findComponent("ModulesPanel");

    setupEditorPanels();

    m_startButton.setEnabled(isWebServer1Enabled() || isWebServer2Enabled() || isDsoEnabled());
    m_stopButton.setEnabled(isWebServer1Enabled() || isWebServer2Enabled() || isDsoEnabled());

    if (prefs.getBoolean(SHOW_SPLASH_PREF_KEY, true)) {
      addComponentListener(new ComponentAdapter() {
        public void componentShown(ComponentEvent e) {
          openSplashDialog(this);
        }
      });
    }

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent we) {
        quit();
      }
    });

    m_l2ConnectListener = new L2ConnectListener();
    m_l2ConnectManager = new ServerConnectionManager("localhost", m_configHelper.getJmxPort(), false,
                                                     m_l2ConnectListener);
    testShutdownL2();
    testShutdownWebServer1();
    testShutdownWebServer2();
  }

  static String getBundleString(String key) {
    return SessionIntegrator.getContext().getMessage(key);
  }

  static String formatBundleString(String key, Object[] args) {
    return MessageFormat.format(getBundleString(key), args);
  }

  static String getTCInstallDir() {
    return TC_INSTALL_DIR;
  }

  public static String getSandBoxRoot() {
    return SANDBOX_ROOT;
  }

  ConfigHelper getConfigHelper() {
    return m_configHelper;
  }

  private static String getScriptExtension() {
    return Os.isWindows() ? BAT_EXTENSION : SH_EXTENSION;
  }

  private static String getDefaultInstallDir() {
    try {
      return Directories.getInstallationRoot().getAbsolutePath();
    } catch (Exception e) {
      String msg = e.getMessage();
      String title = getBundleString("title");
      int msgType = JOptionPane.ERROR_MESSAGE;

      JOptionPane.showMessageDialog(null, msg, title, msgType);
      e.printStackTrace();
      System.exit(-1);
    }

    return null;
  }

  private void openSplashDialog(ComponentAdapter splashListener) {
    DictionaryResource topRes = SessionIntegrator.getContext().topRes;
    DialogResource dialogRes = topRes.getDialog("SplashDialog");

    m_splashDialog = new SplashDialog(this, true);

    m_splashDialog.load(dialogRes);
    ((Button) m_splashDialog.findComponent("HelpButton")).addActionListener(m_helpAction);
    ((Button) m_splashDialog.findComponent("ImportButton")).addActionListener(m_importAction);
    ((Button) m_splashDialog.findComponent("SkipButton")).addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        checkShowSplashToggle();
        m_splashDialog.setVisible(false);
      }
    });
    m_splashDialog.center(this);
    m_splashDialog.addWindowListener(new WindowAdapter() {
      public void windowClosed(WindowEvent we) {
        checkShowSplashToggle();
      }
    });
    if (splashListener != null) {
      removeComponentListener(splashListener);
    }
    m_splashDialog.setVisible(true);
  }

  private void checkShowSplashToggle() {
    if (m_splashDialog != null) {
      CheckBox toggle = (CheckBox) m_splashDialog.findComponent("NoShowSplash");
      Preferences prefs = getPreferences();

      prefs.putBoolean(SHOW_SPLASH_PREF_KEY, !toggle.isSelected());
      storePreferences();
    }
  }

  private void loadIcons() {
    URL url;
    String iconsPrefix = "/com/tc/admin/icons/";
    Class clas = getClass();

    if ((url = clas.getResource(iconsPrefix + "progress_task_yellow.gif")) != null) {
      m_waitingIcon = new ImageIcon(url);
    }

    if ((url = clas.getResource(iconsPrefix + "progress_task_green.gif")) != null) {
      m_readyIcon = new ImageIcon(url);
    }

    if ((url = clas.getResource(iconsPrefix + "progress_task_red.gif")) != null) {
      m_stoppedIcon = new ImageIcon(url);
    }
  }

  private void initMenubar() {
    MenuBar menuBar = new MenuBar();
    Menu menu = new Menu(getBundleString("file.menu.label"));

    menu.add(m_serversAction = new ServersAction());

    menu.add(m_importAction = new ImportWebAppAction());
    menu.add(new ExportConfigurationAction());
    menu.add(new QuitAction());
    menuBar.add(menu);

    menu = new Menu(getBundleString("output.menu.label"));
    menu.add(new ClearOutputAction());
    menuBar.add(menu);

    menu = new Menu(getBundleString("help.menu.label"));
    menu.add(m_helpAction = new HelpAction());
    menu.add(new ShowSplashAction());
    menu.addSeparator();
    menu.add(new ContactTerracottaAction(getBundleString("visit.forums.title"), getBundleString("forums.url")));
    menu.add(new ContactTerracottaAction(getBundleString("contact.support.title"), getBundleString("support.url")));
    /*
     * menu.add(new ContactTerracottaAction(getBundleString("contact.field.eng.title"),
     * getBundleString("field.eng.url"))); menu.add(new ContactTerracottaAction(getBundleString("contact.sales.title"),
     * getBundleString("sales.url")));
     */
    menu.addSeparator();
    menu.add(new AboutAction());
    menuBar.add(menu);

    setMenubar(menuBar);
  }

  class ClearOutputAction extends XAbstractAction {
    ClearOutputAction() {
      super(getBundleString("clear.all.action.name"));
      String uri = "/com/tc/admin/icons/clear_co.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent e) {
      m_l2OutView.setText("");
      m_webServer1OutView.setText("");
      m_webServer2OutView.setText("");
    }
  }

  class HelpAction extends XAbstractAction {
    HelpAction() {
      super(getBundleString("help.action.name"));
      String uri = "/com/tc/admin/icons/help.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
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
      if (m_splashDialog != null) {
        m_splashDialog.center(SessionIntegratorFrame.this);
        m_splashDialog.setVisible(true);
      } else {
        openSplashDialog(null);
      }
    }
  }

  class AboutAction extends XAbstractAction {
    Dialog m_aboutDialog;

    AboutAction() {
      super(getBundleString("about.action.label"));
    }

    public void actionPerformed(ActionEvent ae) {
      if (m_aboutDialog == null) {
        SessionIntegratorContext cntx = SessionIntegrator.getContext();

        m_aboutDialog = new Dialog(SessionIntegratorFrame.this, true);
        m_aboutDialog.load((DialogResource) cntx.topRes.child("AboutDialog"));

        ConfiguratorInfoPanel info;
        String title = SessionIntegratorFrame.this.getTitle();
        info = (ConfiguratorInfoPanel) m_aboutDialog.findComponent("ConfiguratorInfoPanel");
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
      m_aboutDialog.center(SessionIntegratorFrame.this);
      m_aboutDialog.setVisible(true);
    }
  }

  private void showHelp() {
    try {
      openPage("file://" + StringUtils.replace(HELP_DOC, FS, "/"));
    } catch (Exception e) {
      m_configHelper.openError(getBundleString("show.help.error"), e);
    }
  }

  class ServersAction extends XAbstractAction {
    ServersAction() {
      super(getBundleString("servers.action.name"));
      String uri = "/com/tc/admin/icons/thread_obj.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
    }

    public void actionPerformed(ActionEvent e) {
      if (m_serversDialog == null) {
        DialogResource dialogRes = SessionIntegrator.getContext().topRes.getDialog("ServersDialog");
        m_serversDialog = new ServersDialog(SessionIntegratorFrame.this);
        m_serversDialog.load(dialogRes);
        m_serversDialog.addAcceptListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            int oldSelectedServerIndex = m_serverSelection.getSelectedServerIndex();
            m_serversDialog.finishEditing();
            m_serversDialog.setVisible(false);
            m_serverSelection.setServers(m_serversDialog.getServers());
            m_serverSelection.setSelectedServerIndex(m_serversDialog.getSelectedServerIndex());

            m_webServer1Label.setText(getWebServer1Label());
            m_webServer2Label.setText(getWebServer2Label());

            // If the selected server changes, rebuild the webapp tree.
            if(oldSelectedServerIndex != m_serverSelection.getSelectedServerIndex()) {
              m_webAppTreeModel = new WebAppTreeModel(SessionIntegratorFrame.this, getWebApps());    
              m_webAppTree.setModel(m_webAppTreeModel);
            }

            // Each time the user changes anything in the ServersDialog, cause JSP's to be recompiled.
            WebApp[] webApps = getWebApps();
            for (int i = 0; i < webApps.length; i++) {
              touch(webApps[i]);
            }

            m_configHelper = new ConfigHelper(m_serverSelection);
            m_l2ConnectManager.setJMXPortNumber(m_configHelper.getJmxPort());
            initXmlPane();
            setupEditorPanels();
          }
        });
      }

      m_serversDialog.setSelection(m_serverSelection);
      m_serversDialog.center(SessionIntegratorFrame.this);
      m_serversDialog.setVisible(true);
      m_serversDialog.toFront();
    }

  }

  class ImportWebAppAction extends XAbstractAction {
    ImportWebAppAction() {
      super(getBundleString("import.webapp.action.name"));
      String uri = "/com/tc/admin/icons/import_wiz.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
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
    JFileChooser chooser = new JFileChooser();
    File currentDir = getLastDirectory();

    chooser.setMultiSelectionEnabled(false);
    if (currentDir != null) {
      chooser.setCurrentDirectory(currentDir);
    }

    int returnVal = chooser.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      m_configHelper.saveAs(file, m_xmlPane.getText());
    }
  }

  class ExportConfigurationAction extends XAbstractAction {
    ExportConfigurationAction() {
      super(getBundleString("export.configuration.action.name"));
      String uri = "/com/tc/admin/icons/export_wiz.gif";
      setSmallIcon(new ImageIcon(getClass().getResource(uri)));
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
    if (m_quitting) { return; }

    if (isXmlModified()) {
      if (querySaveConfig() == JOptionPane.CANCEL_OPTION) { return; }
    }

    if (anyWaiting()) {
      m_quitting = true;
      showQuittingDialog();
    } else if (anyReady()) {
      m_quitting = true;
      showQuittingDialog();

      try {
        m_webAppTreeModel.updateLinks(false, false);
        disableControls();
        stopAll();
      } catch (Exception e) {
        SessionIntegrator.getContext().client.shutdown();
      }
    } else {
      SessionIntegrator.getContext().client.shutdown();
    }
  }

  void showQuittingDialog() {
    Dialog dialog = new Dialog(this, getTitle());
    Label label = new Label(getBundleString("quitting.dialog.msg"));

    label.setBorder(new EmptyBorder(10, 20, 10, 20));
    dialog.getContentPane().add(label);
    dialog.pack();
    dialog.center(this);
    dialog.setVisible(true);
  }

  class XmlChangeListener extends DocumentAdapter {
    public void insertUpdate(DocumentEvent e) {
      setXmlModified(true);
    }

    public void removeUpdate(DocumentEvent e) {
      setXmlModified(true);
    }
  }

  private void setXmlModified(boolean xmlModified) {
    m_xmlModified = xmlModified;

    if (m_configTabbedPane != null) {
      String label = XML_TAB_LABEL + (xmlModified ? "*" : "");
      m_configTabbedPane.setTitleAt(XML_TAB_INDEX, label);
    }
  }

  private boolean m_xmlModified;

  private boolean isXmlModified() {
    return m_xmlModified;
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
    JFileChooser chooser = new JFileChooser();
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

  private com.tc.servers.ServerInfo getSelectedServer() {
    return m_serverSelection.getSelectedServer();
  }

  private String getSelectedServerName() {
    return getSelectedServer().getName();
  }

  private String getSelectedServerLabel() {
    return getSelectedServer().getLabel();
  }

  private String getSelectedServerStartupTrigger() {
    return getSelectedServer().getStartupTrigger();
  }

  private String getSelectedServerApplicationPath() {
    return getSelectedServer().getApplicationPath();
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
      Map env = new HashMap();
      Properties serverEnv = getSelectedServer().toProperties();

      env.putAll(sysEnv);
      env.putAll(serverEnv);

      ArrayList list = new ArrayList();
      Iterator iter = env.keySet().iterator();
      String key;
      String val;

      while (iter.hasNext()) {
        key = (String) iter.next();
        val = (String) env.get(key);

        list.add(key + "=" + val);
      }

      try {
        File tmpfile = File.createTempFile("terracotta", null);
        list.add("TMPFILE=" + tmpfile.getAbsolutePath());
        tmpfile.delete();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }

      return (String[]) list.toArray(new String[env.size()]);
    }

    return getSelectedServer().toEnvironment();
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

      if (m_splashDialog != null) {
        m_splashDialog.setVisible(false);
        m_splashDialog = null;
      }
    } catch (Exception e) {
      String msg = formatBundleString("install.webapp.failure.msg", new Object[] { file });
      m_configHelper.openError(msg, e);
    }
  }

  private void installWebAppFile(File file) throws Exception {
    String webServer1Area = getWebServer1Area();
    String webServer2Area = getWebServer2Area();

    if (file.isFile()) {
      copyFileToDirectory(file, new File(webServer1Area), false);
      copyFileToDirectory(file, new File(webServer2Area), false);
    } else if (file.isDirectory()) {
      copyDirectory(file, new File(webServer1Area));
      copyDirectory(file, new File(webServer2Area));
    }
  }

  private static void copyFileToDirectory(File file, File dir, boolean keepDate) throws IOException {
    if (dir.exists() && !dir.isDirectory()) {
      throw new IllegalArgumentException(getBundleString("destination.not.directory.msg"));
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
        m_configHelper.openError(msg, e);
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
        m_configHelper.openError(msg, e);
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
    m_properties.setProperty(name, path);

    File webAppProps = m_serverSelection.getSelectedServerWebAppProperties();
    FileOutputStream out = new FileOutputStream(webAppProps);
    Exception err = null;

    try {
      m_properties.store(out, null);

      WebAppNode webAppNode = m_webAppTreeModel.add(new WebApp(name, path));
      TreePath webAppPath = new TreePath(webAppNode.getPath());

      m_webAppTree.expandPath(webAppPath);
      m_webAppTree.setSelectionPath(webAppPath);

      if (m_configHelper.ensureWebApplication(name)) {
        m_configHelper.save();
        initXmlPane();
      }
    } catch (Exception e) {
      err = e;
    } finally {
      IOUtils.closeQuietly(out);
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

    m_properties.remove(name);

    File webAppProps = m_serverSelection.getSelectedServerWebAppProperties();
    FileOutputStream out = new FileOutputStream(webAppProps);
    Exception err = null;

    try {
      m_properties.store(out, null);
      m_webAppTreeModel.remove(name);
      if (m_configHelper.removeWebApplication(name)) {
        m_configHelper.save();
        initXmlPane();
      }
    } catch (Exception e) {
      err = e;
    } finally {
      IOUtils.closeQuietly(out);
    }

    if (err != null) { throw err; }
  }

  private boolean isDsoEnabled() {
    return m_dsoEnabled;
  }

  private void setDsoEnabled(boolean enabled) {
    getPreferences().putBoolean(DSO_ENABLED_PREF_KEY, m_dsoEnabled = enabled);
    storePreferences();

    m_l2Label.setIcon(null);
    m_l2Label.setText(L2_LABEL + (enabled ? "" : DISABLED_LABEL));
    m_l2Label.setEnabled(m_dsoEnabled);
    m_l2OutView.setEnabled(m_dsoEnabled);

    setMonitorTabEnabled(enabled);

    m_startButton.setEnabled(enabled || isWebServer1Enabled() || isWebServer2Enabled());

    if (isL2Ready()) {
      queryRestart();
    }
  }

  private void selectControlTab() {
    m_tabbedPane.setSelectedIndex(CONTROL_TAB_INDEX);
  }

  private boolean isConfigTabSelected() {
    return (m_tabbedPane.getSelectedIndex() == CONFIG_TAB_INDEX);
  }

  private void setConfigTabEnabled(boolean enabled) {
    m_tabbedPane.setEnabledAt(CONFIG_TAB_INDEX, enabled);
  }

  private void setConfigTabForeground(Color fg) {
    m_tabbedPane.setForegroundAt(CONFIG_TAB_INDEX, fg);
  }

  private void setMonitorTabEnabled(boolean enabled) {
    m_tabbedPane.setEnabledAt(MONITOR_TAB_INDEX, enabled);
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
      m_startButton.doClick();
    }

    return answer;
  }

  private WebApp[] getWebApps() {
    File webAppProps = m_serverSelection.getSelectedServerWebAppProperties();

    m_properties = new Properties();

    try {
      m_properties.load(new FileInputStream(webAppProps));
    } catch (IOException ioe) {
      m_properties.setProperty("Cart", "");
      m_properties.setProperty("DepartmentTaskList", "");
      m_properties.setProperty("Townsend", "");
    }

    Enumeration names = m_properties.keys();
    ArrayList appList = new ArrayList();
    String name;

    while (names.hasMoreElements()) {
      name = (String) names.nextElement();
      appList.add(new WebApp(name, m_properties.getProperty(name)));
    }

    return WebAppComparable.sort((WebApp[]) appList.toArray(new WebApp[0]));
  }

  private void setupEditorPanels() {
    try {
      TcConfig config = m_configHelper.getConfig();

      if (config == null) {
        config = m_configHelper.ensureConfig();
        m_configHelper.save();
        setXmlModified(false);
      }

      DsoApplication dsoApp = config.getApplication().getDso();

      m_instrumentedClassesPanel.setup(dsoApp);
      m_transientFieldsPanel.setup(dsoApp);
      m_bootClassesPanel.setup(dsoApp);
      m_modulesPanel.setup(config.getClients());
    } catch (Exception e) {
      m_configHelper.openError(getBundleString("configuration.load.failure.msg"), e);
    }
  }

  private void initXmlPane() {
    Document xmlDocument = m_xmlPane.getDocument();

    xmlDocument.removeDocumentListener(m_xmlChangeListener);
    m_xmlPane.load(m_configHelper.getConfigFilePath());
    xmlDocument.addDocumentListener(m_xmlChangeListener);
    setXmlModified(false);
  }

  private void updateXmlPane() {
    Document xmlDocument = m_xmlPane.getDocument();

    xmlDocument.removeDocumentListener(m_xmlChangeListener);
    m_xmlPane.set(m_configHelper.getConfigText());
    xmlDocument.addDocumentListener(m_xmlChangeListener);
    setXmlModified(true);
  }

  void setConfigErrors(List errorList) {
    m_configProblemTableModel.setErrors(errorList);
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

  private void startSystem() {
    try {
      if (isXmlModified()) {
        if (querySaveConfig() == JOptionPane.CANCEL_OPTION) { return; }
      }
      m_webAppTreeModel.updateLinks(false, false);
      disableControls();
      startAll();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void startAll() throws Exception {
    m_restarting = anyReady();
    startServers();
  }

  private void startServers() {
    trace("startServers");

    m_l2Label.setIcon(m_l2Status.isReady() ? m_waitingIcon : null);

    if (m_restarting) {
      if (isL2Ready()) {
        m_l2Label.setIcon(m_waitingIcon);
        m_l2Label.setText(L2_LABEL + WAITING_LABEL);

        if (m_webServer1Status.isReady() || m_webServer2Status.isReady()) {
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
          m_webServer1Label.setIcon(m_waitingIcon);
          m_webServer1Label.setText(getWebServer1Label() + WAITING_LABEL);
        }
        if (isWebServer2Enabled()) {
          m_webServer2Label.setIcon(m_waitingIcon);
          m_webServer2Label.setText(getWebServer2Label() + WAITING_LABEL);
        }
        startL2();
      } else {
        startWebServers();
      }
    }
  }

  private boolean isWebServer1Enabled() {
    return m_webServer1Enabled;
  }

  private void setWebServer1Enabled(boolean enabled) {
    getPreferences().putBoolean(WEBSERVER1_ENABLED_PREF_KEY, m_webServer1Enabled = enabled);
    storePreferences();

    m_startButton.setEnabled(enabled || isWebServer2Enabled() || isDsoEnabled());
  }

  private boolean isWebServer2Enabled() {
    return m_webServer2Enabled;
  }

  private void setWebServer2Enabled(boolean enabled) {
    getPreferences().putBoolean(WEBSERVER2_ENABLED_PREF_KEY, m_webServer2Enabled = enabled);
    storePreferences();

    m_startButton.setEnabled(enabled || isWebServer1Enabled() || isDsoEnabled());
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
      if (m_l2ConnectManager.testIsConnected()) { return true; }
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
    if(System.getProperty("tc.servers") != null) {
      m_l2OutView.append("Using external Terracotta servers: "+System.getProperty("tc.servers"));
      startWebServers();
      return;
    }
    
    trace("startL2");

    if (m_l2Monitor != null) {
      m_l2Monitor.cancel();
      while (true) {
        try {
          m_l2Monitor.join(0);
          break;
        } catch (InterruptedException ie) {/**/
        }
      }
      m_l2Monitor = null;
    }

    if (isL2Ready()) {
      m_l2Status.setRestarting(true);
      restartL2();
      return;
    }

    if (isDsoEnabled()) {
      _startL2();
    }
  }

  private void _startL2() {
    trace("_startL2");

    m_l2Status.setWaiting();
    m_l2Label.setIcon(m_waitingIcon);
    m_l2Label.setText(L2_LABEL + STARTING_LABEL);
    m_l2OutView.setListener(m_l2StartupListener);
    m_l2OutView.setListenerTrigger(L2_STARTUP_TRIGGER);
    startL2AndNotify(L2_STARTUP_SCRIPT, m_l2OutView, m_l2StartupListener);
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
          if (m_debug) {
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

    m_l2OutView.start(process);

    new L2StartupMonitor(process, startupListener).start();
  }

  class L2StartupMonitor extends Thread {
    private Process         m_process;
    private StartupListener m_startupListener;

    L2StartupMonitor(Process process, StartupListener listener) {
      super();

      m_process = process;
      m_startupListener = listener;
    }

    public void run() {
      while (true) {
        if (m_process != null) {
          try {
            int exitCode = m_process.exitValue();

            if (exitCode != 0) {
              SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  m_startupListener.processFailed();
                }
              });
              return;
            } else {
              m_process = null;
            }
          } catch (IllegalThreadStateException itse) {/**/
          }

          if (isL2Accessible()) { return; }

          try {
            sleep(1000);
          } catch (InterruptedException ignore) {/**/
          }
        }
      }
    }
  }

  class L2StartupListener implements StartupListener, OutputStreamListener {
    public void processFailed() {
      trace("L2.processFailed");

      if (m_webServer1Status.isReady()) {
        stopWebServer1();
      } else {
        m_webServer1Label.setIcon(null);
        m_webServer1Label.setText(getWebServer1Label());
      }

      if (m_webServer2Status.isReady()) {
        stopWebServer2();
      } else {
        m_webServer2Label.setIcon(null);
        m_webServer2Label.setText(getWebServer2Label());
      }

      m_l2Label.setIcon(m_stoppedIcon);
      m_l2Label.setText(L2_LABEL + FAILED_LABEL);
      m_l2Status.setFailed();

      testEnableControls();
    }

    public void startupError(Exception e) {
      trace("L2.startupError exception=" + e.getMessage());
      if (m_debug) e.printStackTrace();

      m_webServer1Label.setIcon(null);
      m_webServer1Label.setText(getWebServer1Label());
      m_webServer2Label.setIcon(null);
      m_webServer2Label.setText(getWebServer2Label());

      m_l2Label.setIcon(m_stoppedIcon);
      m_l2Label.setText(L2_LABEL + FAILED_LABEL);
      m_l2Status.setFailed();

      testEnableControls();
    }

    public void triggerEncountered() {
      m_l2OutView.setListener(null);
      processReady();
    }

    public void processReady() {
      trace("L2.processReady");

      m_l2Label.setIcon(m_readyIcon);
      m_l2Label.setText(L2_LABEL + READY_LABEL);
      m_l2Status.setReady();

      startWebServers();
      waitForMBean();

      m_l2Monitor = new L2ShutdownMonitor(m_l2ShutdownListener);
      m_l2Monitor.start();

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
    if(System.getProperty("tc.servers") != null) {
      if (m_webServer1Status.isReady() || m_webServer2Status.isReady()) {
        stopWebServers();
      }
      return;
    }

    if (m_l2Monitor != null) {
      m_l2Monitor.cancel();
      while (true) {
        try {
          m_l2Monitor.join(0);
          break;
        } catch (InterruptedException ie) {/**/
        }
      }
      m_l2Monitor = null;
    }

    m_l2Status.setWaiting();
    m_l2Label.setIcon(m_waitingIcon);
    m_l2Label.setText(L2_LABEL + STOPPING_LABEL);
    m_l2ShutdownListener.setRestart(restart);

    stopL2AndNotify(L2_SHUTDOWN_SCRIPT, m_l2OutView, m_configHelper.getJmxPort(), m_l2ShutdownListener);
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

    m_l2Monitor = new L2ShutdownMonitor(process, shutdownListener);
    m_l2Monitor.start();
  }

  class L2ShutdownListener implements ShutdownListener {
    boolean m_restart = false;

    void setRestart(boolean restart) {
      m_restart = restart;
    }

    public void processError(Exception e) {
      trace("L2.processError");
      if (m_debug) e.printStackTrace();

      if (m_quitting) {
        m_l2Label.setIcon(m_readyIcon);
        m_l2Label.setText(L2_LABEL + READY_LABEL);
        m_l2Status.setReady();
      } else {
        m_l2Label.setIcon(m_stoppedIcon);
        m_l2Label.setText(L2_LABEL + FAILED_LABEL);
        m_l2Status.setFailed();
      }

      testEnableControls();
    }

    public void processFailed(String errorBuf) {
      trace("L2.processFailed");

      m_l2OutView.append(errorBuf);

      if (m_quitting) {
        m_l2Label.setIcon(m_readyIcon);
        m_l2Label.setText(L2_LABEL + READY_LABEL);
        m_l2Status.setReady();
      } else {
        m_l2Label.setIcon(m_stoppedIcon);
        m_l2Label.setText(L2_LABEL + FAILED_LABEL);
        m_l2Status.setFailed();
      }

      testEnableControls();
    }

    public void processStopped() {
      m_l2Monitor = null;
      m_l2ConnectManager.getConnectionContext().reset();
      m_l2Status.setInactive();

      if (m_restart) {
        startL2();
        m_restart = false;
      } else {
        if (m_webServer1Status.isReady() || m_webServer2Status.isReady()) {
          stopWebServers();
        }

        m_l2Label.setIcon(m_stoppedIcon);
        m_l2Label.setText(L2_LABEL + STOPPED_LABEL);
      }

      testEnableControls();
    }
  }

  class L2ShutdownMonitor extends Thread {
    private Process          m_process;
    private ShutdownListener m_shutdownListener;
    private boolean          m_stop;

    L2ShutdownMonitor(ShutdownListener listener) {
      this(null, listener);
    }

    L2ShutdownMonitor(Process process, ShutdownListener listener) {
      super();

      m_process = process;
      m_shutdownListener = listener;
    }

    public void run() {
      ProcessWaiter waiter = null;

      if (m_process != null) {
        waiter = new ProcessWaiter(m_process);
        waiter.start();
      }

      while (!m_stop) {
        if (m_process != null) {
          try {
            int exitCode = m_process.exitValue();

            if (exitCode != 0) {
              final String errorBuf = waiter.getErrorBuffer();
              SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  m_shutdownListener.processFailed(errorBuf);
                }
              });
              return;
            } else {
              m_process = null;
            }
          } catch (IllegalThreadStateException itse) {/**/
          }
        }

        if (!m_stop) {
          if (!isL2Accessible()) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                m_shutdownListener.processStopped();
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
      m_stop = true;
    }
  }

  class DSOAppEventListener implements NotificationListener {
    public void handleNotification(Notification notification, Object handback) {
      final Object event = notification.getSource();

      if (event instanceof NonPortableObjectEvent) {
        m_handlingAppEvent = true;

        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            SessionIntegratorFrame.this.toFront();
            handleNonPortableReason((NonPortableObjectEvent) event);
          }
        });
      }
    }
  }

  private void handleNonPortableReason(NonPortableObjectEvent event) {
    ContainerResource res = (ContainerResource)SessionIntegrator.getContext().topRes.getComponent("NonPortableObjectPanel");
    NonPortableObjectPanel panel = new NonPortableObjectPanel(res, this);
    Dialog dialog = new Dialog(this, this.getTitle(), true);
    Container cp = (Container)dialog.getContentPane();
    cp.setLayout(new BorderLayout());
    cp.add(panel);
    panel.setEvent(event);
    dialog.pack();
    dialog.center(this);
    dialog.setVisible(true);
    m_handlingAppEvent = false;
    return;
    
    /*
    NonPortableReason reason = event.getReason();

    switch (reason.getReason()) {
      case NonPortableReason.CLASS_NOT_IN_BOOT_JAR: {
        handleClassNotInBootJar(event);
        break;
      }
      case NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG: {
        handleClassNotIncludedInConfig(event);
        break;
      }
      case NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED: {
        handleSuperClassNotInstrumented(event);
        break;
      }
      case NonPortableReason.SUBCLASS_OF_LOGICALLY_MANAGED_CLASS: {
        handleSubclassOfLocicallyManagedClass(event);
        break;
      }
      case NonPortableReason.CLASS_NOT_ADAPTABLE: {
        handleClassNotAdaptable(event);
        break;
      }
      default: {
        handleNonPortable(event);
        break;
      }
    }
    
    m_handlingAppEvent = false;
   */
  }

  private void handleClassNotAdaptable(NonPortableObjectEvent event) {
    NonPortableReason reason = event.getReason();
    NonPortableEventContext cntx = event.getContext();
    String msg = reason.getMessage();
    String targetClass = cntx.getTargetClassName();
    String className = reason.getClassName();

    msg = "<html><p>Attempted to share an instance of type <b>" + targetClass + "</b>,<br>"
          + "containing an instance of type <b>" + className + "</b><br>" + "which inherently cannot be shared in DSO.";

    String fieldName = null;

    if (reason.hasUltimateNonPortableFieldName()) {
      fieldName = reason.getUltimateNonPortableFieldName();
    } else if (cntx instanceof NonPortableFieldSetContext) {
      fieldName = ((NonPortableFieldSetContext) cntx).getFieldName();
    }

    if (fieldName != null) {
      msg += "<br><br>Update the configuration to add <b>" + fieldName + "</b><br>"
             + "as a transient field and restart the system?";

      int answer = showConfirmDialog(msg, JOptionPane.YES_NO_OPTION);
      if (answer == JOptionPane.YES_OPTION) {
        m_transientFieldsPanel.ensureTransient(fieldName);
        m_configHelper.save();
        setXmlModified(false);
        m_startButton.doClick();
      }
    } else {
      TreeModel treeModel = event.getContext().getTreeModel();
      showPlainMessage(new JScrollPane(new JTree(treeModel)));
    }
  }

  // TODO: handle multiple logically-managed parent types.

  private void handleSubclassOfLocicallyManagedClass(NonPortableObjectEvent event) {
    NonPortableReason reason = event.getReason();
    NonPortableEventContext cntx = event.getContext();
    String msg = reason.getMessage();
    String targetClass = cntx.getTargetClassName();
    String className = reason.getClassName();
    List bootClasses = reason.getErroneousBootJarSuperClasses();
    List nonBootClasses = reason.getErroneousSuperClasses();
    String logicalType = bootClasses.size() > 0 ? (String) bootClasses.get(0) : (String) nonBootClasses.get(0);

    if (cntx instanceof NonPortableLogicalInvokeContext) {
      msg = "<html><p>Attempted to share an instance of type <b>" + className + "</b><br>"
            + "that extends the logically-managed type <b>" + targetClass + "</b>.<br><br>"
            + "Subclasses of logically-managed types cannot be shared in DSO.";
    } else {
      msg = "<html><p>Attempted to share an instance of type <b>" + targetClass + "</b>,<br>"
            + "containing an instance of type <b>" + className + "</b><br>"
            + "that extends the logically-managed type <b>" + logicalType + "</b>.<br><br>"
            + "Subclasses of logically-managed types cannot be shared in DSO.";
    }

    if (reason.hasUltimateNonPortableFieldName()) {
      String fieldName = reason.getUltimateNonPortableFieldName();

      msg += "<br><br>Update the configuration to add <b>" + fieldName + "</b><br>"
             + "as a transient field and restart the system?";

      int answer = showConfirmDialog(msg, JOptionPane.YES_NO_OPTION);
      if (answer == JOptionPane.YES_OPTION) {
        m_transientFieldsPanel.ensureTransient(fieldName);
        m_configHelper.save();
        setXmlModified(false);
        m_startButton.doClick();
      }
    } else {
      showPlainMessage(msg);
    }
  }

  private void handleNonPortable(NonPortableObjectEvent event) {
    NonPortableReason reason = event.getReason();
    NonPortableEventContext cntx = event.getContext();
    String msg = reason.getMessage();
    String className = reason.getClassName();

    if (cntx instanceof NonPortableFieldSetContext) {
      NonPortableFieldSetContext npfsc = (NonPortableFieldSetContext) cntx;
      String targetClass = cntx.getTargetClassName();
      String fieldName = npfsc.getFieldName();

      msg = "<html><p>Attempted to share an instance of type <b>" + targetClass + "</b>,<br>"
            + "containing an instance of type <b>" + className + "</b><br>"
            + "which inherently cannot be shared in DSO.<br><br>" + "Update the configuration to add <b>" + fieldName
            + "</b><br>" + "as a transient field and restart the system?";

      int answer = showConfirmDialog(msg, JOptionPane.YES_NO_OPTION);
      if (answer == JOptionPane.YES_OPTION) {
        m_transientFieldsPanel.ensureTransient(fieldName);
        m_configHelper.save();
        setXmlModified(false);
        m_startButton.doClick();
      }
    } else if (cntx instanceof NonPortableLogicalInvokeContext) {
      List bootClasses = reason.getErroneousBootJarSuperClasses();
      List nonBootClasses = reason.getErroneousSuperClasses();
      String nonPortableType = bootClasses.size() > 0 ? (String) bootClasses.get(0) : (String) nonBootClasses.get(0);

      msg = "<html><p>Attempted to share an instance of type <b>" + className + "</b>,<br>"
            + "containing an instance of type <b>" + nonPortableType + "</b><br>"
            + "which is inherently not sharable in DSO.";

      if (reason.hasUltimateNonPortableFieldName()) {
        String fieldName = reason.getUltimateNonPortableFieldName();

        msg += "<br><br>Update the configuration to add <b>" + fieldName + "</b><br>"
               + "as a transient field and restart the system?";

        int answer = showConfirmDialog(msg, JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {

          m_transientFieldsPanel.ensureTransient(fieldName);
          m_configHelper.save();
          setXmlModified(false);
          m_startButton.doClick();
        }
      } else {
        showPlainMessage(msg);
      }
    } else {
      showPlainMessage(msg);
    }
  }

  private String getClassNotInBootJarMessage(NonPortableObjectEvent event) {
    NonPortableReason reason = event.getReason();
    NonPortableEventContext cntx = event.getContext();
    String targetClass = cntx.getTargetClassName();
    String className = reason.getClassName();
    List bootTypes = reason.getErroneousBootJarSuperClasses();

    if (cntx instanceof NonPortableLogicalInvokeContext) {
      if (bootTypes.size() > 0) {
        return "<html><p>An instance of type <b>" + className + "</b> cannot be shared until<br>"
               + "it, and some of its parent types, are added to the DSO boot jar.</html>";
      } else {
        return "<html><p>An instance of type <b>" + className + "</b> cannot be shared until<br>"
               + "it is added to the DSO boot jar.<br><br>" + "Update the configuration to add <b>" + className
               + "</b> to the<br>" + "DSO boot-jar and restart the system?";
      }
    } else {
      if (bootTypes.size() > 0) {
        return "<html><p>Cannot share an instance of type <b>" + targetClass + "</b><br>"
               + "because it contains an instance of type <b>" + className + "</b>,<br>"
               + "and some parent types, that must be added to the DSO boot-jar.</html>";
      } else {
        return "<html><p>Cannot share an instance of type <b>" + targetClass + "</b><br>"
               + "because it contains an instance of type <b>" + className + "</b><br>"
               + "that must be added to the DSO boot-jar.<br><br>" + "Update the configuration to add <b>" + className
               + "</b> to the<br>" + "DSO boot-jar and restart the system?";
      }
    }
  }

  private void handleClassNotInBootJar(NonPortableObjectEvent event) {
    NonPortableReason reason = event.getReason();
    String className = reason.getClassName();
    List bootTypes = reason.getErroneousBootJarSuperClasses();
    List superTypes = reason.getErroneousSuperClasses();
    String msg = getClassNotInBootJarMessage(event);
    int type = JOptionPane.YES_NO_OPTION;

    if (bootTypes.size() > 0) {
      bootTypes.add(0, className);

      InstrumentSuperTypesPanel panel = getInstrumentSuperTypesPanel(msg, className, bootTypes, superTypes);

      if (showConfirmDialog(panel, type) == JOptionPane.OK_OPTION) {
        for (int i = 0; i < bootTypes.size(); i++) {
          m_bootClassesPanel.ensureBootClass((String) bootTypes.get(i));
        }

        m_configHelper.save();
        setXmlModified(false);

        if (panel.restartSystem()) {
          m_startButton.doClick();
        }
      }
    } else {
      if (showConfirmDialog(msg, type) == JOptionPane.YES_OPTION) {
        m_bootClassesPanel.ensureBootClass(className);
        m_configHelper.save();
        setXmlModified(false);
        m_startButton.doClick();
      }
    }
  }

  private InstrumentTypePanel m_instrumentPanel;

  private InstrumentTypePanel getInstrumentPanel(String msg, String className, List bootTypes, List superTypes) {
    if (m_instrumentPanel == null) {
      DictionaryResource topRes = SessionIntegrator.getContext().topRes;
      ContainerResource res = (ContainerResource) topRes.getComponent("InstrumentPanel");

      m_instrumentPanel = new InstrumentTypePanel(res);
    }
    m_instrumentPanel.setup(msg, className, bootTypes, superTypes);

    return m_instrumentPanel;
  }

  private void handleClassNotIncludedInConfig(NonPortableObjectEvent event) {
    NonPortableReason reason = event.getReason();
    List bootTypes = reason.getErroneousBootJarSuperClasses();
    List superTypes = reason.getErroneousSuperClasses();

    String className = reason.getClassName();
    String msg = "<html><p>Cannot share an instance of type <b>" + className + "</b>,<br> "
                 + "because it is not instrumented for use with DSO.";
    int type = JOptionPane.OK_CANCEL_OPTION;
    InstrumentTypePanel panel = getInstrumentPanel(msg, className, bootTypes, superTypes);
    JScrollPane scrollPane = new JScrollPane(new JTree(event.getContext().getTreeModel()));
    int answer = showConfirmDialog(scrollPane, type);

    if (answer == JOptionPane.OK_OPTION) {
      m_instrumentedClassesPanel.ensureAdaptable(panel.getPattern());

      for (int i = 0; i < bootTypes.size(); i++) {
        m_bootClassesPanel.ensureBootClass((String) bootTypes.get(i));
      }

      if (!panel.includeAll()) {
        for (int i = 0; i < superTypes.size(); i++) {
          m_instrumentedClassesPanel.ensureAdaptable((String) superTypes.get(i));
        }
      }

      m_configHelper.save();
      setXmlModified(false);

      if (panel.restartSystem()) {
        m_startButton.doClick();
      }
    }
  }

  private InstrumentSuperTypesPanel m_instrumentSuperTypesPanel;

  private InstrumentSuperTypesPanel getInstrumentSuperTypesPanel(String msg, String className, List bootTypes,
                                                                 List superTypes) {
    if (m_instrumentSuperTypesPanel == null) {
      DictionaryResource topRes = SessionIntegrator.getContext().topRes;
      ContainerResource res = (ContainerResource) topRes.getComponent("InstrumentSuperTypesPanel");

      m_instrumentSuperTypesPanel = new InstrumentSuperTypesPanel(res);
    }
    m_instrumentSuperTypesPanel.setup(msg, className, bootTypes, superTypes);

    return m_instrumentSuperTypesPanel;
  }

  private void handleSuperClassNotInstrumented(NonPortableObjectEvent event) {
    NonPortableReason reason = event.getReason();
    String className = reason.getClassName();
    String msg = "<html><p>An instance of type <b>" + className + "</b> cannot be shared because<br>"
                 + "some of its base classes are not instrumented for use with DSO.</html>";
    int type = JOptionPane.OK_CANCEL_OPTION;
    List bootTypes = reason.getErroneousBootJarSuperClasses();
    List superTypes = reason.getErroneousSuperClasses();
    InstrumentSuperTypesPanel panel = getInstrumentSuperTypesPanel(msg, className, bootTypes, superTypes);
    int answer = showConfirmDialog(panel, type);

    if (answer == JOptionPane.OK_OPTION) {
      for (int i = 0; i < bootTypes.size(); i++) {
        m_bootClassesPanel.ensureBootClass((String) bootTypes.get(i));
      }

      for (int i = 0; i < superTypes.size(); i++) {
        m_instrumentedClassesPanel.ensureAdaptable((String) superTypes.get(i));
      }

      m_configHelper.save();
      setXmlModified(false);

      if (panel.restartSystem()) {
        m_startButton.doClick();
      }
    }
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
      public void run() {
        while (true) {
          try {
            if (m_l2ConnectManager.testIsConnected()) {
              ConnectionContext cc = m_l2ConnectManager.getConnectionContext();
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
    return m_l2Status.isReady();
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
    if (m_webServer1Monitor != null) {
      m_webServer1Monitor.cancel();
      m_webServer1Monitor = null;
    }

    if (isWebServer1Ready()) {
      m_webServer1Status.setRestarting(true);
      restartWebServer1();
      return;
    }

    m_webServer1Label.setIcon(m_waitingIcon);
    m_webServer1Label.setText(getWebServer1Label() + STARTING_LABEL);
    m_webServer1Status.setWaiting();
    m_webServer1OutView.setListener(m_webServer1StartupListener);
    m_webServer1OutView.setListenerTrigger(getSelectedServerStartupTrigger());
    startWebServerAndNotify(m_webServer1OutView, SERVER1_PORT, m_webServer1StartupListener);
  }

  class WebServer1StartupListener implements StartupListener, OutputStreamListener {
    public void startupError(Exception e) {
      trace(getSelectedServerLabel() + "1.startupError exception=" + e.getMessage());

      m_webServer1Label.setIcon(m_stoppedIcon);
      m_webServer1Label.setText(getWebServer1Label() + FAILED_LABEL);
      m_webServer1Status.setFailed();

      testEnableControls();
    }

    public void processFailed() {
      trace(getSelectedServerLabel() + ".processFailed");

      m_webServer1Label.setIcon(m_stoppedIcon);
      m_webServer1Label.setText(getWebServer1Label() + FAILED_LABEL);
      m_webServer1Status.setFailed();

      testEnableControls();
    }

    public void triggerEncountered() {
      m_webServer1OutView.setListener(null);
      processReady();
    }

    public void processReady() {
      trace(getSelectedServerLabel() + "1.processReady");

      m_webServer1Status.setReady();
      m_webServer1Label.setIcon(m_readyIcon);
      m_webServer1Label.setText(getWebServer1Label() + READY_LABEL);

      m_webServer1Monitor = new WebServerShutdownMonitor(SERVER1_PORT, m_webServer1ShutdownListener);
      m_webServer1Monitor.start();

      testEnableControls();
    }
  }

  private void restartWebServer1() {
    stopWebServer1(true);
  }

  private boolean isWebServer1Ready() {
    return m_webServer1Status.isReady();
  }

  private void stopWebServer1() {
    stopWebServer1(false);
  }

  private void stopWebServer1(boolean restart) {
    if (m_webServer1Monitor != null) {
      m_webServer1Monitor.cancel();
      m_webServer1Monitor = null;
    }

    m_webServer1Label.setIcon(m_waitingIcon);
    m_webServer1Label.setText(getWebServer1Label() + STOPPING_LABEL);
    m_webServer1Status.setWaiting();
    m_webServer1ShutdownListener.setRestart(restart);

    stopWebServerAndNotify(m_webServer1OutView, SERVER1_PORT, m_webServer1ShutdownListener);
  }

  class WebServer1ShutdownListener implements ShutdownListener {
    boolean m_restart = false;

    void setRestart(boolean restart) {
      m_restart = restart;
    }

    public void processError(Exception e) {
      trace(getSelectedServerLabel() + "1.processError exception=" + e.getMessage());
      if (m_debug) e.printStackTrace();

      if (!m_quitting) {
        m_webServer1Status.setReady();
        m_webServer1Label.setIcon(m_readyIcon);
        m_webServer1Label.setText(getWebServer1Label() + READY_LABEL);
      } else {
        m_webServer1Status.setFailed();
        m_webServer1Label.setIcon(m_stoppedIcon);
        m_webServer1Label.setText(getWebServer1Label() + FAILED_LABEL);
      }

      testEnableControls();
    }

    public void processFailed(String errorBuf) {
      trace(getSelectedServerLabel() + "1.processFailed");

      m_webServer1OutView.append(errorBuf);

      if (!m_quitting) {
        m_webServer1Status.setReady();
        m_webServer1Label.setIcon(m_readyIcon);
        m_webServer1Label.setText(getWebServer1Label() + READY_LABEL);
      } else {
        m_webServer1Status.setFailed();
        m_webServer1Label.setIcon(m_stoppedIcon);
        m_webServer1Label.setText(getWebServer1Label() + FAILED_LABEL);
      }

      testEnableControls();
    }

    public void processStopped() {
      trace(getSelectedServerLabel() + "1.processStopped");

      m_webServer1Monitor = null;
      m_webServer1Status.setInactive();
      if (m_restarting && isDsoEnabled()) {
        m_webServer1Label.setText(getWebServer1Label() + WAITING_LABEL);
        if (m_webServer2Status.isInactive()) {
          startL2();
        }
      } else {
        if (m_restart) {
          startWebServer1();
        } else {
          m_webServer1Label.setIcon(m_stoppedIcon);
          m_webServer1Label.setText(getWebServer1Label() + STOPPED_LABEL);
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
    if (m_webServer2Monitor != null) {
      m_webServer2Monitor.cancel();
      m_webServer2Monitor = null;
    }

    if (isWebServer2Ready()) {
      m_webServer2Status.setRestarting(true);
      restartWebServer2();
      return;
    }

    m_webServer2Label.setIcon(m_waitingIcon);
    m_webServer2Label.setText(getWebServer2Label() + STARTING_LABEL);
    m_webServer2Status.setWaiting();
    m_webServer2OutView.setListener(m_webServer2StartupListener);
    m_webServer2OutView.setListenerTrigger(getSelectedServerStartupTrigger());
    startWebServerAndNotify(m_webServer2OutView, SERVER2_PORT, m_webServer2StartupListener);
  }

  class WebServer2StartupListener implements StartupListener, OutputStreamListener {
    public void startupError(Exception e) {
      trace(getSelectedServerLabel() + "2.startupError exception=" + e.getMessage());

      m_webServer2Label.setIcon(m_stoppedIcon);
      m_webServer2Label.setText(getWebServer2Label() + FAILED_LABEL);
      m_webServer2Status.setFailed();

      testEnableControls();
    }

    public void processFailed() {
      trace(getSelectedServerLabel() + "2.processFailed");

      m_webServer2Label.setIcon(m_stoppedIcon);
      m_webServer2Label.setText(getWebServer2Label() + FAILED_LABEL);
      m_webServer2Status.setFailed();

      testEnableControls();
    }

    public void triggerEncountered() {
      m_webServer2OutView.setListener(null);
      processReady();
    }

    public void processReady() {
      trace(getSelectedServerLabel() + "2.processReady");

      m_webServer2Status.setReady();
      m_webServer2Label.setIcon(m_readyIcon);
      m_webServer2Label.setText(getWebServer2Label() + READY_LABEL);

      m_webServer2Monitor = new WebServerShutdownMonitor(SERVER2_PORT, m_webServer2ShutdownListener);
      m_webServer2Monitor.start();

      testEnableControls();
    }
  }

  private void restartWebServer2() {
    stopWebServer2(true);
  }

  private boolean isWebServer2Ready() {
    return m_webServer2Status.isReady();
  }

  private void stopWebServer2() {
    stopWebServer2(false);
  }

  private void stopWebServer2(boolean restart) {
    if (m_webServer2Monitor != null) {
      m_webServer2Monitor.cancel();
      m_webServer2Monitor = null;
    }

    m_webServer2Label.setIcon(m_waitingIcon);
    m_webServer2Label.setText(getWebServer2Label() + STOPPING_LABEL);
    m_webServer2Status.setWaiting();
    m_webServer2ShutdownListener.setRestart(restart);

    stopWebServerAndNotify(m_webServer2OutView, SERVER2_PORT, m_webServer2ShutdownListener);
  }

  class WebServer2ShutdownListener implements ShutdownListener {
    boolean m_restart = false;

    void setRestart(boolean restart) {
      m_restart = restart;
    }

    public void processError(Exception e) {
      trace(getSelectedServerLabel() + "2.processError");
      if (m_debug) e.printStackTrace();

      if (!m_quitting) {
        m_webServer2Status.setReady();
        m_webServer2Label.setIcon(m_readyIcon);
        m_webServer2Label.setText(getWebServer2Label() + READY_LABEL);
      } else {
        m_webServer2Status.setFailed();
        m_webServer2Label.setIcon(m_stoppedIcon);
        m_webServer2Label.setText(getWebServer2Label() + FAILED_LABEL);
      }

      testEnableControls();
    }

    public void processFailed(String errorBuf) {
      trace(getSelectedServerLabel() + "2.processFailed");

      m_webServer2OutView.append(errorBuf);

      if (!m_quitting) {
        m_webServer2Status.setReady();
        m_webServer2Label.setIcon(m_readyIcon);
        m_webServer2Label.setText(getWebServer2Label() + READY_LABEL);
      } else {
        m_webServer2Status.setFailed();
        m_webServer2Label.setIcon(m_stoppedIcon);
        m_webServer2Label.setText(getWebServer2Label() + FAILED_LABEL);
      }

      testEnableControls();
    }

    public void processStopped() {
      trace(getSelectedServerLabel() + "2.processStopped");

      m_webServer2Monitor = null;
      m_webServer2Status.setInactive();
      if (m_restarting && isDsoEnabled()) {
        m_webServer2Label.setText(getWebServer2Label() + WAITING_LABEL);
        if (m_webServer1Status.isInactive()) {
          startL2();
        }
      } else {
        if (m_restart) {
          startWebServer2();
        } else {
          m_webServer2Label.setIcon(m_stoppedIcon);
          m_webServer2Label.setText(getWebServer2Label() + STOPPED_LABEL);
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
      String dso = isDsoEnabled() ? "dso" : "nodso";
      String[] args = new String[] { getSelectedServerName(), Integer.toString(port), dso };

      process = invokeScript(WEBSERVER_STARTUP_SCRIPT, args);
      IOUtils.closeQuietly(process.getOutputStream());
      new ProcessMonitor(process, new ProcessTerminationListener() {
        public void processTerminated(int exitCode) {
          if (m_debug) {
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

  class WebServerStartupMonitor extends Thread {
    private Process         m_process;
    private int             m_port;
    private StartupListener m_startupListener;

    WebServerStartupMonitor(Process process, int port, StartupListener listener) {
      super();

      m_process = process;
      m_port = port;
      m_startupListener = listener;
    }

    public void run() {
      while (true) {
        try {
          m_process.exitValue();
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              m_startupListener.processFailed();
            }
          });
          return;
        } catch (IllegalThreadStateException itse) {/**/
        }

        try {
          safeCloseSocket(new Socket("localhost", m_port));
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
      String[] args = new String[] { getSelectedServerName(), Integer.toString(port) };

      process = invokeScript(WEBSERVER_SHUTDOWN_SCRIPT, args);
      IOUtils.closeQuietly(process.getOutputStream());
    } catch (Exception e) {
      shutdownListener.processError(e);
      return;
    }

    new WebServerShutdownMonitor(process, port, shutdownListener).start();
  }

  class WebServerShutdownMonitor extends Thread {
    private Process          m_process;
    private int              m_port;
    private ShutdownListener m_shutdownListener;
    private boolean          m_stop;

    WebServerShutdownMonitor(int port, ShutdownListener listener) {
      this(null, port, listener);
    }

    WebServerShutdownMonitor(Process process, int port, ShutdownListener listener) {
      super();

      m_process = process;
      m_port = port;
      m_shutdownListener = listener;
    }

    public void run() {
      ProcessWaiter waiter = null;

      if (m_process != null) {
        waiter = new ProcessWaiter(m_process);
        waiter.start();
      }

      while (!m_stop) {
        if (m_process != null) {
          try {
            int exitCode = m_process.exitValue();

            if (exitCode != 0) {
              final String errorBuf = waiter.getErrorBuffer();
              SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                  m_shutdownListener.processFailed(errorBuf);
                }
              });
              return;
            } else {
              m_process = null;
            }
          } catch (IllegalThreadStateException itse) {/**/
          }
        }

        if (!m_stop) {
          try {
            safeCloseSocket(new Socket("localhost", m_port));
          } catch (Exception e) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                m_shutdownListener.processStopped();
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
      m_stop = true;
    }
  }

  // End -- Process control support

  private void stopAll() throws Exception {
    if (m_webServer1Status.isReady()) {
      stopWebServer1();
    }
    if (m_webServer2Status.isReady()) {
      stopWebServer2();
    }
    if (m_l2Status.isReady()) {
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
      m_webAppTreeModel.updateLinks(false, false);
      disableControls();
      stopAll();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean anyReady() {
    return m_l2Status.isReady() || m_webServer1Status.isReady() || m_webServer2Status.isReady();
  }

  private boolean anyRestarting() {
    return m_l2Status.isRestarting() || m_webServer1Status.isRestarting() || m_webServer2Status.isRestarting();
  }

  private boolean anyWaiting() {
    return m_l2Status.isWaiting() || m_webServer1Status.isWaiting() || m_webServer2Status.isWaiting();
  }

  private void disableControls() {
    m_webServer1EnabledToggle.setEnabled(false);
    m_webServer2EnabledToggle.setEnabled(false);
    m_dsoEnabledToggle.setEnabled(false);

    m_startButton.setEnabled(false);
    m_stopButton.setEnabled(false);

    m_webServer1Control.setVisible(false);
    m_webServer2Control.setVisible(false);

    selectControlTab();
    setConfigTabEnabled(false);
    setMonitorTabEnabled(false);

    m_serversAction.setEnabled(false);
    m_importAction.setEnabled(false);
    m_webAppTreeModel.setRefreshEnabled(false);
    m_webAppTreeModel.setRemoveEnabled(false);

    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  private synchronized void testEnableControls() {
    boolean anyRestarting = anyRestarting();
    boolean anyWaiting = anyWaiting();

    if (anyRestarting || anyWaiting) { return; }

    m_restarting = false;

    boolean anyReady = anyReady();

    if (!anyWaiting && !anyRestarting && anyReady && m_quitting) {
      stopSystem();
    }

    if (!anyWaiting && !anyRestarting && !anyReady) {
      if (m_quitting) {
        SessionIntegrator.getContext().client.shutdown();
        return;
      } else {
        m_serversAction.setEnabled(true);
        m_importAction.setEnabled(true);
        m_webAppTreeModel.setRefreshEnabled(true);
        m_webAppTreeModel.setRemoveEnabled(true);
      }
    }

    m_webServer1EnabledToggle.setEnabled(!anyWaiting && !anyRestarting && !anyReady);
    m_webServer2EnabledToggle.setEnabled(!anyWaiting && !anyRestarting && !anyReady);
    m_dsoEnabledToggle.setEnabled(!anyWaiting && !anyRestarting && !anyReady);
    m_startButton.setEnabled(!anyWaiting && !anyRestarting);
    m_stopButton.setEnabled(!anyWaiting && !anyRestarting && anyReady);

    if ((!anyWaiting && !anyReady) || anyRestarting) {
      m_webServer1Control.setVisible(false);
      m_webServer2Control.setVisible(false);

      m_startButton.setText(getBundleString("start.all.label"));
    } else {
      testEnableWebServer1Control();
      testEnableWebServer2Control();

      m_startButton.setText(getBundleString("restart.all.label"));
    }

    if (!anyWaiting && !anyRestarting) {
      updateLinks();
      setConfigTabEnabled(true);
      setMonitorTabEnabled(isDsoEnabled());
      setCursor(STANDARD_CURSOR);
    }
  }

  private void testEnableWebServer1Control() {
    boolean webServer1NotWaiting = !m_webServer1Status.isWaiting();
    m_webServer1Control.setVisible(webServer1NotWaiting);
    m_webServer1Control.setEnabled(webServer1NotWaiting);
    if (webServer1NotWaiting) {
      boolean webServer1Ready = isWebServer1Ready();

      m_webServer1Control.setIcon(webServer1Ready ? m_stopIcon : m_startIcon);

      String tip = (webServer1Ready ? getBundleString("stop.label") : getBundleString("start.label")) + " "
                   + getWebServer1Label();
      m_webServer1Control.setToolTipText(tip);
    }
  }

  private void testEnableWebServer2Control() {
    boolean webServer2NotWaiting = !m_webServer2Status.isWaiting();
    m_webServer2Control.setVisible(webServer2NotWaiting);
    m_webServer2Control.setEnabled(webServer2NotWaiting);
    if (webServer2NotWaiting) {
      boolean webServer2Ready = isWebServer2Ready();

      m_webServer2Control.setIcon(webServer2Ready ? m_stopIcon : m_startIcon);

      String tip = (webServer2Ready ? getBundleString("stop.label") : getBundleString("start.label")) + " "
                   + getWebServer2Label();
      m_webServer2Control.setToolTipText(tip);
    }
  }

  private void updateLinks() {
    m_webAppTreeModel.updateLinks(isWebServer1Ready(), isWebServer2Ready());
  }

  private void saveConfig() {
    saveXML(m_xmlPane.getText());
  }

  public void modelChanged() {
    setupEditorPanels();
    updateXmlPane();

    if (false && isL2Ready() && !m_handlingAppEvent) {
      queryRestart();
    }
  }

  public void saveXML(String xmlText) {
    m_configHelper.save(xmlText);
    setupEditorPanels();
    setXmlModified(false);

    if (isConfigTabSelected() && isL2Ready()) {
      m_askRestart = true;
    }
  }

  private static void trace(String msg) {
    if (m_debug) {
      System.out.println(msg);
      System.out.flush();
    }
  }

  // Everything belows goes into com.tc.ui.common.Frame

  private Preferences getPreferences() {
    SessionIntegratorContext cntx = SessionIntegrator.getContext();
    return cntx.prefs.node("SessionIntegratorFrame");
  }

  private void storePreferences() {
    SessionIntegratorContext cntx = SessionIntegrator.getContext();
    cntx.client.storePrefs();
  }

  public Rectangle getDefaultBounds() {
    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension size = tk.getScreenSize();
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice device = env.getDefaultScreenDevice();
    GraphicsConfiguration config = device.getDefaultConfiguration();
    Insets insets = tk.getScreenInsets(config);

    size.width -= (insets.left + insets.right);
    size.height -= (insets.top + insets.bottom);

    int width = (int) (size.width * 0.75f);
    int height = (int) (size.height * 0.66f);

    // center
    int x = size.width / 2 - width / 2;
    int y = size.height / 2 - height / 2;

    return new Rectangle(x, y, width, height);
  }

  private String getBoundsString() {
    Rectangle b = getBounds();
    return b.x + "," + b.y + "," + b.width + "," + b.height;
  }

  private int parseInt(String s) {
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      return 0;
    }
  }

  private Rectangle parseBoundsString(String s) {
    String[] split = s.split(",");
    int x = parseInt(split[0]);
    int y = parseInt(split[1]);
    int width = parseInt(split[2]);
    int height = parseInt(split[3]);

    return new Rectangle(x, y, width, height);
  }

  public void storeBounds() {
    if (getName() != null && (getExtendedState() & NORMAL) == NORMAL) {
      getPreferences().put("Bounds", getBoundsString());
      storePreferences();
    }
  }

  protected Rectangle getPreferredBounds() {
    Preferences prefs = getPreferences();
    String s = prefs.get("Bounds", null);

    return s != null ? parseBoundsString(s) : getDefaultBounds();
  }

  // TODO: make each SplitPane manage its own preference value.

  private int getSplitPref(JSplitPane splitter) {
    Preferences prefs = getPreferences();
    Preferences splitPrefs = prefs.node(splitter.getName());

    return splitPrefs.getInt("Split", -1);
  }

  private JSplitPane getControlSplitter() {
    if (m_controlSplitter == null) {
      m_controlSplitter = (SplitPane) findComponent("ControlSplitter");
      m_controlDividerLocation = new Integer(getSplitPref(m_controlSplitter));

      if (m_dividerListener == null) {
        m_dividerListener = new DividerListener();
      }
    }

    return m_controlSplitter;
  }

  class DividerListener implements PropertyChangeListener {
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

      if (m_controlSplitter.getName().equals(name)) {
        m_controlDividerLocation = divLocObj;
      }
    }
  }

  public void doLayout() {
    super.doLayout();

    JSplitPane splitter = getControlSplitter();
    if (m_controlDividerLocation != null) {
      splitter.setDividerLocation(m_controlDividerLocation.intValue());
    }
  }

  public void addNotify() {
    super.addNotify();
    getControlSplitter().addPropertyChangeListener(m_dividerListener);
  }

  public void removeNotify() {
    getControlSplitter().removePropertyChangeListener(m_dividerListener);
    super.removeNotify();
  }
}
