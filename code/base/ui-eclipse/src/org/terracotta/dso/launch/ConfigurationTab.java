/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.launch;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;
import org.terracotta.dso.TcPlugin;
import org.terracotta.ui.util.SWTUtil;

public class ConfigurationTab extends AbstractLaunchConfigurationTab implements IDSOLaunchConfigurationConstants {
  private Text fServerText;
  private Button fConfigServerButton;
  private Text fConfigServerText;
  private Button fConfigFileButton;
  private Text fConfigFileText;
  private Button fConfigBrowseButton;
  
  private ModifyListener fBasicModifyListener = new ModifyListener() {
    public void modifyText(ModifyEvent evt) {
      updateLaunchConfigurationDialog();
    }
  };
  
  private SelectionAdapter fConfigSelector = new SelectionAdapter() {
    public void widgetSelected(SelectionEvent e) {
      if(e.widget == fConfigServerButton) {
        fConfigServerText.setEnabled(true);
        fConfigFileText.setEnabled(false);
      } else {
        fConfigServerText.setEnabled(false);
        fConfigFileText.setEnabled(true);
      }
    }
  };
  
  public void createControl(Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    setControl(comp);
    comp.setLayout(new GridLayout());
    Group group = new Group(comp, SWT.NONE);
    group.setText("Server specification");
    group.setLayout(new GridLayout());
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    fServerText = new Text(group, SWT.BORDER|SWT.SINGLE);
    fServerText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    fServerText.addModifyListener(fBasicModifyListener);

    group = new Group(comp, SWT.NONE);
    group.setText("Configuration specification");
    group.setLayout(new GridLayout(3, false));
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    fConfigServerButton = new Button(group, SWT.RADIO);
    fConfigServerButton.setText("Server");
    fConfigServerButton.addSelectionListener(fConfigSelector);
    fConfigServerText = new Text(group, SWT.BORDER|SWT.SINGLE);
    fConfigServerText.addModifyListener(fBasicModifyListener);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    fConfigServerText.setLayoutData(gridData);
    fConfigFileButton = new Button(group, SWT.RADIO);
    fConfigFileButton.setText("File");
    fConfigFileButton.addSelectionListener(fConfigSelector);
    fConfigFileText = new Text(group, SWT.BORDER|SWT.SINGLE);
    fConfigFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    fConfigFileText.addModifyListener(fBasicModifyListener);
    fConfigBrowseButton = new Button(group, SWT.PUSH);
    fConfigBrowseButton.setText("Browse...");
    SWTUtil.applyDefaultButtonSize(fConfigBrowseButton);
    fConfigBrowseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(),
            new WorkbenchContentProvider());
        dialog.setTitle("Terracotta");
        dialog.setMessage("Locate Terracotta configuration file");
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dialog.addFilter(new ViewerFilter() {
          public boolean select(Viewer viewer, Object parentElement, Object element) {
            if(element instanceof IProject || element instanceof IFolder) {
              return true;
            }
            if(element instanceof IFile) {
              IFile file = (IFile)element;
              return file.getProjectRelativePath().getFileExtension().equals("xml");
            }
            return false;
          }
        });
        dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
        if (dialog.open() == IDialogConstants.OK_ID) {
          IResource resource = (IResource) dialog.getFirstResult();
          String arg = resource.getFullPath().toString();
          IStringVariableManager variableManager = VariablesPlugin.getDefault().getStringVariableManager();
          String fileLoc = variableManager.generateVariableExpression("workspace_loc", arg); //$NON-NLS-1$
          fConfigFileText.setText(fileLoc);
        }
      }
    });
  }

  public boolean isValid(ILaunchConfiguration config) {
    setMessage(null);
    setErrorMessage(null);
    
    return validateServer() | validateConfigSpec();
  }

  private boolean validateServer() {
    return validateServerSpec(fServerText.getText());
  }
  
  private boolean validateConfigSpec() {
    if(fConfigServerButton.getSelection()) {
      return validateServerSpec(fConfigServerText.getText());
    } else {
      String configSpec = fConfigFileText.getText().trim(); 
      if(configSpec != null && !configSpec.startsWith("$")) {
        try {
          IStringVariableManager variableManager = VariablesPlugin.getDefault().getStringVariableManager();
          variableManager.validateStringVariables(configSpec);
          configSpec = variableManager.performStringSubstitution(configSpec);
        } catch(CoreException ce) {
          setErrorMessage(ce.getMessage());
          updateLaunchConfigurationDialog();          
          return false;
        }
        if(configSpec == null || configSpec.length() == 0) {
          setErrorMessage("Configuration file path is empty.");
          return false;
        }
        Path configSpecPath = new Path(configSpec);
        if(!configSpecPath.toFile().exists()) {
          setErrorMessage("File '"+configSpec+"' does not exist.");
          return false;
        }
      }
    }
    return true;
  }
  
  /*private*/ IResource getResource(String path) {
    Path containerPath = new Path(path);
    return ResourcesPlugin.getWorkspace().getRoot().findMember(containerPath);
  }

  private boolean validateServerSpec(String text) {
    if(text != null && text.length() > 0) {
      String[] elems = StringUtils.split(text, ",");
      
      if(elems != null && elems.length > 0) {
        for(int i = 0; i < elems.length; i++) {
          String elem = elems[i];
          int colonIndex = elem.indexOf(':');
          String host = colonIndex == -1 ? elem : elem.substring(0, colonIndex);
          
          if(host == null || host.length() == 0) {
            setErrorMessage("Malformed server list");
            return false;
          }
          if(colonIndex != -1) {
            String port = "";
            
            try {
              port = elem.substring(colonIndex+1);
              if(port != null && port.length() > 0) {
                Integer.parseInt(port);
              }
            } catch(NumberFormatException nfe) {
              setErrorMessage("Cannot parse port '"+port+"'");
              return false;
            }
          }
        }
      }
    }
  
    return true;
  }
  
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    try {
      IProject project = getProject(configuration);
      IFile configFile = TcPlugin.getDefault().getConfigurationFile(project);
      
      if(configFile != null) {
        String arg = configFile.getFullPath().toString();
        IStringVariableManager variableManager = VariablesPlugin.getDefault().getStringVariableManager();
        String configSpec = variableManager.generateVariableExpression("workspace_loc", arg); //$NON-NLS-1$
        configuration.setAttribute(ID_CONFIG_FILE_SPEC, configSpec);
      }
    } catch (CoreException ce) {
      /**/
    }
  }    

  
  public void initializeFrom(ILaunchConfiguration configuration) {
    try {
      String serverSpec = configuration.getAttribute(ID_SERVER_SPEC, "");
      fServerText.setText(serverSpec);
    } catch(CoreException ce) {
      /**/
    }
    
    try {
      String configFileSpec = configuration.getAttribute(ID_CONFIG_FILE_SPEC, (String)null);
      if(configFileSpec != null) {
        fConfigFileText.setText(configFileSpec);
        fConfigFileButton.setSelection(true);
      }
    } catch(CoreException ce) {
      /**/
    }

    try {
      String configServerSpec = configuration.getAttribute(ID_CONFIG_SERVER_SPEC, (String)null);
      if(configServerSpec != null) {
        fConfigServerText.setText(configServerSpec);
        fConfigServerButton.setSelection(true);
      }
    } catch(CoreException ce) {
      /**/
    }
  }
  
  private IProject getProject(ILaunchConfiguration configuration) throws CoreException {
    String projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
    if (projectName != null) {
      projectName = projectName.trim();
      if (projectName.length() > 0) { return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName); }
    }
    return null;
  }
  
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    String serverSpec = fServerText.getText().trim();
    configuration.setAttribute(ID_SERVER_SPEC, serverSpec != null && serverSpec.length() > 0 ? serverSpec : null);

    if (fConfigFileButton.getSelection()) {
      String spec = fConfigFileText.getText().trim();
      configuration.setAttribute(ID_CONFIG_FILE_SPEC, spec != null && spec.length() > 0 ? spec : null);
      configuration.setAttribute(ID_CONFIG_SERVER_SPEC, (String) null);
    } else {
      String spec = fConfigServerText.getText().trim();
      configuration.setAttribute(ID_CONFIG_SERVER_SPEC, spec != null && spec.length() > 0 ? spec : null);
      configuration.setAttribute(ID_CONFIG_FILE_SPEC, (String) null);
    }
  }

  public String getName() {
    return "Terracotta";
  }
}
