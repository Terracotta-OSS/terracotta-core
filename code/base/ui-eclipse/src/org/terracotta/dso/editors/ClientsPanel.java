/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.DictionaryResource;
import org.dijon.Label;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.chooser.ProjectFolderNavigator;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.dso.editors.xmlbeans.XmlStringField;

import com.terracottatech.config.Client;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ClientsPanel extends ConfigurationEditorPanel implements ActionListener, ConfigurationEditorRoot,
    XmlObjectStructureListener {
  private static ContainerResource m_res;
  private IProject                 m_project;
  private TcConfig                 m_config;
  private Client                   m_client;
  private XmlStringField           m_logs;
  private Label                    m_logsLabel;
  private Button                   m_logsButton;
  private ProjectFolderNavigator   m_folderNavigator;
  private DsoClientDataPanel       m_dsoClientData;
  private ModulesPanel             m_modulesPanel;

  static {
    TcPlugin plugin = TcPlugin.getDefault();
    DictionaryResource topRes = plugin.getResources();

    m_res = (ContainerResource) topRes.find("ClientsPanel");
  }

  public ClientsPanel() {
    super();
    if (m_res != null) {
      load(m_res);
    }
  }

  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_modulesPanel = (ModulesPanel) findComponent("ModulesPanel");

    m_logs = (XmlStringField) findComponent("Logs");
    m_logs.init(Client.class, "logs");

    m_logsLabel = (Label) findComponent("LogsLabel");
    m_logsLabel.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() == 1) {
          m_logs.unset();
        }
      }
    });

    m_logsButton = (Button) findComponent("LogsButton");
    m_logsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        Frame frame = (Frame) getAncestorOfClass(Frame.class);

        if (m_folderNavigator == null) {
          m_folderNavigator = new ProjectFolderNavigator(frame);
        }
        m_folderNavigator.init(m_project);
        m_folderNavigator.setActionListener(new LogsNavigatorListener());
        m_folderNavigator.center(frame);
        m_folderNavigator.setVisible(true);
      }
    });

    m_dsoClientData = (DsoClientDataPanel) findComponent("DsoClientData");
  }

  class LogsNavigatorListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      IFolder folder = m_folderNavigator.getSelectedFolder();

      if (folder != null) {
        m_logs.setText(folder.getProjectRelativePath().toString());
        m_logs.set();
      }
    }
  }

  public boolean hasAnySet() {
    return m_logs.isSet() || m_dsoClientData.hasAnySet();
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();

    if (m_client == null) {
      removeListeners();
      m_client = m_config.addNewClients();
      updateChildren();
      addListeners();
    }
  }

  private void syncModel() {
    if (!hasAnySet() && m_config.getClients() != null) {
      m_config.unsetClients();
      m_client = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    } else {
      setDirty();
    }
  }

  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_config);
  }

  public void actionPerformed(ActionEvent ae) {
    setDirty();
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    syncModel();
  }

  private void addListeners() {
    m_logs.addActionListener(this);
    m_logs.addXmlObjectStructureListener(this);

    m_dsoClientData.addXmlObjectStructureListener(this);
  }

  private void removeListeners() {
    m_logs.removeActionListener(this);
    m_logs.removeXmlObjectStructureListener(this);

    m_dsoClientData.removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    m_logs.setup(m_client);
    m_dsoClientData.setup(m_client);
  }

  public void setup(IProject project) {
    TcPlugin plugin = TcPlugin.getDefault();

    removeListeners();
    setEnabled(true);

    m_project = project;
    m_config = plugin.getConfiguration(project);
    m_client = m_config != null ? m_config.getClients() : null;
    m_modulesPanel.setup(m_client);
    m_modulesPanel.addChangeListener(new ModulesPanel.ModulesEventListener() {
      public void handleEvent() {
        fireXmlObjectStructureChanged();
      }
    });
    m_modulesPanel.addSetDirtyListener(new ModulesPanel.ModulesEventListener() {
      public void handleEvent() {
        setDirty();
      }
    });

    updateChildren();
    addListeners();
  }

  public IProject getProject() {
    return m_project;
  }

  public void tearDown() {
    removeListeners();

    m_logs.tearDown();
    m_dsoClientData.tearDown();

    setEnabled(false);
  }
}
