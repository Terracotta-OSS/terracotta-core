/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import org.dijon.Button;
import org.dijon.ContainerResource;
import org.dijon.Label;

import org.terracotta.dso.editors.chooser.ProjectFolderNavigator;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.dso.editors.xmlbeans.XmlStringField;
import com.terracottatech.config.Server;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ServerPanel extends ConfigurationEditorPanel
  implements ActionListener,
             XmlObjectStructureListener
{
  private Server                 m_server;
  private XmlStringField         m_data;
  private Label                  m_dataLabel;
  private Button                 m_dataButton;
  private XmlStringField         m_logs;
  private Label                  m_logsLabel;
  private Button                 m_logsButton;
  private DsoServerDataPanel     m_dsoServerDataPanel;
  private ProjectFolderNavigator m_folderNavigator;
  
  public ServerPanel() {
    super();
  }

  public void load(ContainerResource containerRes) {
    super.load(containerRes);

    m_data = (XmlStringField)findComponent("Data");
    m_data.init(Server.class, "data");
    m_dataLabel = (Label)findComponent("DataLabel");
    m_dataLabel.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if(me.getClickCount() == 1) {
          m_data.unset();
        }
      }
    });
    m_dataButton = (Button)findComponent("DataButton");
    m_dataButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        Frame        frame        = (Frame)getAncestorOfClass(Frame.class);
        ServersPanel serversPanel = (ServersPanel)getAncestorOfClass(ServersPanel.class);
        IProject     project      = serversPanel.getProject();

        if(m_folderNavigator == null) {
          m_folderNavigator = new ProjectFolderNavigator(frame);
        }
        m_folderNavigator.init(project);
        m_folderNavigator.setActionListener(new DataNavigatorListener());
        m_folderNavigator.center(frame);
        m_folderNavigator.setVisible(true);
      }
    });

    m_logs = (XmlStringField)findComponent("Logs");
    m_logs.init(Server.class, "logs");
    m_logsLabel = (Label)findComponent("LogsLabel");
    m_logsLabel.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if(me.getClickCount() == 1) {
          m_logs.unset();
        }
      }
    });
    m_logsButton = (Button)findComponent("LogsButton");
    m_logsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        Frame        frame        = (Frame)getAncestorOfClass(Frame.class);
        ServersPanel serversPanel = (ServersPanel)getAncestorOfClass(ServersPanel.class);
        IProject     project      = serversPanel.getProject();

        if(m_folderNavigator == null) {
          m_folderNavigator = new ProjectFolderNavigator(frame);
        }
        m_folderNavigator.init(project);
        m_folderNavigator.setActionListener(new LogsNavigatorListener());
        m_folderNavigator.center(frame);
        m_folderNavigator.setVisible(true);
      }
    });

    m_dsoServerDataPanel = (DsoServerDataPanel)findComponent("DsoServerData");
  }

  class DataNavigatorListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      IFolder folder = m_folderNavigator.getSelectedFolder();

      if(folder != null) {
        m_data.setText(folder.getProjectRelativePath().toString());
        m_data.set();
      }
    }
  }
  
  class LogsNavigatorListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      IFolder folder = m_folderNavigator.getSelectedFolder();

      if(folder != null) {
        m_logs.setText(folder.getProjectRelativePath().toString());
        m_logs.set();
      }
    }
  }

  public void structureChanged(XmlObjectStructureChangeEvent e) {
    syncModel();
  }

  public void actionPerformed(ActionEvent ae) {
    setDirty();
  }

  private void syncModel() {
    // One of our children has been added/removed,
    // but we're required because of host.

    setDirty();
  }

  public boolean hasAnySet() {
    return m_server != null &&
          (m_server.isSetData()    ||
           m_server.isSetLogs()    ||
           m_server.isSetDsoPort() ||
           m_server.isSetDso());
  }

  private void addListeners() {
    m_data.addActionListener(this);
    m_data.addXmlObjectStructureListener(this);

    m_logs.addActionListener(this);
    m_logs.addXmlObjectStructureListener(this);

    m_dsoServerDataPanel.addXmlObjectStructureListener(this);
  }

  private void removeListeners() {
    m_data.removeActionListener(this);
    m_data.removeXmlObjectStructureListener(this);

    m_logs.removeActionListener(this);
    m_logs.removeXmlObjectStructureListener(this);

    m_dsoServerDataPanel.removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    m_data.setup(m_server);
    m_logs.setup(m_server);
    m_dsoServerDataPanel.setup(m_server);
  }
  
  public void setup(Server server) {
    setEnabled(true);
    removeListeners();

    m_server = server;

    updateChildren();
    addListeners();
  }

  public void tearDown() {
    removeListeners();

    m_server = null;

    m_data.tearDown();
    m_logs.tearDown();
    m_dsoServerDataPanel.tearDown();

    setEnabled(false);
  }
}
