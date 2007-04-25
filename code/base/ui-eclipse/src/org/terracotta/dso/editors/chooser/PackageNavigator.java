/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.ui.JavaWorkbenchAdapter;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerLabelProvider;
import org.eclipse.jdt.internal.ui.packageview.WorkingSetAwareJavaElementSorter;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.terracotta.ui.util.SWTUtil;

import com.tc.util.event.EventMulticaster;
import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;

import java.util.ArrayList;
import java.util.List;

public class PackageNavigator extends MessageDialog {

  private final Shell             m_parentShell;
  private final IProject          m_project;
  private final EventMulticaster  m_valueListener;
  private final NavigatorBehavior m_behavior;
  private Layout                  m_layout;

  public PackageNavigator(Shell shell, String title, IProject project, NavigatorBehavior behavior) {
    super(shell, title, null, null, MessageDialog.NONE, new String[] {
      IDialogConstants.OK_LABEL,
      IDialogConstants.CANCEL_LABEL }, 0);
    this.m_parentShell = shell;
    this.m_project = project;
    this.m_valueListener = new EventMulticaster();
    this.m_behavior = behavior;
  }

  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setSize(400, 300);
    SWTUtil.placeDialogInCenter(m_parentShell, shell);
  }

  protected Control createDialogArea(Composite parent) {
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    parent.setLayout(gridLayout);
    return super.createDialogArea(parent);
  }

  protected Control createButtonBar(Composite parent) {
    Composite comp = (Composite) super.createButtonBar(parent);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    gridLayout.marginWidth = 5;
    gridLayout.marginHeight = 5;
    comp.setLayout(gridLayout);
    getButton(getDefaultButtonIndex()).setEnabled(false);
    return comp;
  }

  void okButtonEnabled(boolean enable) {
    getButton(getDefaultButtonIndex()).setEnabled(enable);
  }

  void enableSelection(boolean enable, ISelectionChangedListener listener) {
    if (enable) m_layout.m_viewer.addSelectionChangedListener(listener);
    else m_layout.m_viewer.removeSelectionChangedListener(listener);
  }

  protected Control createCustomArea(Composite parent) {
    initLayout(m_layout = new Layout(parent, m_behavior.style()));
    return parent;
  }

  private void initLayout(final Layout layout) {
    JavaHierarchyContentProvider contentProvider;
    layout.m_viewer.setContentProvider(contentProvider = new JavaHierarchyContentProvider());
    layout.m_viewer.setLabelProvider(new PackageExplorerLabelProvider(AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS
        | JavaElementLabels.P_COMPRESSED | JavaElementLabels.ALL_CATEGORY,
        AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS, contentProvider));
    layout.m_viewer.setSorter(new WorkingSetAwareJavaElementSorter());
    IJavaProject jproj = JavaCore.create(m_project);
    IJavaElement root = jproj.getJavaModel();
    layout.m_viewer.setInput(root);
    layout.m_viewer.addSelectionChangedListener(m_behavior.getSelectionChangedListener(this));
    layout.m_viewer.addFilter(m_behavior.getFilter(jproj));
  }

  protected void buttonPressed(int buttonId) {
    tearDown();
    if (buttonId == IDialogConstants.OK_ID) {
      m_valueListener.fireUpdateEvent(new UpdateEvent(m_behavior.getValues()));
    }
    super.buttonPressed(buttonId);
  }

  public void addValueListener(UpdateEventListener listener) {
    m_valueListener.addListener(listener);
  }

  private void tearDown() {
    m_layout.m_viewer.getTree().setRedraw(false);
    m_layout.m_viewer.getTree().setEnabled(false);
    m_layout.m_viewer.getTree().removeAll();
  }

  // --------------------------------------------------------------------------------

  private class JavaHierarchyContentProvider extends PackageExplorerContentProvider {

    private final WorkbenchContentProvider m_workbench;

    public JavaHierarchyContentProvider() {
      super(true);
      this.m_workbench = new WorkbenchContentProvider() {
        protected IWorkbenchAdapter getAdapter(Object element) {
          return new JavaWorkbenchAdapter() {
            public Object[] getChildren(Object parentElement) {
              List subset = new ArrayList();
              Object[] children = super.getChildren(parentElement);
              for (int i = 0; i < children.length; i++) {
                if (!(children[i] instanceof IPackageFragment && super.getChildren(children[i]).length == 0)
                    && !(children[i] instanceof PackageFragmentRoot && !(children[i] instanceof JarPackageFragmentRoot))) {
                  subset.add(children[i]);
                }
              }
              return subset.toArray();
            }
          };
        }
      };
    }

    public Object[] getChildren(Object parentElement) {
      List subset = new ArrayList();
      Object[] children = super.getChildren(parentElement);
      for (int i = 0; i < children.length; i++) {
        if (children[i] instanceof JarPackageFragmentRoot) {
          Object[] workbenchElements = m_workbench.getChildren(((IJavaElement) children[i]).getParent());
          return workbenchElements;
        }
        if (children[i].getClass().getName().equals("org.eclipse.jdt.internal.core.JarPackageFragment")) {
          Object[] workbenchElements = m_workbench.getChildren(parentElement);
          return workbenchElements;
        }
        if (!(children[i] instanceof IPackageFragment && super.getChildren(children[i]).length == 0)) {
          subset.add(children[i]);
        }
      }
      return subset.toArray();
    }
  }

  // --------------------------------------------------------------------------------

  private class Layout {

    final TreeViewer m_viewer;
    GridData         m_gridData;

    private Layout(Composite parent, int style) {
      this.m_viewer = new TreeViewer(parent, style);
      m_viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
    }
  }
}
