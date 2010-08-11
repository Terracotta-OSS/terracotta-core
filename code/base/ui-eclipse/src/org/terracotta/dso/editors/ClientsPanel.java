/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.dso.editors.xmlbeans.XmlStringField;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.Client;
import com.terracottatech.config.TcConfigDocument.TcConfig;

public class ClientsPanel extends ConfigurationEditorPanel implements ConfigurationEditorRoot,
    XmlObjectStructureListener {
  private IProject                         m_project;
  private TcConfig                         m_config;
  private Client                           m_client;
  private final Layout                     m_layout;
  private final LogsBrowseSelectionHandler m_logsBrowseSelectionHandler;

  public ClientsPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    m_logsBrowseSelectionHandler = new LogsBrowseSelectionHandler();
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  public boolean hasAnySet() {
    return m_client.isSetDso() || m_client.isSetLogs() || m_layout.m_modulesPanel.hasAnySet();
  }

  @Override
  public void ensureXmlObject() {
    super.ensureXmlObject();

    if (m_client == null) {
      removeListeners();
      m_client = m_config.addNewClients();
      updateChildren();
      addListeners();
    }
  }

  private void testUnsetClients() {
    if (!hasAnySet() && m_config.getClients() != null) {
      m_config.unsetClients();
      m_client = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    }
    fireClientChanged();
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_config);
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    testUnsetClients();
  }

  private void addListeners() {
    m_layout.m_logsBrowse.addSelectionListener(m_logsBrowseSelectionHandler);
    ((XmlStringField) m_layout.m_logsLocation.getData()).addXmlObjectStructureListener(this);

    m_layout.m_dsoClientDataPanel.addXmlObjectStructureListener(this);
    m_layout.m_modulesPanel.addXmlObjectStructureListener(this);

  }

  private void removeListeners() {
    m_layout.m_logsBrowse.removeSelectionListener(m_logsBrowseSelectionHandler);
    ((XmlStringField) m_layout.m_logsLocation.getData()).removeXmlObjectStructureListener(this);

    m_layout.m_dsoClientDataPanel.removeXmlObjectStructureListener(this);
    m_layout.m_modulesPanel.removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    m_layout.setup(m_client);
  }

  public void setup(IProject project) {
    TcPlugin plugin = TcPlugin.getDefault();

    removeListeners();
    setEnabled(true);

    m_project = project;
    m_config = plugin.getConfiguration(project);
    m_client = m_config != null ? m_config.getClients() : null;

    updateChildren();
    addListeners();
  }

  @Override
  public IProject getProject() {
    return m_project;
  }

  public void tearDown() {
    removeListeners();
    m_layout.tearDown();
    setEnabled(false);
  }

  private class Layout {
    private static final String      LOGS   = "Logs";
    private static final String      BROWSE = "Browse...";

    private final Button             m_logsBrowse;
    private final Text               m_logsLocation;

    private final DsoClientDataPanel m_dsoClientDataPanel;
    private final ModulesPanel       m_modulesPanel;

    void setup(Client client) {
      ((XmlStringField) m_logsLocation.getData()).setup(client);
      m_dsoClientDataPanel.setup(client);
      m_modulesPanel.setup(client);
    }

    void tearDown() {
      ((XmlStringField) m_logsLocation.getData()).tearDown();
      m_dsoClientDataPanel.tearDown();
      m_modulesPanel.tearDown();
    }

    private Layout(Composite parent) {
      parent.setLayout(new GridLayout());

      Composite panel = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.marginWidth = gridLayout.marginHeight = 10;
      panel.setLayout(gridLayout);
      panel.setLayoutData(new GridData(GridData.FILL_BOTH));

      Composite comp = new Composite(panel, SWT.NONE);
      gridLayout = new GridLayout(3, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      Label logsLabel = new Label(comp, SWT.NONE);
      logsLabel.setText(LOGS);

      m_logsLocation = new Text(comp, SWT.BORDER);
      m_logsLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      initStringField(m_logsLocation, Client.class, "logs");

      m_logsBrowse = new Button(comp, SWT.PUSH);
      m_logsBrowse.setText(BROWSE);
      SWTUtil.applyDefaultButtonSize(m_logsBrowse);

      m_dsoClientDataPanel = new DsoClientDataPanel(panel, SWT.NONE);
      m_dsoClientDataPanel.setLayout(new GridLayout());
      m_dsoClientDataPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      m_modulesPanel = new ModulesPanel(panel, SWT.NONE);
      m_modulesPanel.setLayout(new GridLayout());
      m_modulesPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
    }
  }

  private class LogsBrowseSelectionHandler extends SelectionAdapter {
    @Override
    public void widgetSelected(SelectionEvent e) {
      IFolder folder = SWTUtil.openSelectFolderDialog(m_project, "Select logs folder",
                                                      "Choose a folder for the log area");
      if (folder != null) {
        setStringField(m_layout.m_logsLocation, folder.getProjectRelativePath().toString());
      }
    }
  }
}
