/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.ConfigurationEditor;
import org.terracotta.ui.util.SWTUtil;

public class ConfigProblemsDialog extends MessageDialog {

  private final Shell    fParentShell;
  private final IProject fProject;
  private Table          fTable;
  private Button         fDisableConfigWarningsButton;

  private static String  DISABLE_CONFIG_WARNINGS_MSG = "Always proceed even if there are config problems";

  private static String  TITLE                       = "Terracotta";
  private static String  MSG                         = "There are problems with the Terracotta configuration. Continue?";

  private static Image   ERROR_IMG                   = getSharedImage("IMG_OBJS_ERROR_PATH");
  private static Image   WARNING_IMG                 = getSharedImage("IMG_OBJS_WARNING_PATH");
  private static Image   INFO_IMG                    = getSharedImage("IMG_OBJS_INFO_PATH");

  public ConfigProblemsDialog(Shell shell, IProject project) {
    super(shell, TITLE, null, MSG, MessageDialog.NONE, new String[] {
      IDialogConstants.OK_LABEL,
      IDialogConstants.CANCEL_LABEL }, 0);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    fProject = project;
    fParentShell = shell;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    if (fParentShell != null) {
      SWTUtil.placeDialogInCenter(fParentShell, shell);
    }
  }

  protected Control createDialogArea(Composite parent) {
    parent.setLayout(new GridLayout());
    return super.createDialogArea(parent);
  }

  protected Control createCustomArea(Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    comp.setLayout(new GridLayout());
    comp.setLayoutData(new GridData(GridData.FILL_BOTH));

    fTable = new Table(comp, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
    fTable.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        try {
          ConfigurationEditor configEditor = TcPlugin.getDefault().openConfigurationEditor(fProject);
          TableItem item = fTable.getSelection()[0];
          configEditor.gotoMarker((IMarker) item.getData());
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
    fTable.setHeaderVisible(true);
    fTable.setLinesVisible(true);

    TableColumn fieldCol = new TableColumn(fTable, SWT.NONE);
    fieldCol.setResizable(true);
    fieldCol.setText("Description");
    fieldCol.pack();

    TableColumn nameCol = new TableColumn(fTable, SWT.NONE);
    nameCol.setResizable(true);
    nameCol.setText("Location");
    nameCol.pack();

    SWTUtil.makeTableColumnsResizeWeightedWidth(comp, fTable, new int[] { 5, 1 });
    GridData gridData = new GridData(GridData.FILL_BOTH);
    gridData.heightHint = SWTUtil.tableRowsToPixels(fTable, 5);
    gridData.widthHint = SWTUtil.textColumnsToPixels(fTable, 120);
    fTable.setLayoutData(gridData);

    IFile configFile = TcPlugin.getDefault().getConfigurationFile(fProject);
    IMarker[] markers = null;
    try {
      markers = configFile.findMarkers("org.eclipse.core.resources.problemmarker", true, IResource.DEPTH_ZERO);
    } catch (CoreException ce) {
      ce.printStackTrace();
    }
    if (markers != null && markers.length > 0) {
      for (IMarker marker : markers) {
        TableItem item = new TableItem(fTable, SWT.NONE);
        item.setData(marker);
        item.setImage(getImage(marker));
        item.setText(new String[] {
          marker.getAttribute(IMarker.MESSAGE, ""),
          "line " + marker.getAttribute(IMarker.LINE_NUMBER, 0) });
      }
    }

    fDisableConfigWarningsButton = new Button(comp, SWT.CHECK);
    fDisableConfigWarningsButton.setLayoutData(new GridData());
    fDisableConfigWarningsButton.setText(DISABLE_CONFIG_WARNINGS_MSG);
    fDisableConfigWarningsButton.setSelection(!TcPlugin.getDefault().getWarnConfigProblemsOption(fProject));
    fDisableConfigWarningsButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        boolean disableWarn = fDisableConfigWarningsButton.getSelection();
        TcPlugin.getDefault().setWarnConfigProblemsOption(fProject, !disableWarn);
      }
    });
    
    return parent;
  }

  private static Image getImage(IMarker marker) {
    switch (marker.getAttribute(IMarker.SEVERITY, 0)) {
      case IMarker.SEVERITY_ERROR:
        return ERROR_IMG;
      case IMarker.SEVERITY_WARNING:
        return WARNING_IMG;
      case IMarker.SEVERITY_INFO:
        return INFO_IMG;
      default:
        return null;
    }
  }

  private static Image getSharedImage(String symbolicName) {
    ImageDescriptor imageDesc = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(symbolicName);
    if (imageDesc != null) { return JFaceResources.getResources().createImageWithDefault(imageDesc); }
    return null;
  }
}
