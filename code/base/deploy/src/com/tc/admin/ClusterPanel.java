/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.SyncHTMLEditorKit;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTextPane;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IProductVersion;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;

public class ClusterPanel extends XContainer implements HyperlinkListener {
  private IAdminClientContext adminClientContext;
  private ClusterNode         clusterNode;
  private final XContainer    connectedPanel;
  private final XContainer    disconnectedPanel;
  private final XButton       disconnectButton;
  private XLabel              connectSummaryLabel;
  private PagedView           pagedView;
  private final XTextPane     introPane;
  private JTextField          hostField;
  private JTextField          portField;
  private final JCheckBox     autoConnectToggle;
  private JButton             connectButton;
  static private ImageIcon    connectIcon;
  static private ImageIcon    disconnectIcon;
  private ConnectPanel        connectPanel;
  private StatusView          statusView;
  private ProductInfoPanel    productInfoPanel;

  private static final String DISCONNECTED_PAGE = "disconnected";
  private static final String CONNECTED_PAGE    = "connected";

  static {
    connectIcon = new ImageIcon(ClusterPanel.class.getResource("/com/tc/admin/icons/disconnect_co.gif"));
    disconnectIcon = new ImageIcon(ClusterPanel.class.getResource("/com/tc/admin/icons/newex_wiz.gif"));
  }

  public ClusterPanel(IAdminClientContext adminClientContext, ClusterNode clusterNode) {
    super(clusterNode);

    this.adminClientContext = adminClientContext;
    this.clusterNode = clusterNode;

    setLayout(new BorderLayout());
    add(pagedView = new PagedView());

    disconnectedPanel = new XContainer(new BorderLayout());

    XContainer centerPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    centerPanel.add(connectPanel = new ConnectPanel(adminClientContext), gbc);
    gbc.gridy++;

    hostField = connectPanel.getHostField();
    portField = connectPanel.getPortField();
    autoConnectToggle = connectPanel.getAutoConnectToggle();
    connectButton = connectPanel.getConnectButton();

    gbc.anchor = GridBagConstraints.WEST;
    centerPanel.add(statusView = new StatusView(), gbc);
    setStatusLabel("Not connected");
    disconnectedPanel.add(centerPanel, BorderLayout.CENTER);

    productInfoPanel = new ProductInfoPanel();
    disconnectedPanel.add(productInfoPanel, BorderLayout.SOUTH);
    productInfoPanel.init("foo", "", "bar");
    productInfoPanel.setVisible(false);

    hostField.addActionListener(new HostFieldHandler());
    hostField.addFocusListener(new HostFieldFocusListener());

    portField.addActionListener(new PortFieldHandler());
    portField.addFocusListener(new PortFieldFocusListener());

    autoConnectToggle.addActionListener(new AutoConnectHandler());
    connectButton.addActionListener(new ConnectionButtonHandler());

    hostField.setText(clusterNode.getHost());
    portField.setText(Integer.toString(clusterNode.getPort()));

    setupConnectButton();

    pagedView.add(DISCONNECTED_PAGE, disconnectedPanel);

    connectedPanel = new XContainer(new BorderLayout());
    XContainer topPanel = new XContainer(new GridBagLayout());
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.WEST;

    disconnectButton = new XButton(adminClientContext.getString("disconnect"), disconnectIcon);
    topPanel.add(disconnectButton, gbc);
    gbc.gridx++;
    disconnectButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        disconnect();
      }
    });

    topPanel.add(connectSummaryLabel = new XLabel(), gbc);
    gbc.gridx++;

    // filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    topPanel.add(new XLabel(), gbc);

    connectedPanel.add(topPanel, BorderLayout.NORTH);

    introPane = new XTextPane();
    introPane.setEditorKit(new SyncHTMLEditorKit());
    try {
      introPane.setPage(getClass().getResource("Intro.html"));
    } catch (Exception e) {
      adminClientContext.log(e);
    }
    introPane.addHyperlinkListener(this);
    introPane.setEditable(false);
    connectedPanel.add(new XScrollPane(introPane));
    pagedView.add(CONNECTED_PAGE, connectedPanel);
  }

  public void hyperlinkUpdate(HyperlinkEvent e) {
    HyperlinkEvent.EventType type = e.getEventType();
    Element elem = e.getSourceElement();

    if (elem == null || type == HyperlinkEvent.EventType.ENTERED || type == HyperlinkEvent.EventType.EXITED) { return; }

    if (introPane.getCursor().getType() != Cursor.WAIT_CURSOR) {
      AttributeSet a = elem.getAttributes();
      AttributeSet anchor = (AttributeSet) a.getAttribute(HTML.Tag.A);
      String action = (String) anchor.getAttribute(HTML.Attribute.HREF);

      hyperlinkActivated(anchor, action);
    }
  }

  protected void hyperlinkActivated(AttributeSet anchor, String action) {
    adminClientContext.getAdminClientController().selectNode(clusterNode, action);
  }

  @Override
  public void addNotify() {
    super.addNotify();
    JRootPane rootPane = getRootPane();
    if (rootPane != null) {
      rootPane.setDefaultButton(connectButton);
    }
    IClusterModel clusterModel = clusterNode.getClusterModel();
    if (!clusterModel.isConnected() && clusterNode.isAutoConnect()) {
      clusterModel.connect();
    }
  }

  void reinitialize() {
    hostField.setText(clusterNode.getHost());
    portField.setText(Integer.toString(clusterNode.getPort()));
  }

  class HostFieldFocusListener implements FocusListener {
    public void focusGained(FocusEvent e) {
      /**/
    }

    public void focusLost(FocusEvent fe) {
      updateHost();
    }
  }

  private void updateHost() {
    String host = hostField.getText().trim();

    if (host == null || host.length() == 0) {
      hostField.setText(host = "localhost");
    }
    clusterNode.setHost(host);
    clusterNode.nodeChanged();
    adminClientContext.getAdminClientController().updateServerPrefs();
  }

  class HostFieldHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      updateHost();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          connectButton.doClick();
        }
      });
    }
  }

  class PortFieldFocusListener implements FocusListener {
    public void focusGained(FocusEvent e) {
      /**/
    }

    public void focusLost(FocusEvent fe) {
      updatePort();
    }
  }

  private boolean updatePort() {
    String portText = portField.getText().trim();

    try {
      clusterNode.setPort(Integer.parseInt(portText));
      clusterNode.nodeChanged();
      adminClientContext.getAdminClientController().updateServerPrefs();
      return true;
    } catch (Exception e) {
      Toolkit.getDefaultToolkit().beep();
      adminClientContext.log("'" + portText + "' not a number");
      portField.setText(Integer.toString(clusterNode.getPort()));
    }
    return false;
  }

  class PortFieldHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      if (updatePort()) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            connectButton.doClick();
          }
        });
      }
    }
  }

  class AutoConnectHandler implements ActionListener, Runnable {
    public void actionPerformed(ActionEvent ae) {
      SwingUtilities.invokeLater(this);
    }

    public void run() {
      boolean autoConnect = autoConnectToggle.isSelected();
      clusterNode.setAutoConnect(autoConnect);
      if (!clusterNode.isConnected()) {
        connectButton.setEnabled(!autoConnect);
      }
    }
  }

  class ConnectionButtonHandler implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      connectButton.setEnabled(false);
      if (clusterNode.isConnected()) {
        disconnect();
        if (clusterNode.isConnected()) {
          connectButton.setEnabled(true);
        }
      } else {
        connect();
      }
    }
  }

  void setupConnectButton() {
    String label;
    Icon icon;
    boolean enabled;
    boolean connected = clusterNode.isConnected();
    boolean autoConnect = clusterNode.isAutoConnect();

    if (connected) {
      label = adminClientContext.getString("disconnect");
      icon = disconnectIcon;
      enabled = true;
    } else {
      label = adminClientContext.getString("connect.elipses");
      icon = connectIcon;
      enabled = !autoConnect;
    }

    connectButton.setText(label);
    connectButton.setIcon(icon);
    connectButton.setEnabled(enabled);
    autoConnectToggle.setSelected(autoConnect);
  }

  JButton getConnectButton() {
    return connectButton;
  }

  private void connect() {
    setStatusLabel("Connecting...");
    clusterNode.connect();
  }

  void activated() {
    adminClientContext.execute(new ActivatedWorker());
  }

  private class ActivatedWorker extends BasicWorker<Date> {
    private ActivatedWorker() {
      super(new Callable<Date>() {
        public Date call() throws Exception {
          return new Date(clusterNode.getActivateTime());
        }
      }, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          adminClientContext.log(e);
        }
      } else {
        handleConnected("server.activated.label", getResult());
      }
    }
  }

  private void handleConnected(String labelKey, Date time) {
    hostField.setEditable(false);
    portField.setEditable(false);
    setupConnectButton();
    setStatusLabel(adminClientContext.format(labelKey, time.toString()));
    testShowProductInfo();
    connectSummaryLabel.setText("Connected to " + hostField.getText() + ":" + portField.getText() + " at " + time);
    pagedView.setPage(CONNECTED_PAGE);
  }

  /**
   * The only differences between activated() and started() is the status message and the serverlog is only added in
   * activated() under the presumption that a non-active server won't be saying anything.
   */
  void started() {
    adminClientContext.execute(new StartedWorker());
  }

  private class StartedWorker extends BasicWorker<Date> {
    private StartedWorker() {
      super(new Callable<Date>() {
        public Date call() throws Exception {
          return new Date(clusterNode.getStartTime());
        }
      }, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          adminClientContext.log(e);
        }
      } else {
        handleConnected("server.started.label", getResult());
      }
    }
  }

  void passiveUninitialized() {
    handleConnected("server.initializing.label", new Date());
  }

  void passiveStandby() {
    handleConnected("server.standingby.label", new Date());
  }

  private void disconnect() {
    clusterNode.getDisconnectAction().actionPerformed(null);
  }

  void disconnected() {
    hostField.setEditable(true);
    portField.setEditable(true);

    String startTime = new Date().toString();
    setupConnectButton();
    setStatusLabel(adminClientContext.format("server.disconnected.label", startTime));
    hideProductInfo();

    adminClientContext.setStatus(adminClientContext.format("server.disconnected.status", clusterNode, startTime));

    pagedView.setPage(DISCONNECTED_PAGE);
  }

  void setStatusLabel(String msg) {
    statusView.setText(msg);
    ServerHelper.getHelper().setStatusView(clusterNode.getActiveCoordinator(), statusView);
    statusView.setVisible(true);
    statusView.revalidate();
    statusView.paintImmediately(0, 0, statusView.getWidth(), statusView.getHeight());
  }

  boolean isProductInfoShowing() {
    return productInfoPanel.isVisible();
  }

  private void testShowProductInfo() {
    if (!isProductInfoShowing()) {
      adminClientContext.execute(new ProductInfoWorker());
    }
  }

  private class ProductInfoWorker extends BasicWorker<IProductVersion> {
    private ProductInfoWorker() {
      super(new Callable<IProductVersion>() {
        public IProductVersion call() throws Exception {
          return clusterNode.getProductInfo();
        }
      }, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          adminClientContext.log(e);
        }
      } else {
        showProductInfo(getResult());
      }
    }
  }

  private void showProductInfo(IProductVersion productInfo) {
    if (productInfo != null) {
      productInfoPanel.init(productInfo.version(), productInfo.patchLevel(), productInfo.copyright());
      productInfoPanel.setVisible(true);
      revalidate();
      repaint();
    }
  }

  private void hideProductInfo() {
    productInfoPanel.setVisible(false);
    revalidate();
    repaint();
  }

  @Override
  public void tearDown() {
    super.tearDown();

    statusView.tearDown();
    productInfoPanel.tearDown();

    synchronized (this) {
      adminClientContext = null;
      clusterNode = null;
      hostField = null;
      portField = null;
      connectButton = null;
      statusView = null;
      productInfoPanel = null;
    }
  }
}
