/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AddModuleDialog extends MessageDialog {

  private Text          m_moduleName;
  private Text          m_moduleVersion;
  private ValueListener m_valueListener;

  public AddModuleDialog(Shell parentShell, String title, String message) {
    super(parentShell, title, null, message, MessageDialog.NONE, new String[] {
      IDialogConstants.OK_LABEL,
      IDialogConstants.CANCEL_LABEL }, 0);
  }

  protected Control createCustomArea(Composite parent) {
    final Composite comp = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    comp.setLayout(gridLayout);
    comp.setLayoutData(new GridData(GridData.FILL_BOTH));

    GridData gridData = new GridData();
    gridData.verticalAlignment = GridData.VERTICAL_ALIGN_CENTER;
    Label nameLabel = new Label(comp, SWT.RIGHT);
    nameLabel.setText("Name");
    nameLabel.setLayoutData(gridData);

    m_moduleName = new Text(comp, SWT.SINGLE | SWT.BORDER);
    m_moduleName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Label versionLabel = new Label(comp, SWT.RIGHT);
    versionLabel.setText("Version");
    versionLabel.setLayoutData(gridData);

    m_moduleVersion = new Text(comp, SWT.SINGLE | SWT.BORDER);
    m_moduleVersion.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    return comp;
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      if (m_valueListener != null) m_valueListener.setValues(m_moduleName.getText(), m_moduleVersion.getText());
    }
    super.buttonPressed(buttonId);
  }

  public void addValueListener(ValueListener listener) {
    this.m_valueListener = listener;
  }

  // --------------------------------------------------------------------------------

  public static interface ValueListener {
    void setValues(String name, String version);
  }
}
