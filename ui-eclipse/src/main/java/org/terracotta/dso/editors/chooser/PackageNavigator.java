/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

import com.tc.util.event.EventMulticaster;
import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;

public class PackageNavigator extends ElementTreeSelectionDialog {

  private final IProject          m_project;
  private final EventMulticaster  m_valueListener;
  private final NavigatorBehavior m_behavior;

  public PackageNavigator(Shell shell, String title, IProject project, NavigatorBehavior behavior) {
    super(shell, behavior.getLabelProvider(), behavior.getContentProvider());

    m_project = project;
    m_valueListener = new EventMulticaster();
    m_behavior = behavior;

    setComparator(new JavaElementComparator());
    setTitle(behavior.getTitle());
    setMessage(behavior.getMessage());
    addFilter(behavior.getFilter(JavaCore.create(m_project)));
    setValidator(behavior.getValidator());
    setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
    setAllowMultiple(behavior.style() == SWT.MULTI);
  }

  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.OK_ID) {
      m_valueListener.fireUpdateEvent(new UpdateEvent(m_behavior.getValues()));
    }
    super.buttonPressed(buttonId);
  }

  public void addValueListener(UpdateEventListener listener) {
    m_valueListener.addListener(listener);
  }

}
