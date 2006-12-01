/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.wizards;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;

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

public class SetupWizardPage extends WizardPage {
  private IJavaProject         m_javaProject;
  private Frame                m_frame;
  private TextField            m_configFileField;
  private Button               m_configFileButton;
  private ProjectFileNavigator m_fileNavigator;
  private TextField            m_serverOptionsField;
  private Button               m_resetOptionsButton;

  private static final String
    PAGE_NAME = "TCSetupWizardPage";

  private static final String
    PAGE_TITLE = "Terracotta Project Setup";

  private static final String
    PAGE_DESC  = "Specify the location of your Terracotta domain configuration file and\n" +
                 "Terracotta server Java runtime options";

  private static final String DEFAULT_CONFIG_FILENAME = TcPlugin.DEFAULT_CONFIG_FILENAME;
  private static final String DEFAULT_SERVER_OPTIONS  = TcPlugin.DEFAULT_SERVER_OPTIONS;
  
  public SetupWizardPage(IJavaProject project) {
    super(PAGE_NAME);

    m_javaProject = project;

    setTitle(PAGE_TITLE);
    setDescription(PAGE_DESC);
  }

  public void createControl(Composite parent) {
    Composite          composite = new Composite(parent, SWT.EMBEDDED);
    Frame              frame     = SWT_AWT.new_Frame(composite);
    JRootPane          rootPane  = new JRootPane();
    RootPanel          root      = new RootPanel();
    DictionaryResource topRes    = TcPlugin.getDefault().getResources();
    Container          panel     = (Container)topRes.resolve("PropertyPanel");

    frame.add(root);
    root.add(rootPane);
    rootPane.getContentPane().add(panel);
    
    m_configFileField = (TextField)panel.findComponent("ConfigFileField");
    m_configFileField.setText(DEFAULT_CONFIG_FILENAME);

    m_configFileButton = (Button)panel.findComponent("ConfigFileButton");
    m_configFileButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        if(m_fileNavigator == null) {
          m_fileNavigator = new ProjectFileNavigator(m_frame, "xml");
          m_fileNavigator.setActionListener(new NavigatorListener());
        }
        m_fileNavigator.init(m_javaProject.getProject());
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

    setControl(composite);
  }

  class NavigatorListener implements ActionListener {
    public void actionPerformed(ActionEvent ae) {
      IResource member = m_fileNavigator.getSelectedMember();

      if(member != null) {
        if(member instanceof IFolder) {
          member = ((IFolder)member).getFile(DEFAULT_CONFIG_FILENAME);
        }
        m_configFileField.setText(member.getProjectRelativePath().toString());
      }
    }
  } 
  
  public boolean isPageComplete() {
    return true;
  }

  public boolean canFinish() {
    return isPageComplete();
  }

  public String getDomainConfigurationPath() {
    return m_configFileField.getText();
  }
  
  public String getServerOptions() {
    return m_serverOptionsField.getText();
  }
}
