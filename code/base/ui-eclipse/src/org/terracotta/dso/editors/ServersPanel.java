/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.editors.chooser.FolderBehavior;
import org.terracotta.dso.editors.chooser.NavigatorBehavior;
import org.terracotta.dso.editors.chooser.PackageNavigator;
import org.terracotta.dso.editors.xmlbeans.XmlConfigContext;
import org.terracotta.dso.editors.xmlbeans.XmlConfigEvent;
import org.terracotta.dso.editors.xmlbeans.XmlConfigUndoContext;
import org.terracotta.ui.util.SWTLayout;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;
import com.terracottatech.config.Persistence;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;

import java.util.LinkedList;
import java.util.List;

public final class ServersPanel extends ConfigurationEditorPanel {

  private static final int NAME_INDEX     = 0;
  private static final int HOST_INDEX     = 1;
  private static final int DSO_PORT_INDEX = 2;
  private static final int JMX_PORT_INDEX = 3;

  private final Layout     m_layout;
  private State            m_state;

  public ServersPanel(Composite parent, int style) {
    super(parent, style);
    this.m_layout = new Layout(this);
    providePersistComboDefaults();
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  // ================================================================================
  // INTERFACE
  // ================================================================================

  public synchronized void init(Object data) {
    if (m_isActive && m_state.project == (IProject) data) return;
    setActive(false);
    m_state = new State((IProject) data);
    createContextListeners();
    setActive(true);
    initTableItems(m_state.xmlContext.getParentElementProvider().hasServers());
  }

  public synchronized void clearState() {
    setActive(false);
    m_layout.reset();
    m_state.xmlContext.detachComponentModel(this);
    m_state = null;
  }

  public synchronized void refreshContent() {
    m_layout.reset();
    initTableItems(m_state.xmlContext.getParentElementProvider().hasServers());
    if (m_state.selectionIndex < m_layout.m_serverTable.getItemCount()) {
      m_layout.m_serverTable.setSelection(m_state.selectionIndex);
      handleTableSelection();
    }
  }
  
  public void detach() {
    m_state.xmlContext.detachComponentModel(this);
  }
  
  // ================================================================================
  // INIT LISTENERS
  // ================================================================================

  private void initTableItems(Servers servers) {
    if (servers == null) return;
    m_layout.m_serverTable.setEnabled(false);
    m_state.serverIndices.clear();
    Server[] serverElements = servers.getServerArray();
    for (int i = 0; i < serverElements.length; i++) {
      createServerItem(serverElements[i]);
      updateServerListeners(serverElements[i]);
      m_layout.m_dataBrowse.setEnabled(true);
      m_layout.m_logsBrowse.setEnabled(true);
    }
    m_layout.m_serverTable.setEnabled(true);
  }

  private void createContextListeners() {
    registerServersElementBehavior(XmlConfigEvent.SERVER_NAME, NAME_INDEX, m_layout.m_nameField);
    registerServersElementBehavior(XmlConfigEvent.SERVER_HOST, HOST_INDEX, m_layout.m_hostField);
    registerServersElementBehavior(XmlConfigEvent.SERVER_DSO_PORT, DSO_PORT_INDEX, m_layout.m_dsoPortField);
    registerServersElementBehavior(XmlConfigEvent.SERVER_JMX_PORT, JMX_PORT_INDEX, m_layout.m_jmxPortField);
    registerFieldBehavior(XmlConfigEvent.SERVER_DATA, m_layout.m_dataLocation);
    registerFieldBehavior(XmlConfigEvent.SERVER_LOGS, m_layout.m_logsLocation);
    registerPersistenceModeBehavior(XmlConfigEvent.SERVER_PERSIST, m_layout.m_persistenceModeCombo);
    registerGCIntervalBehavior(XmlConfigEvent.SERVER_GC_INTERVAL, m_layout.m_gcIntervalSpinner);
    registerGCCheckBehavior(XmlConfigEvent.SERVER_GC, m_layout.m_gcCheck);
    registerGCCheckBehavior(XmlConfigEvent.SERVER_GC_VERBOSE, m_layout.m_verboseCheck);
    // - new server
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        Server server = (Server) castEvent(e).element;
        TableItem item = createServerItem(server);
        m_state.xmlContext.notifyListeners(new XmlConfigEvent(XmlConfigContext.DEFAULT_NAME, null, server,
            XmlConfigEvent.SERVER_NAME));
        m_state.xmlContext.notifyListeners(new XmlConfigEvent(XmlConfigContext.DEFAULT_HOST, null, server,
            XmlConfigEvent.SERVER_HOST));
        m_layout.m_serverTable.setSelection(item);
        m_layout.m_removeServerButton.setEnabled(true);
        updateServerListeners(server);
        updateServerSubGroupListeners(server);
        m_layout.m_dataBrowse.setEnabled(true);
        m_layout.m_logsBrowse.setEnabled(true);
        m_state.selectionIndex = m_layout.m_serverTable.getSelectionIndex();
      }
    }, XmlConfigEvent.NEW_SERVER, this);
    // - context create server
    m_layout.m_addServerButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        m_state.xmlContext.notifyListeners(new XmlConfigEvent(XmlConfigEvent.CREATE_SERVER));
      }
    });
    // - table selection
    m_layout.m_serverTable.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        handleTableSelection();
      }
    });
    // - context delete server
    m_layout.m_removeServerButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        m_layout.m_removeServerButton.setEnabled(false);
        int row = m_layout.m_serverTable.getSelectionIndex();
        TableItem item = m_layout.m_serverTable.getItem(row);
        Server server = (Server) item.getData();
        m_state.xmlContext.notifyListeners(new XmlConfigEvent(server, XmlConfigEvent.DELETE_SERVER));
      }
    });
    // - remove server
    m_state.xmlContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        Server server = (Server) castEvent(e).element;
        int row = m_state.serverIndices.indexOf(server);
        m_state.serverIndices.remove(server);
        m_layout.m_serverTable.remove(row);
        m_layout.m_serverTable.deselectAll();
        m_layout.resetServerFields(false);
        m_state.selectionIndex = m_layout.m_serverTable.getSelectionIndex();
      }
    }, XmlConfigEvent.REMOVE_SERVER, this);
    // - browse data button
    m_layout.m_dataBrowse.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;if (!m_isActive) return;
        NavigatorBehavior behavior = new FolderBehavior();
        PackageNavigator dialog = new PackageNavigator(getShell(), behavior.getTitle(), m_state.project, behavior);
        dialog.addValueListener(new UpdateEventListener() {
          public void handleUpdate(UpdateEvent event) {
            m_state.xmlContext.notifyListeners(new XmlConfigEvent(event.data, null, getSelectedServer(),
                XmlConfigEvent.SERVER_DATA));
          }
        });
        dialog.open();
      }
    });
    // - browse logs button
    m_layout.m_logsBrowse.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        NavigatorBehavior behavior = new FolderBehavior();
        PackageNavigator dialog = new PackageNavigator(getShell(), behavior.getTitle(), m_state.project, behavior);
        dialog.addValueListener(new UpdateEventListener() {
          public void handleUpdate(UpdateEvent event) {
            m_state.xmlContext.notifyListeners(new XmlConfigEvent(event.data, null, getSelectedServer(),
                XmlConfigEvent.SERVER_LOGS));
          }
        });
        dialog.open();
      }
    });
  }

  // - handle field notifications
  private void registerFieldNotificationListener(final int type, final Text text) {
    text.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        handleFieldEvent(text, type);
      }
    });
    text.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.Selection) {
          handleFieldEvent(text, type);
        }
      }
    });
  }

  // - spinner behavior
  private void registerGCIntervalBehavior(final int type, final Spinner spinner) {
    spinner.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        handleSpinnerEvent(spinner, type);
      }
    });
    spinner.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.Selection) {
          spinner.getParent().forceFocus();
          spinner.forceFocus();
          handleSpinnerEvent(spinner, type);
        }
      }
    });
    spinner.addMouseListener(new MouseAdapter() {
      public void mouseUp(MouseEvent mouseevent) {
        handleSpinnerEvent(spinner, type);
      }
    });
    UpdateEventListener spinnerListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        int index = m_state.serverIndices.indexOf(event.variable);
        if (event.data == null) event.data = "0";
        if (m_layout.m_serverTable.getSelectionIndex() == index) {
          spinner.setEnabled(true);
          spinner.setCapture(true);
          spinner.setIncrement(30);
          spinner.setMaximum(86400); // 24hr
          spinner.setSelection(Integer.parseInt((String) event.data));
        }
      }
    };
    spinner.setData(spinnerListener);
    m_state.xmlContext.addListener(spinnerListener, type, this);
  }

  // - check box behavior
  private void registerGCCheckBehavior(final int type, final Button check) {
    check.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        Server server = getSelectedServer();
        m_state.xmlContext.notifyListeners(new XmlConfigEvent("" + check.getSelection(), (UpdateEventListener) check
            .getData(), server, type));
      }
    });
    UpdateEventListener checkListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        int index = m_state.serverIndices.indexOf(event.variable);
        if (m_layout.m_serverTable.getSelectionIndex() == index) {
          boolean select = Boolean.parseBoolean((String) event.data);
          check.setEnabled(true);
          check.setSelection(select);
        }
      }
    };
    check.setData(checkListener);
    m_state.xmlContext.addListener(checkListener, type, this);
  }

  // - combo behavior
  private void registerPersistenceModeBehavior(final int type, final Combo combo) {
    combo.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (!m_isActive) return;
        Server server = getSelectedServer();
        m_state.xmlContext.notifyListeners(new XmlConfigEvent(combo.getText(), (UpdateEventListener) combo.getData(),
            server, type));
      }
    });
    UpdateEventListener comboListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        int index = m_state.serverIndices.indexOf(event.variable);
        if (m_layout.m_serverTable.getSelectionIndex() == index) {
          combo.setEnabled(true);
          combo.select(combo.indexOf((String) event.data));
        }
      }
    };
    combo.setData(comboListener);
    m_state.xmlContext.addListener(comboListener, type, this);
  }

  // - field listeners
  private void registerFieldBehavior(int type, final Text text) {
    registerFieldNotificationListener(type, text);
    UpdateEventListener textListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        int index = m_state.serverIndices.indexOf(event.element);
        if (event.data == null) event.data = "";
        if (m_layout.m_serverTable.getSelectionIndex() == index) {
          text.setEnabled(true);
          text.setText((String) event.data);
        }
      }
    };
    text.setData(textListener);
    m_state.xmlContext.addListener(textListener, type, this);
  }

  // - table cell listeners
  private void registerServersElementBehavior(int type, final int column, final Text text) {
    registerFieldBehavior(type, text);
    UpdateEventListener itemListener = new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        if (!m_isActive) return;
        XmlConfigEvent event = castEvent(e);
        int index = m_state.serverIndices.indexOf(event.element);
        TableItem item = m_layout.m_serverTable.getItem(index);
        if (event.data == null) event.data = "";
        item.setText(column, (String) event.data);
      }
    };
    m_state.xmlContext.addListener(itemListener, type, this);
  }

  // ================================================================================
  // HELPERS
  // ================================================================================

  private void updateServerListeners(Server server) {
    updateListeners(XmlConfigEvent.SERVER_NAME, server);
    updateListeners(XmlConfigEvent.SERVER_HOST, server);
    updateListeners(XmlConfigEvent.SERVER_DSO_PORT, server);
    updateListeners(XmlConfigEvent.SERVER_JMX_PORT, server);
  }

  private void updateServerSubGroupListeners(Server server) {
    updateListeners(XmlConfigEvent.SERVER_DATA, server);
    updateListeners(XmlConfigEvent.SERVER_LOGS, server);
    updateListeners(XmlConfigEvent.SERVER_PERSIST, server);
    updateListeners(XmlConfigEvent.SERVER_GC, server);
    updateListeners(XmlConfigEvent.SERVER_GC_INTERVAL, server);
    updateListeners(XmlConfigEvent.SERVER_GC_VERBOSE, server);
  }

  private void updateListeners(int event, XmlObject element) {
    m_state.xmlContext.updateListeners(new XmlConfigEvent(element, event));
  }

  private Server getSelectedServer() {
    TableItem item = m_layout.m_serverTable.getItem(m_layout.m_serverTable.getSelectionIndex());
    return (Server) item.getData();
  }

  private TableItem createServerItem(XmlObject server) {
    TableItem item = new TableItem(m_layout.m_serverTable, SWT.NONE);
    item.setData(server);
    m_state.serverIndices.add((Server) server);
    return item;
  }

  private void providePersistComboDefaults() {
    String[] values = XmlConfigContext.getListDefaults(Persistence.class, XmlConfigEvent.SERVER_PERSIST);
    for (int i = 0; i < values.length; i++) {
      m_layout.m_persistenceModeCombo.add(values[i]);
    }
  }

  private void handleFieldEvent(Text text, int type) {
    if (!m_isActive) return;
    TableItem item = m_layout.m_serverTable.getItem(m_layout.m_serverTable.getSelectionIndex());
    Server server = (Server) item.getData();
    m_state.xmlContext.notifyListeners(new XmlConfigEvent(text.getText(), (UpdateEventListener) text.getData(), server,
        type));
  }

  private void handleSpinnerEvent(Spinner spinner, int type) {
    if (!m_isActive) return;
    TableItem item = m_layout.m_serverTable.getItem(m_layout.m_serverTable.getSelectionIndex());
    Server server = (Server) item.getData();
    m_state.xmlContext.notifyListeners(new XmlConfigEvent("" + spinner.getSelection(), (UpdateEventListener) spinner
        .getData(), server, type));
  }
  
  private void handleTableSelection() {
    if (m_state.selectionIndex == -1) m_layout.resetServerFields(true);
    m_state.selectionIndex = m_layout.m_serverTable.getSelectionIndex();
    m_layout.m_removeServerButton.setEnabled(true);
    Server server = getSelectedServer();
    updateServerListeners(server);
    updateServerSubGroupListeners(server);
  }

  // ================================================================================
  // STATE
  // ================================================================================

  private class State {
    final IProject             project;
    final XmlConfigContext     xmlContext;
    final XmlConfigUndoContext xmlUndoContext;
    final List<Server>         serverIndices;
    int                        selectionIndex;

    private State(IProject project) {
      this.project = project;
      this.xmlContext = XmlConfigContext.getInstance(project);
      this.xmlUndoContext = XmlConfigUndoContext.getInstance(project);
      this.serverIndices = new LinkedList<Server>();
    }
  }

  // ================================================================================
  // LAYOUT
  // ================================================================================

  private class Layout implements SWTLayout {

    private static final int    WIDTH_HINT         = 500;
    private static final int    HEIGHT_HINT        = 120;
    private static final String BROWSE             = "Browse...";
    private static final String ADD                = "Add...";
    private static final String REMOVE             = "Remove";
    private static final String SERVERS            = "Servers";
    private static final String NAME               = "Name";
    private static final String HOST               = "Host";
    private static final String DSO_PORT           = "DSO Port";
    private static final String JMX_PORT           = "JMX Port";
    private static final String SERVER             = "Server";
    private static final String DATA               = "Data";
    private static final String LOGS               = "Logs";
    private static final String PERSISTENCE_MODE   = "Persistence Mode";
    private static final String GARBAGE_COLLECTION = "Garbage Collection";
    private static final String VERBOSE            = "Verbose";
    private static final String GC_INTERVAL        = "GC Interval (seconds)";

    private Table               m_serverTable;
    private Button              m_addServerButton;
    private Button              m_removeServerButton;
    private Text                m_nameField;
    private Text                m_hostField;
    private Text                m_dsoPortField;
    private Text                m_jmxPortField;
    private Text                m_dataLocation;
    private Text                m_logsLocation;
    private Combo               m_persistenceModeCombo;
    private Button              m_gcCheck;
    private Spinner             m_gcIntervalSpinner;
    private Button              m_verboseCheck;
    private Button              m_logsBrowse;
    private Button              m_dataBrowse;
    private Group               m_serverGroup;

    public void reset() {
      m_serverTable.removeAll();
      resetServerFields(false);
    }

    private void resetServerFields(boolean enabled) {
      m_nameField.setText("");
      m_nameField.setEnabled(enabled);
      m_hostField.setText("");
      m_hostField.setEnabled(enabled);
      m_dsoPortField.setText("");
      m_dsoPortField.setEnabled(enabled);
      m_jmxPortField.setText("");
      m_jmxPortField.setEnabled(enabled);
      m_dataLocation.setText("");
      m_dataLocation.setEnabled(enabled);
      m_logsLocation.setText("");
      m_logsLocation.setEnabled(enabled);
      m_persistenceModeCombo.deselectAll();
      m_persistenceModeCombo.setEnabled(enabled);
      m_gcCheck.setSelection(enabled);
      m_gcCheck.setEnabled(enabled);
      m_gcIntervalSpinner.setSelection(0);
      m_gcIntervalSpinner.setEnabled(enabled);
      m_verboseCheck.setSelection(enabled);
      m_verboseCheck.setEnabled(enabled);
      m_dataBrowse.setEnabled(enabled);
      m_logsBrowse.setEnabled(enabled);
    }

    private Layout(Composite parent) {
      Composite panel = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      panel.setLayout(gridLayout);

      Composite comp = new Composite(panel, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;

      GridData gridData = new GridData();
      gridData.widthHint = WIDTH_HINT;
      gridData.horizontalAlignment = GridData.CENTER;
      gridData.grabExcessHorizontalSpace = true;
      gridData.grabExcessVerticalSpace = true;
      gridData.verticalAlignment = GridData.BEGINNING;
      comp.setLayout(gridLayout);
      comp.setLayoutData(gridData);

      createServersGroup(comp);
      new Label(comp, SWT.NONE); // filler
      createServerGroup(comp);
    }

    private void createServersGroup(Composite parent) {
      Group serversGroup = new Group(parent, SWT.BORDER);
      serversGroup.setText(SERVERS);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      serversGroup.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL);
      gridData.heightHint = HEIGHT_HINT;
      serversGroup.setLayoutData(gridData);

      Composite comp = new Composite(serversGroup, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      gridLayout.makeColumnsEqualWidth = false;
      comp.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 3;
      comp.setLayoutData(gridData);

      Composite sidePanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      sidePanel.setLayout(gridLayout);
      sidePanel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Composite tablePanel = new Composite(sidePanel, SWT.BORDER);
      tablePanel.setLayout(new FillLayout());
      tablePanel.setLayoutData(new GridData(GridData.FILL_BOTH));
      m_serverTable = new Table(tablePanel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
      m_serverTable.setHeaderVisible(true);
      m_serverTable.setLinesVisible(true);
      SWTUtil.makeTableColumnsResizeEqualWidth(tablePanel, m_serverTable);

      TableColumn nameCol = new TableColumn(m_serverTable, SWT.NONE, NAME_INDEX);
      nameCol.setResizable(true);
      nameCol.setText(NAME);
      nameCol.pack();

      TableColumn hostCol = new TableColumn(m_serverTable, SWT.NONE, HOST_INDEX);
      hostCol.setResizable(true);
      hostCol.setText(HOST);
      hostCol.pack();

      TableColumn dsoPortCol = new TableColumn(m_serverTable, SWT.NONE, DSO_PORT_INDEX);
      dsoPortCol.setResizable(true);
      dsoPortCol.setText(DSO_PORT);
      dsoPortCol.pack();

      TableColumn jmxPortCol = new TableColumn(m_serverTable, SWT.NONE, JMX_PORT_INDEX);
      jmxPortCol.setResizable(true);
      jmxPortCol.setText(JMX_PORT);
      jmxPortCol.pack();

      Composite buttonPanel = new Composite(comp, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 1;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      buttonPanel.setLayout(gridLayout);
      buttonPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

      m_addServerButton = new Button(buttonPanel, SWT.PUSH);
      m_addServerButton.setText(ADD);
      m_addServerButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
      SWTUtil.applyDefaultButtonSize(m_addServerButton);

      m_removeServerButton = new Button(buttonPanel, SWT.PUSH);
      m_removeServerButton.setText(REMOVE);
      m_removeServerButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
      SWTUtil.applyDefaultButtonSize(m_removeServerButton);
    }

    private void createServerGroup(Composite parent) {
      m_serverGroup = new Group(parent, SWT.BORDER);
      m_serverGroup.setText(SERVER);
      GridLayout gridLayout = new GridLayout();
      gridLayout.numColumns = 5;
      gridLayout.marginWidth = 10;
      gridLayout.marginHeight = 10;
      m_serverGroup.setLayout(gridLayout);
      GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
      m_serverGroup.setLayoutData(gridData);

      Label nameLabel = new Label(m_serverGroup, SWT.NONE);
      nameLabel.setText(NAME);
      m_nameField = new Text(m_serverGroup, SWT.BORDER);
      m_nameField.setEnabled(false);
      m_nameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      new Label(m_serverGroup, SWT.NONE); // space

      Label dsoPortLabel = new Label(m_serverGroup, SWT.NONE);
      dsoPortLabel.setText(DSO_PORT);
      m_dsoPortField = new Text(m_serverGroup, SWT.BORDER);
      SWTUtil.makeIntField(m_dsoPortField);
      m_dsoPortField.setEnabled(false);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.widthHint = SWTUtil.textColumnsToPixels(m_dsoPortField, 6);
      m_dsoPortField.setLayoutData(gridData);

      Label hostLabel = new Label(m_serverGroup, SWT.NONE);
      hostLabel.setText(HOST);
      m_hostField = new Text(m_serverGroup, SWT.BORDER);
      m_hostField.setEnabled(false);
      m_hostField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      new Label(m_serverGroup, SWT.NONE); // space

      Label jmxPortLabel = new Label(m_serverGroup, SWT.NONE);
      jmxPortLabel.setText(JMX_PORT);
      m_jmxPortField = new Text(m_serverGroup, SWT.BORDER);
      SWTUtil.makeIntField(m_jmxPortField);
      m_jmxPortField.setEnabled(false);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.widthHint = SWTUtil.textColumnsToPixels(m_jmxPortField, 6);
      m_jmxPortField.setLayoutData(gridData);

      Label dataLabel = new Label(m_serverGroup, SWT.NONE);
      dataLabel.setText(DATA);

      Composite dataPanel = new Composite(m_serverGroup, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      dataPanel.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 4;
      dataPanel.setLayoutData(gridData);

      m_dataLocation = new Text(dataPanel, SWT.BORDER);
      m_dataLocation.setEnabled(false);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      m_dataLocation.setLayoutData(gridData);

      m_dataBrowse = new Button(dataPanel, SWT.PUSH);
      m_dataBrowse.setText(BROWSE);
      m_dataBrowse.setEnabled(false);
      SWTUtil.applyDefaultButtonSize(m_dataBrowse);

      Label logsLabel = new Label(m_serverGroup, SWT.NONE);
      logsLabel.setText(LOGS);

      Composite logsPanel = new Composite(m_serverGroup, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 2;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      logsPanel.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 4;
      logsPanel.setLayoutData(gridData);

      m_logsLocation = new Text(logsPanel, SWT.BORDER);
      m_logsLocation.setEnabled(false);
      gridData = new GridData(GridData.FILL_HORIZONTAL);
      m_logsLocation.setLayoutData(gridData);

      m_logsBrowse = new Button(logsPanel, SWT.PUSH);
      m_logsBrowse.setText(BROWSE);
      m_logsBrowse.setEnabled(false);
      SWTUtil.applyDefaultButtonSize(m_logsBrowse);

      Composite gcPanel = new Composite(m_serverGroup, SWT.NONE);
      gridLayout = new GridLayout();
      gridLayout.numColumns = 3;
      gridLayout.marginWidth = 0;
      gridLayout.marginHeight = 0;
      gcPanel.setLayout(gridLayout);
      gridData = new GridData(GridData.FILL_BOTH);
      gridData.horizontalSpan = 5;
      gcPanel.setLayoutData(gridData);

      Label persistenceModeLabel = new Label(gcPanel, SWT.NONE);
      persistenceModeLabel.setText(PERSISTENCE_MODE);

      m_persistenceModeCombo = new Combo(gcPanel, SWT.BORDER | SWT.READ_ONLY);
      m_persistenceModeCombo.setEnabled(false);
      gridData = new GridData(GridData.GRAB_HORIZONTAL);
      gridData.widthHint = 200;
      m_persistenceModeCombo.setLayoutData(gridData);

      m_gcCheck = new Button(gcPanel, SWT.CHECK);
      m_gcCheck.setEnabled(false);
      m_gcCheck.setText(GARBAGE_COLLECTION);

      Label gcIntervalLabel = new Label(gcPanel, SWT.NONE);
      gcIntervalLabel.setText(GC_INTERVAL);

      m_gcIntervalSpinner = new Spinner(gcPanel, SWT.BORDER);
      m_gcIntervalSpinner.setEnabled(false);
      gridData = new GridData(GridData.FILL_VERTICAL | GridData.GRAB_HORIZONTAL);
      gridData.widthHint = 100;
      m_gcIntervalSpinner.setLayoutData(gridData);

      m_verboseCheck = new Button(gcPanel, SWT.CHECK);
      m_verboseCheck.setEnabled(false);
      m_verboseCheck.setText(VERBOSE);
    }
  }
}
