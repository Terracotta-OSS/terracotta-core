/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.properties;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.dijon.Button;
import org.dijon.Container;
import org.dijon.DictionaryResource;
import org.dijon.TextField;

import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.RootPanel;
import org.terracotta.dso.editors.chooser.ProjectFileNavigator;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRootPane;

public class PropertyPage extends org.eclipse.ui.dialogs.PropertyPage {
  private Frame                m_frame;
  private TextField            m_configPathField;
  private Button               m_configFileButton;
  private ProjectFileNavigator m_fileNavigator;
  private TextField            m_serverOptionsField;
  private Button               m_resetOptionsButton;
  
  private static final String DEFAULT_CONFIG_FILENAME = TcPlugin.DEFAULT_CONFIG_FILENAME;
  private static final String DEFAULT_SERVER_OPTIONS  = TcPlugin.DEFAULT_SERVER_OPTIONS;
    
  public PropertyPage() {
    super();
  }

  private void fillControls() {
    TcPlugin plugin  = TcPlugin.getDefault();
    IProject project = getProject();
    
    m_configPathField.setText(plugin.getConfigurationFilePath(project));
    m_serverOptionsField.setText(plugin.getServerOptions(project));
  }
	
  protected Control createContents(Composite parent) {
    Composite          composite = new Composite(parent, SWT.EMBEDDED);
    Frame              frame     = SWT_AWT.new_Frame(composite);
    DictionaryResource topRes    = TcPlugin.getDefault().getResources();
    JRootPane          rootPane  = new JRootPane();
    RootPanel          root      = new RootPanel();
    Container          panel     = (Container)topRes.resolve("PropertyPanel");

    frame.add(root);
    root.add(rootPane);
    rootPane.getContentPane().add(panel);

    m_configPathField = (TextField)panel.findComponent("ConfigFileField");
    
    m_configFileButton = (Button)panel.findComponent("ConfigFileButton");
    m_configFileButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        if(m_fileNavigator == null) {
          m_fileNavigator = new ProjectFileNavigator(m_frame, "xml");
          m_fileNavigator.setActionListener(new NavigatorListener());
        }
        m_fileNavigator.init(getProject());
        m_fileNavigator.center();
        m_fileNavigator.setVisible(true);
      }
    });
    
    m_serverOptionsField = (TextField)panel.findComponent("ServerOptionsField");
    m_resetOptionsButton = (Button)panel.findComponent("ResetOptionsButton");
    m_resetOptionsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        m_serverOptionsField.setText(DEFAULT_SERVER_OPTIONS);
      }
    });
    
    fillControls();

    return composite;
  }

  class NavigatorListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      IResource member = m_fileNavigator.getSelectedMember();
      
      if(member != null) {
        if(member instanceof IFolder) {
          member = ((IFolder)member).getFile(TcPlugin.DEFAULT_CONFIG_FILENAME);
        }
        m_configPathField.setText(member.getProjectRelativePath().toString());
      }
    }
  }  
  
  protected void performDefaults() {
    m_configPathField.setText(DEFAULT_CONFIG_FILENAME);
    m_serverOptionsField.setText(DEFAULT_SERVER_OPTIONS);
  }
	
  private IProject getProject() {
    return ((IJavaProject)getElement()).getProject();
  }
  
  private void updateProject() {
    TcPlugin plugin  = TcPlugin.getDefault();
    IProject project = getProject();
    
    plugin.setConfigurationFilePath(project, m_configPathField.getText());
    plugin.setServerOptions(project, m_serverOptionsField.getText());
  }
  
  public boolean performOk() {
    updateProject();
    return true;
  }
}
