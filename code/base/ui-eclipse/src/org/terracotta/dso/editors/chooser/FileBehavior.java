/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.chooser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.terracotta.dso.TcPlugin;

public class FileBehavior extends AbstractNavigatorBehavior {

  private static final String SELECT_FILE     = "Select File";
  private static final String SELECT_FILE_MSG = "Select a file";
  protected String            m_selectedValue;

  public int style() {
    return SWT.SINGLE;
  }

  public String getTitle() {
    return SELECT_FILE;
  }

  public ViewerFilter getFilter(final IJavaProject javaProject) {
    return new ViewerFilter() {
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        return filterSelect(viewer, parentElement, element);
      }
    };
  }

  protected boolean packageFragmentContainsJavaResources(final IPackageFragment packageFragment) {
    try {
      return packageFragment.containsJavaResources();
    } catch (JavaModelException jme) {
      return false;
    }
  }

  protected boolean isPackageFragmentBinaryKind(final IPackageFragment packageFragment) {
    try {
      return packageFragment.getKind() == IPackageFragmentRoot.K_BINARY;
    } catch (JavaModelException jme) {
      return false;
    }
  }

  protected boolean filterSelect(Viewer viewer, Object parentElement, Object element) {
    if(parentElement instanceof ICompilationUnit || parentElement instanceof IClassFile) return false;
    if (element instanceof IJavaModel) return true;
    if (element instanceof IJavaProject) return true;
    if (element instanceof IFolder) return true;
    if (element instanceof IFile) return true;
    if (element instanceof IPackageFragment) {
      IPackageFragment packageFragment = (IPackageFragment) element;
      if (isPackageFragmentBinaryKind(packageFragment)
          || (packageFragment.isDefaultPackage() && !packageFragmentContainsJavaResources(packageFragment))) { return false; }
      try {
        return ((IPackageFragment) element).containsJavaResources();
      } catch (JavaModelException jme) {/**/
      }
      return true;
    }
    if (element instanceof IPackageFragmentRoot) return true;
//    if (element instanceof ClassPathContainer) {
//      ClassPathContainer container = (ClassPathContainer) element;
//      IClasspathEntry cpe = container.getClasspathEntry();
//      return (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE);
//    }
    if (element instanceof ICompilationUnit) return true;
    return false;
  }

  public ISelectionStatusValidator getValidator() {
    return new ISelectionStatusValidator() {
      public IStatus validate(Object[] selection) {
        m_selectedValue = null;
        for (Object element : selection) {
          if (element instanceof IFile) {
            IFile file = (IFile) element;
            m_selectedValue = file.getProjectRelativePath().toString();
          }
        }
        String id = TcPlugin.getPluginId();
        if (m_selectedValue == null) { return new Status(IStatus.ERROR, id, IStatus.ERROR, "", null); }
        return new Status(IStatus.OK, id, IStatus.OK, "", null);
      }
    };
  }

  public Object getValues() {
    return m_selectedValue;
  }

  @Override
  public String getMessage() {
    return SELECT_FILE_MSG;
  }
}
