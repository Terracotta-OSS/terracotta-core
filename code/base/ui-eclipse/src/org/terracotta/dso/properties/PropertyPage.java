/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.chooser.ConfigFileBehavior;
import org.terracotta.dso.editors.chooser.NavigatorBehavior;
import org.terracotta.dso.editors.chooser.PackageNavigator;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;

public final class PropertyPage extends org.eclipse.ui.dialogs.PropertyPage {
  private static final String  TERRACOTTA_CONFIG                   = "Terracotta Configuration";
  private static final String  BROWSE                              = "Browse...";
  private static final String  RESET                               = "Reset";
  private static final String  SERVER_OPTIONS                      = "Server Options";

  private Text                 m_configPathField;
  private Button               m_configFileButton;
  private Text                 m_serverOptionsField;
  private Button               m_autoStartServerButton;
  private Button               m_warnConfigProblemsButton;
  private Button               m_queryRestartButton;
  private Button               m_resetOptionsButton;

  private static final String  DEFAULT_CONFIG_FILENAME             = TcPlugin.DEFAULT_CONFIG_FILENAME;
  private static final String  DEFAULT_SERVER_OPTIONS              = TcPlugin.DEFAULT_SERVER_OPTIONS;
  private static final boolean DEFAULT_AUTO_START_SERVER_OPTION    = TcPlugin.DEFAULT_AUTO_START_SERVER_OPTION;
  private static final boolean DEFAULT_WARN_CONFIG_PROBLEMS_OPTION = TcPlugin.DEFAULT_WARN_CONFIG_PROBLEMS_OPTION;
  private static final boolean DEFAULT_QUERY_RESTART_OPTION        = TcPlugin.DEFAULT_QUERY_RESTART_OPTION;

  private static final String  AUTO_RESTART_SERVER_MSG             = "Automatically start the Terracotta Server instance when necessary";
  private static final String  WARN_CONFIG_PROBLEMS_MSG            = "Warn about config problems before launching a Terracotta process";
  private static final String  QUERY_RESTART_MSG                   = "When the config changes, offer to restart running Terracotta processes";

  private void fillControls() {
    TcPlugin plugin = TcPlugin.getDefault();
    IProject project = getProject();
    IFile configFile = plugin.getConfigurationFile(project);
    validateConfigFile(configFile);
    m_configPathField.setText(configFile != null ? configFile.getProjectRelativePath().toString() : "");
    m_serverOptionsField.setText(plugin.getServerOptions(project));
    m_autoStartServerButton.setSelection(plugin.getAutoStartServerOption(project));
    m_warnConfigProblemsButton.setSelection(plugin.getWarnConfigProblemsOption(project));
    m_queryRestartButton.setSelection(plugin.getQueryRestartOption(project));
  }

  protected Control createContents(Composite parent) {
    final Composite topComp = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 1;
    gridLayout.makeColumnsEqualWidth = false;
    topComp.setLayout(gridLayout);
    topComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Group domainConfig = new Group(topComp, SWT.NONE);
    domainConfig.setText(TERRACOTTA_CONFIG);
    gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    domainConfig.setLayout(gridLayout);
    domainConfig.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    m_configPathField = new Text(domainConfig, SWT.SINGLE | SWT.BORDER);
    m_configPathField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    m_configFileButton = new Button(domainConfig, SWT.PUSH);
    m_configFileButton.setText(BROWSE);
    m_configFileButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    SWTUtil.applyDefaultButtonSize(m_configFileButton);
    m_configFileButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        NavigatorBehavior behavior = new ConfigFileBehavior();
        PackageNavigator dialog = new PackageNavigator(getShell(), behavior.getTitle(), getProject(), behavior);
        dialog.addValueListener(new UpdateEventListener() {
          public void handleUpdate(UpdateEvent event) {
            m_configPathField.setText((String) event.data);
            validateConfigFile();
          }
        });
        dialog.open();
      }
    });
    Group serverOptions = new Group(topComp, SWT.NONE);
    serverOptions.setText(SERVER_OPTIONS);
    serverOptions.setLayout(new GridLayout(2, false));
    GridData gridData = new GridData();
    gridData.horizontalAlignment = GridData.FILL;
    gridData.heightHint = 60;
    serverOptions.setLayoutData(gridData);

    m_serverOptionsField = new Text(serverOptions, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
    GridData gd = new GridData(GridData.FILL_BOTH);
    m_serverOptionsField.setLayoutData(gd);

    m_resetOptionsButton = new Button(serverOptions, SWT.PUSH);
    m_resetOptionsButton.setText(RESET);
    SWTUtil.applyDefaultButtonSize(m_resetOptionsButton);
    gd = (GridData) m_resetOptionsButton.getLayoutData();
    gd.verticalAlignment = SWT.BEGINNING;
    m_resetOptionsButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        m_serverOptionsField.setText(DEFAULT_SERVER_OPTIONS);
      }
    });

    m_autoStartServerButton = new Button(topComp, SWT.CHECK);
    m_autoStartServerButton.setText(AUTO_RESTART_SERVER_MSG);
    m_autoStartServerButton.setLayoutData(new GridData());

    m_warnConfigProblemsButton = new Button(topComp, SWT.CHECK);
    m_warnConfigProblemsButton.setText(WARN_CONFIG_PROBLEMS_MSG);
    m_warnConfigProblemsButton.setLayoutData(new GridData());

    m_queryRestartButton = new Button(topComp, SWT.CHECK);
    m_queryRestartButton.setText(QUERY_RESTART_MSG);
    m_queryRestartButton.setLayoutData(new GridData());

    Composite composite = new Composite(topComp, SWT.EMBEDDED);
    composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
    fillControls();
    return topComp;
  }

  protected void performDefaults() {
    m_configPathField.setText(DEFAULT_CONFIG_FILENAME);
    m_serverOptionsField.setText(DEFAULT_SERVER_OPTIONS);
    m_autoStartServerButton.setSelection(DEFAULT_AUTO_START_SERVER_OPTION);
    m_warnConfigProblemsButton.setSelection(DEFAULT_WARN_CONFIG_PROBLEMS_OPTION);
    m_queryRestartButton.setSelection(DEFAULT_QUERY_RESTART_OPTION);
  }

  private IProject getProject() {
    return ((IJavaProject) getElement()).getProject();
  }

  private void updateProject() {
    TcPlugin plugin = TcPlugin.getDefault();
    IProject project = getProject();
    plugin.setup(project, m_configPathField.getText(), m_serverOptionsField.getText());
    plugin.setAutoStartServerOption(project, m_autoStartServerButton.getSelection());
    plugin.setWarnConfigProblemsOption(project, m_warnConfigProblemsButton.getSelection());
    plugin.setQueryRestartOption(project, m_queryRestartButton.getSelection());
  }

  public boolean performOk() {
    if (validateConfigFile()) {
      updateProject();
      return true;
    } else {
      getControl().getDisplay().beep();
      m_configPathField.forceFocus();
      return false;
    }
  }

  private boolean validateConfigFile() {
    IProject project = getProject();
    IFile configFile = project.getFile(m_configPathField.getText());
    return validateConfigFile(configFile);
  }

  private boolean validateConfigFile(IFile configFile) {
    if (configFile == null || !configFile.exists()) {
      setErrorMessage("Config file not found");
      return false;
    } else {
      setErrorMessage(null);
      return true;
    }
  }
}
