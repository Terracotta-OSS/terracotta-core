/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.wizards;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;

import org.dijon.Button;
import org.dijon.Container;
import org.dijon.DictionaryResource;
import org.dijon.TextField;

import org.terracotta.dso.TcPlugin;

/**
 * This is not currently being used but if we decide the plugin needs to support
 * an external release package, as opposed to the release BEING the plugin, we
 * would need this.
 */

public class InstallWizardPage extends WizardPage {
  private Frame     m_frame;
  private TextField m_installDirField;
  private Button    m_browseButton;
  
  private static final String PAGE_NAME  = "TerracottaInstallDirectoryWizardPage";
  private static final String PAGE_TITLE = "Specify Terracotta Installation Directory";
  private static final String PAGE_DESC  = "Please specify the location of your Terracotta installation.";
  
  public InstallWizardPage() {
    super(PAGE_NAME);
    
    setTitle(PAGE_TITLE);
    setDescription(PAGE_DESC);
  }

  public void createControl(Composite parent) {
    DictionaryResource topRes    = TcPlugin.getDefault().getResources();
    Container          panel     = (Container)topRes.resolve("PropertyPanel");
    Composite          composite = new Composite(parent, SWT.EMBEDDED);

    m_frame = SWT_AWT.new_Frame(composite);
    m_frame.add(panel);
    
    m_installDirField = (TextField)panel.findComponent("TCInstallDirField");
    m_installDirField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        final File file = new File(m_installDirField.getText());
        
        getShell().getDisplay().asyncExec(new Runnable() {
          public void run() {
            String err = null;
            
            if(!file.exists()) {
              err = "Directory does not exist.";
            }

            setErrorMessage(err);
          
            getContainer().updateMessage();
            getContainer().updateButtons();
          }
        });
      }
    });
    
    m_browseButton = (Button)panel.findComponent("BrowseButton");
    m_browseButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        String       title   = "Browse for Terracotta installation";
        JFileChooser chooser = new JFileChooser();
        
        chooser.setDialogTitle(title);
        chooser.setCurrentDirectory(new File(m_installDirField.getText()));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int returnVal = chooser.showOpenDialog(m_frame);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
          File f = chooser.getSelectedFile();
          
          m_installDirField.setText(f.getAbsolutePath());
        }
      
        getShell().getDisplay().asyncExec(new Runnable() {
          public void run() {
            getContainer().updateMessage();
            getContainer().updateButtons();
          }
        });
      }
    });
    
    setControl(composite);
  }
  
  public boolean isPageComplete() {
    File file = new File(m_installDirField.getText());
    return file.exists() && file.isDirectory();
  }
  
  public boolean canFinish() {
    return isPageComplete();
  }
  
  public String getInstallDir() {
    return m_installDirField.getText();
  }
}
