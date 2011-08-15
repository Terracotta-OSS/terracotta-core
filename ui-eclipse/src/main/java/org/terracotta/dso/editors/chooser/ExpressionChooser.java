/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.EventMulticaster;
import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;

public class ExpressionChooser extends MessageDialog {

  private static final String    EXPLORE = "Explore...";
  private static final String    ADD     = "Add";
  private final Shell            m_parentShell;
  private final IProject         m_project;
  private final EventMulticaster m_valueListener;
  private Shell                  m_shell;
  private boolean                m_isAddButton;
  private SelectionListener      m_exploreListener;
  private SelectionListener      m_addListener;
  private Layout                 m_layout;
  private NavigatorBehavior      m_behavior;

  public ExpressionChooser(Shell shell, String title, String message, IProject project, NavigatorBehavior behavior) {
    super(shell, title, null, message, MessageDialog.NONE, new String[] {
      IDialogConstants.OK_LABEL,
      IDialogConstants.CANCEL_LABEL }, 0);
    this.m_parentShell = shell;
    this.m_project = project;
    this.m_valueListener = new EventMulticaster();
    this.m_behavior = behavior;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    m_shell = shell;
    m_shell.setSize(400, 250);
    SWTUtil.placeDialogInCenter(m_parentShell, m_shell);
  }

  protected Control createCustomArea(Composite parent) {
    initLayout(m_layout = new Layout(parent));
    return parent;
  }

  private void initLayout(final Layout layout) {
    layout.m_exploreButton.addSelectionListener(m_exploreListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        PackageNavigator dialog = new PackageNavigator(getShell(), m_behavior.getTitle(), m_project, m_behavior);
        dialog.addValueListener(new UpdateEventListener() {
          public void handleUpdate(UpdateEvent arg) {
            String[] values = (String[]) arg.data;
            for (int i = 0; i < values.length; i++) {
              layout.m_list.add(values[i]);
              layout.m_list.forceFocus();
            }
          }
        });
        dialog.open();
      }
    });
    m_addListener = new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        layout.m_list.add(layout.m_selectField.getText());
        layout.m_selectField.setText("");
      }
    };
    layout.m_selectField.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        if (((Text) e.widget).getText().trim().equals("")) {
          layout.m_exploreButton.removeSelectionListener(m_addListener);
          layout.m_exploreButton.addSelectionListener(m_exploreListener);
          layout.m_exploreButton.setText(EXPLORE);
          SWTUtil.applyDefaultButtonSize(layout.m_exploreButton);
          m_shell.setDefaultButton(getButton(IDialogConstants.OK_ID));
          m_isAddButton = false;
        } else if (!m_isAddButton) {
          layout.m_exploreButton.removeSelectionListener(m_exploreListener);
          layout.m_exploreButton.addSelectionListener(m_addListener);
          layout.m_exploreButton.setText(ADD);
          SWTUtil.applyDefaultButtonSize(layout.m_exploreButton);
          m_shell.setDefaultButton(layout.m_exploreButton);
          m_isAddButton = true;
        }
      }
    });
    layout.m_list.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.DEL || e.keyCode == SWT.BS) {
          int[] selected = ((List) e.getSource()).getSelectionIndices();
          layout.m_list.remove(selected);
          layout.m_selectField.forceFocus();
        }
      }
    });
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      m_valueListener.fireUpdateEvent(new UpdateEvent(m_layout.m_list.getItems()));
    }
    super.buttonPressed(buttonId);
  }

  public void addValueListener(UpdateEventListener listener) {
    m_valueListener.addListener(listener);
  }

  // --------------------------------------------------------------------------------

  private class Layout {
    final Text   m_selectField;
    final Button m_exploreButton;
    final List   m_list;
    GridData     m_gridData;

    private Layout(Composite parent) {
      Composite comp = new Composite(parent, SWT.NONE);

      GridLayout gridLayout = new GridLayout(2, false);
      gridLayout.marginWidth = gridLayout.marginHeight = 0;
      comp.setLayout(gridLayout);
      comp.setLayoutData(new GridData(GridData.FILL_BOTH));

      this.m_selectField = new Text(comp, SWT.SINGLE | SWT.BORDER);
      m_selectField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      this.m_exploreButton = new Button(comp, SWT.PUSH);
      m_exploreButton.setText(EXPLORE);
      SWTUtil.applyDefaultButtonSize(m_exploreButton);

      this.m_list = new List(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
      m_gridData = new GridData(GridData.FILL_BOTH);
      m_gridData.horizontalSpan = 2;
      m_list.setLayoutData(m_gridData);
    }
  }
}
