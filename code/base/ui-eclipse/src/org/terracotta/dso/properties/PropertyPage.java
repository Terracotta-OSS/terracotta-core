/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.properties;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.chooser.ProjectFileNavigator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;

public final class PropertyPage extends org.eclipse.ui.dialogs.PropertyPage {
  private final Display        m_display;
  private Text                 m_configPathField;
  private Button               m_configFileButton;
  private ProjectFileNavigator m_fileNavigator;
  private Text                 m_serverOptionsField;
  private Button               m_resetOptionsButton;

  private static final String  DEFAULT_CONFIG_FILENAME = TcPlugin.DEFAULT_CONFIG_FILENAME;
  private static final String  DEFAULT_SERVER_OPTIONS  = TcPlugin.DEFAULT_SERVER_OPTIONS;

  public PropertyPage() {
    super();
    m_display = Display.getCurrent();
  }

  private void fillControls() {
    TcPlugin plugin = TcPlugin.getDefault();
    IProject project = getProject();
    m_configPathField.setText(plugin.getConfigurationFilePath(project));
    m_serverOptionsField.setText(plugin.getServerOptions(project));
  }

  protected Control createContents(Composite parent) {
    final Composite topComp = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 1;
    topComp.setLayout(gridLayout);
    topComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    GridData gridData = new GridData();
    gridData.grabExcessHorizontalSpace = true;
    gridData.horizontalAlignment = GridData.FILL;

    Group domainConfig = new Group(topComp, SWT.SHADOW_ETCHED_IN);
    domainConfig.setText("Domain Configuration");
    gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    domainConfig.setLayout(gridLayout);
    domainConfig.setLayoutData(gridData);

    m_configPathField = new Text(domainConfig, SWT.SINGLE|SWT.BORDER);
    m_configPathField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    m_configFileButton = new Button(domainConfig, SWT.PUSH);
    m_configFileButton.setText("  Browse...  ");
    m_configFileButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    m_configFileButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        if (m_fileNavigator == null) {
          m_fileNavigator = new ProjectFileNavigator(null, "xml");
          m_fileNavigator.setActionListener(new NavigatorListener());
        }
        m_fileNavigator.init(getProject());
        m_fileNavigator.center();
        m_display.asyncExec(new Runnable() {
          public void run() {
            m_fileNavigator.setVisible(true);
            m_fileNavigator.toFront();
            m_fileNavigator.setAlwaysOnTop(true);
          }
        });
      }
    });

    Group serverOptions = new Group(topComp, SWT.SHADOW_ETCHED_IN);
    serverOptions.setText("Server Options");
    gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    serverOptions.setLayout(gridLayout);
    serverOptions.setLayoutData(gridData);

    m_serverOptionsField = new Text(serverOptions, SWT.MULTI|SWT.BORDER|SWT.WRAP);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL|GridData.FILL_VERTICAL);
    gd.minimumHeight = 60;
    m_serverOptionsField.setLayoutData(gd);

    m_resetOptionsButton = new Button(serverOptions, SWT.PUSH);
    m_resetOptionsButton.setText("  Reset  ");
    m_resetOptionsButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END|GridData.VERTICAL_ALIGN_BEGINNING));
    m_resetOptionsButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        m_serverOptionsField.setText(DEFAULT_SERVER_OPTIONS);
      }
    });

    Composite composite = new Composite(topComp, SWT.EMBEDDED);
    composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
    fillControls();
    return topComp;
  }

  class NavigatorListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          IResource member = m_fileNavigator.getSelectedMember();
          if (member != null) {
            if (member instanceof IFolder) {
              member = ((IFolder) member).getFile(TcPlugin.DEFAULT_CONFIG_FILENAME);
            }
            final IResource finalMember = member;
            m_display.asyncExec(new Runnable() {
              public void run() {
                m_configPathField.setText(finalMember.getProjectRelativePath().toString());
              }
            });
          }
        }
      });
    }
  }

  protected void performDefaults() {
    m_configPathField.setText(DEFAULT_CONFIG_FILENAME);
    m_serverOptionsField.setText(DEFAULT_SERVER_OPTIONS);
  }

  private IProject getProject() {
    return ((IJavaProject) getElement()).getProject();
  }

  private void updateProject() {
    TcPlugin plugin = TcPlugin.getDefault();
    IProject project = getProject();
    plugin.setup(project, m_configPathField.getText(), m_serverOptionsField.getText());
  }

  public boolean performOk() {
    updateProject();
    return true;
  }
}
