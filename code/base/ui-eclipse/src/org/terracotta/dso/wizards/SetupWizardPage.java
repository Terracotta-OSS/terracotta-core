/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.chooser.ConfigFileBehavior;
import org.terracotta.dso.editors.chooser.NavigatorBehavior;
import org.terracotta.dso.editors.chooser.PackageNavigator;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;

public class SetupWizardPage extends WizardPage {
  private IProject            m_project;
  private Text                m_configPathField;
  private Button              m_configFileButton;
  private Text                m_serverOptionsField;
  private Button              m_resetOptionsButton;

  private static final String TERRACOTTA_CONFIG       = "Terracotta Configuration";
  private static final String BROWSE                  = "Browse...";
  private static final String RESET                   = "Reset";
  private static final String SERVER_OPTIONS          = "Server Options";
  private static final String PAGE_NAME               = "TCSetupWizardPage";
  private static final String PAGE_TITLE              = "Terracotta Project Setup";
  private static final String PAGE_DESC               = "Specify the location of your Terracotta configuration file and\n"
                                                          + "Terracotta server instance Java runtime options";

  private static final String DEFAULT_CONFIG_FILENAME = TcPlugin.DEFAULT_CONFIG_FILENAME;
  private static final String DEFAULT_SERVER_OPTIONS  = TcPlugin.DEFAULT_SERVER_OPTIONS;

  public SetupWizardPage(IJavaProject project) {
    super(PAGE_NAME);

    m_project = project.getProject();

    setTitle(PAGE_TITLE);
    setDescription(PAGE_DESC);
  }

  public void createControl(Composite parent) {
    final Composite topComp = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 1;
    gridLayout.makeColumnsEqualWidth = false;
    topComp.setLayout(gridLayout);
    topComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Group domainConfig = new Group(topComp, SWT.SHADOW_ETCHED_IN);
    domainConfig.setText(TERRACOTTA_CONFIG);
    gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    domainConfig.setLayout(gridLayout);
    domainConfig.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    m_configPathField = new Text(domainConfig, SWT.SINGLE | SWT.BORDER);
    m_configPathField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    m_configPathField.setText(DEFAULT_CONFIG_FILENAME);

    m_configFileButton = new Button(domainConfig, SWT.PUSH);
    m_configFileButton.setText(BROWSE);
    m_configFileButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    SWTUtil.applyDefaultButtonSize(m_configFileButton);
    m_configFileButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        NavigatorBehavior behavior = new ConfigFileBehavior();
        PackageNavigator dialog = new PackageNavigator(getShell(), behavior.getTitle(), m_project, behavior);
        dialog.addValueListener(new UpdateEventListener() {
          public void handleUpdate(UpdateEvent event) {
            m_configPathField.setText((String) event.data);
          }
        });
        dialog.open();
      }
    });
    Group serverOptions = new Group(topComp, SWT.SHADOW_ETCHED_IN);
    serverOptions.setText(SERVER_OPTIONS);
    gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    serverOptions.setLayout(gridLayout);
    GridData gridData = new GridData();
    gridData.horizontalAlignment = GridData.FILL;
    gridData.heightHint = 60;
    serverOptions.setLayoutData(gridData);

    m_serverOptionsField = new Text(serverOptions, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
    GridData gd = new GridData(GridData.FILL_BOTH);
    m_serverOptionsField.setLayoutData(gd);
    m_serverOptionsField.setText(DEFAULT_SERVER_OPTIONS);

    m_resetOptionsButton = new Button(serverOptions, SWT.PUSH);
    m_resetOptionsButton.setText(RESET);
    SWTUtil.applyDefaultButtonSize(m_resetOptionsButton);
    gridData = (GridData)m_resetOptionsButton.getLayoutData();
    gridData.verticalAlignment = SWT.BEGINNING;
    m_resetOptionsButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        m_serverOptionsField.setText(DEFAULT_SERVER_OPTIONS);
      }
    });
    setControl(topComp);
  }

  public boolean isPageComplete() {
    return true;
  }

  public boolean canFinish() {
    return isPageComplete();
  }

  public String getDomainConfigurationPath() {
    return m_configPathField.getText();
  }

  public String getServerOptions() {
    return m_serverOptionsField.getText();
  }
}
