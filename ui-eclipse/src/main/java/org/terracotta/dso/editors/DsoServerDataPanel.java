/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.terracotta.dso.editors.xmlbeans.XmlIntegerSpinner;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.ui.util.SWTLayout;
import org.terracotta.ui.util.SWTUtil;

import com.terracottatech.config.DsoServerData;
import com.terracottatech.config.Server;

public class DsoServerDataPanel extends ConfigurationEditorPanel
  implements XmlObjectStructureListener
{
  private Server        m_server;
  private DsoServerData m_dsoServerData;
  private Layout        m_layout;

  public DsoServerDataPanel(Composite parent, int style) {
    super(parent, style);
    m_layout = new Layout(this);
    SWTUtil.setBGColorRecurse(this.getDisplay().getSystemColor(SWT.COLOR_WHITE), this);
  }

  public void ensureXmlObject() {
    super.ensureXmlObject();

    if(m_dsoServerData == null) {
      removeListeners();
      m_dsoServerData = m_server.addNewDso();
      updateChildren();
      addListeners();
    }
  }
  
  public boolean hasAnySet() {
    return m_dsoServerData != null &&
          (m_dsoServerData.isSetGarbageCollection() ||
           m_dsoServerData.isSetPersistence() ||
           m_dsoServerData.isSetClientReconnectWindow());
  }
  
  public void structureChanged(XmlObjectStructureChangeEvent e) {
    testUnsetDsoServerData();
  }
  
  private void testUnsetDsoServerData() {
    if(!hasAnySet() && m_server.getDso() != null) {
      m_server.unsetDso();
      m_dsoServerData = null;
      fireXmlObjectStructureChanged();
      updateChildren();
    }
    fireServerChanged();
  }

  void fireServerChanged() {
    ServersPanel serversPanel = (ServersPanel)SWTUtil.getAncestorOfClass(ServersPanel.class, this);
    serversPanel.fireServerChanged(m_server);
  }
  
  private void fireXmlObjectStructureChanged() {
    fireXmlObjectStructureChanged(m_server);    
  }
  
  private void addListeners() {
    ((XmlIntegerSpinner)m_layout.m_clientReconnectWindowSpinner.getData()).addXmlObjectStructureListener(this);
    m_layout.m_garbageCollectionPanel.addXmlObjectStructureListener(this);
    m_layout.m_persistencePanel.addXmlObjectStructureListener(this);
  }
  
  private void removeListeners() {
    ((XmlIntegerSpinner)m_layout.m_clientReconnectWindowSpinner.getData()).removeXmlObjectStructureListener(this);
    m_layout.m_garbageCollectionPanel.removeXmlObjectStructureListener(this);
    m_layout.m_persistencePanel.removeXmlObjectStructureListener(this);
  }

  private void updateChildren() {
    m_layout.setDsoServerData(m_dsoServerData);
  }

  public void setup(Server server) {
    setEnabled(true);
    removeListeners();

    m_server        = server;
    m_dsoServerData = m_server != null ? m_server.getDso() : null;
    
    updateChildren();
    addListeners();
  }
  
  public void tearDown() {
    removeListeners();

    m_server        = null;
    m_dsoServerData = null;
    
    m_layout.tearDown();
    
    setEnabled(false);
  }

  private class Layout implements SWTLayout {
    private static final String    CLIENT_RECONNECT = "Client re-connect";
    private static final String    CLIENT_RECONNECT_WINDOW = "Window (sec.)";

    private Spinner                m_clientReconnectWindowSpinner;
    private GarbageCollectionPanel m_garbageCollectionPanel;
    private PersistencePanel       m_persistencePanel;

    void tearDown() {
      ((XmlIntegerSpinner)m_clientReconnectWindowSpinner.getData()).tearDown();
      m_garbageCollectionPanel.tearDown();
      m_persistencePanel.tearDown();
    }
    
    public void reset() {
      setDsoServerData(null);
    }
    
    private void setDsoServerData(DsoServerData dsoServerData) {
      ((XmlIntegerSpinner)m_clientReconnectWindowSpinner.getData()).setup(dsoServerData);
      m_garbageCollectionPanel.setup(dsoServerData);
      m_persistencePanel.setup(dsoServerData);
    }

    private Layout(Composite parent) {
      parent.setLayout(new GridLayout());
      
      Group clientReconnectGroup = new Group(parent, SWT.NONE);
      clientReconnectGroup.setText(CLIENT_RECONNECT);
      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 10;
      clientReconnectGroup.setLayout(gridLayout);
      
      Label label = new Label(clientReconnectGroup, SWT.NONE);
      label.setText(CLIENT_RECONNECT_WINDOW);
      
      m_clientReconnectWindowSpinner = new Spinner(clientReconnectGroup, SWT.BORDER);
      initIntegerSpinnerField(m_clientReconnectWindowSpinner, DsoServerData.class, "client-reconnect-window");
      clientReconnectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      
      m_garbageCollectionPanel = new GarbageCollectionPanel(parent, SWT.NONE);
      m_garbageCollectionPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      m_persistencePanel = new PersistencePanel(parent, SWT.NONE);
      m_persistencePanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }
  }
}
