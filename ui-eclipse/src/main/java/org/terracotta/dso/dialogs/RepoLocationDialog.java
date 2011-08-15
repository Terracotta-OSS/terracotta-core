/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.terracotta.ui.util.SWTUtil;

import java.io.File;

public class RepoLocationDialog extends MessageDialog {

  private Text          m_repoLocation;
  private Button        m_browseButton;
  private ValueListener m_valueListener;

  public RepoLocationDialog(Shell parentShell, String title, String message) {
    super(parentShell, title, null, message, MessageDialog.NONE, new String[] {
      IDialogConstants.OK_LABEL,
      IDialogConstants.CANCEL_LABEL }, 0);
  }

  protected Control createCustomArea(Composite parent) {
    Composite comp = new Composite(parent, SWT.NONE);
    comp.setLayout(new GridLayout(2, false));
    comp.setLayoutData(new GridData(GridData.FILL_BOTH));
    
    m_repoLocation = new Text(comp, SWT.SINGLE | SWT.BORDER);
    m_repoLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    m_browseButton = new Button(comp, SWT.PUSH);
    m_browseButton.setText("Browse...");
    m_browseButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
        directoryDialog.setText("Terracotta Module Repository Chooser");
        directoryDialog.setMessage("Select a module repository directory");
        String path = directoryDialog.open();
        if (path != null) {
          File dir = new File(path);
          m_repoLocation.setText(dir.toString());
        }
      }
    });
    SWTUtil.applyDefaultButtonSize(m_browseButton);
    
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
