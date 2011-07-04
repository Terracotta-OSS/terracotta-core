/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.terracotta.ui.util.SWTUtil;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionDialog extends MessageDialog {

  private final Shell  m_parentShell;
  private final String m_stackTrace;

  private static String throwable2StackTrace(Throwable throwable) {
    if(throwable == null) return "";
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    throwable.printStackTrace(pw);
    return sw.toString();
  }
  
  public ExceptionDialog(Shell shell, String title, String message, Throwable throwable) {
    this(shell, title, message, throwable2StackTrace(throwable));
  }
  
  public ExceptionDialog(Shell shell, String title, String message, String stackTrace) {
    super(shell, title, null, "  " + message, MessageDialog.ERROR, new String[] { IDialogConstants.OK_LABEL }, 0);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.m_parentShell = shell;
    this.m_stackTrace = stackTrace;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    if(m_parentShell != null) {
      SWTUtil.placeDialogInCenter(m_parentShell, shell);
    }
  }

  protected Control createDialogArea(Composite parent) {
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = gridLayout.marginHeight = 0;
    parent.setLayout(gridLayout);
    return super.createDialogArea(parent);
  }

  protected Control createCustomArea(Composite parent) {
    new Layout(parent, m_stackTrace);
    return parent;
  }

  // --------------------------------------------------------------------------------

  private static class Layout {
    final Text m_area;

    private Layout(Composite parent, String stackTrace) {
      Composite comp = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout();
      gridLayout.marginHeight = gridLayout.marginWidth = 0;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));

      this.m_area = new Text(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
      m_area.setText(stackTrace);
      GridData gridData =new GridData(GridData.FILL_BOTH);
      gridData.widthHint = SWTUtil.textColumnsToPixels(m_area, 120);
      gridData.heightHint = SWTUtil.textRowsToPixels(m_area, 24);
      m_area.setLayoutData(gridData);
    }
  }
}
