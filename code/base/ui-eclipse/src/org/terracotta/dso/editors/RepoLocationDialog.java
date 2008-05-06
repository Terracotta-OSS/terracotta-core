/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class RepoLocationDialog extends MessageDialog {

  private Text          m_repoLocation;
  private ValueListener m_valueListener;

  public RepoLocationDialog(Shell parentShell, String title, String message) {
    super(parentShell, title, null, message, MessageDialog.NONE, new String[] {
      IDialogConstants.OK_LABEL,
      IDialogConstants.CANCEL_LABEL }, 0);
  }

  protected Control createCustomArea(Composite parent) {
    m_repoLocation = new Text(parent, SWT.SINGLE | SWT.BORDER);
    m_repoLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    return m_repoLocation;
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      if (m_valueListener != null) m_valueListener.setValues(m_repoLocation.getText());
    }
    super.buttonPressed(buttonId);
  }

  public void addValueListener(ValueListener listener) {
    this.m_valueListener = listener;
  }

  // --------------------------------------------------------------------------------

  public static interface ValueListener {
    void setValues(String repoLocation);
  }
}
