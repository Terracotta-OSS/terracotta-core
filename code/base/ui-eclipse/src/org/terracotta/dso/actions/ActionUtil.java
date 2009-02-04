/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.terracotta.dso.JdtUtils;
import org.terracotta.dso.TcPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

/**
 * Collection of static methods for finding various workspace artifacts, such as the currently selected Java project.
 * Deals with the Package Explorer as well as selected textual elements in a JavaEditor.
 * 
 * @see org.eclipse.jface.viewers.IStructuredSelection
 * @see org.eclipse.jface.viewers.ISelection
 * @see org.eclipse.jface.text.ITextSelection
 * @see org.terracotta.dso.actions
 */

public class ActionUtil {
  public static IJavaProject locateSelectedJavaProject(ISelection selection) {
    if (selection != null && !selection.isEmpty()) {
      if (selection instanceof IStructuredSelection) {
        IStructuredSelection ss = (IStructuredSelection) selection;
        Iterator iter = ss.iterator();
        Object obj;

        while (iter.hasNext()) {
          obj = iter.next();

          if (obj instanceof IJavaElement) {
            return ((IJavaElement) obj).getJavaProject();
          } else if (obj instanceof IProject) {
            return findJavaProject((IProject) obj);
          } else if (obj instanceof IResource) {
            return findJavaProject(((IResource) obj).getProject());
          } else if (obj instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) obj;
            IResource res = (IResource) adaptable.getAdapter(IResource.class);
            if (res != null) { return findJavaProject(res.getProject()); }
          }
        }
      }
    }

    IEditorPart part = findSelectedEditorPart();

    if (part != null) {
      IEditorInput input = part.getEditorInput();

      if (input instanceof IFileEditorInput) {
        IProject project = ((IFileEditorInput) input).getFile().getProject();
        if (project != null) { return findJavaProject(project); }
      } else {
        IClassFile classFile = (IClassFile) input.getAdapter(IClassFile.class);
        if (classFile != null) { return classFile.getJavaProject(); }
      }
    }

    return null;
  }

  public static IJavaProject findJavaProject(IProject project) {
    return project.isOpen() ? JavaCore.create(project) : null;
  }

  public static IJavaProject findSelectedJavaProject(ISelection selection) {
    if (selection != null) {
      if (selection instanceof IStructuredSelection) {
        IJavaElement elem = findSelectedJavaElement((IStructuredSelection) selection);

        if (elem instanceof IJavaProject) { return (IJavaProject) elem; }
      }
    }

    return null;
  }

  public static IPackageFragment findSelectedPackageFragment(ISelection selection) {
    if (selection != null) {
      IJavaElement elem = findSelectedJavaElement(selection);

      if (elem instanceof IPackageFragment) {
        return (IPackageFragment) elem;
      } else if (elem instanceof IPackageDeclaration) {
        IPackageDeclaration packageDecl = (IPackageDeclaration) elem;
        String name = packageDecl.getElementName();
        IPackageFragmentRoot root = JdtUtils.getPackageFragmentRoot(elem);

        if (root != null) { return root.getPackageFragment(name); }
      }
    }

    return null;
  }

  public static IField findSelectedField(ISelection selection) {
    if (selection != null) {
      IJavaElement elem = findSelectedJavaElement(selection);

      if (elem instanceof IField) { return (IField) elem; }
    }

    return null;
  }

  public static ICompilationUnit findSelectedCompilationUnit() {
    ITextEditor editor = findSelectedTextEditor();

    if (editor != null) { return JdtUtils.getInputAsCompilationUnit(editor); }

    return null;
  }

  public static IEditorPart findSelectedEditorPart() {
    IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

    if (win != null) {
      IWorkbenchPage page = win.getActivePage();

      if (page != null) { return page.getActiveEditor(); }
    }

    return null;
  }

  public static ITextEditor findSelectedTextEditor() {
    IEditorPart part = findSelectedEditorPart();

    if (part != null && part instanceof ITextEditor) { return (ITextEditor) part; }

    return null;
  }

  public static IMethod findSelectedMethod(ISelection selection) {
    if (selection != null) {
      IJavaElement elem = findSelectedJavaElement(selection);

      if (elem instanceof IMethod) { return (IMethod) elem; }
    }

    return null;
  }

  public static IFile findSelectedFile(ISelection selection) {
    if (selection != null) {
      if (selection instanceof IStructuredSelection) {
        IStructuredSelection ss = (IStructuredSelection) selection;

        Object obj = ss.getFirstElement();
        if (obj instanceof IFile) { return (IFile) obj; }
      }
    }

    return null;
  }

  public static ICompilationUnit findSelectedCompilationUnit(ISelection selection) {
    if (selection != null) {
      if (selection instanceof IStructuredSelection) {
        IJavaElement elem = findSelectedJavaElement((IStructuredSelection) selection);

        if (elem instanceof ICompilationUnit) { return (ICompilationUnit) elem; }
      } else if (selection instanceof ITextSelection) { return findSelectedCompilationUnit(); }
    }

    return null;
  }

  public static IType findSelectedType(ISelection selection) {
    if (selection != null) {
      IJavaElement elem = findSelectedJavaElement(selection);

      if (elem instanceof IType) {
        return (IType) elem;
      } else if (elem instanceof IMethod) {
        return ((IMethod) elem).getDeclaringType();
      } else if (elem instanceof IField) {
        IJavaProject javaProject = elem.getJavaProject();
        IField field = (IField) elem;
        IType declaringType = field.getDeclaringType();

        try {
          String sig = field.getTypeSignature();
          String typeName = JdtUtils.getResolvedTypeName(sig, declaringType);

          return JdtUtils.findType(javaProject, typeName);
        } catch (JavaModelException jme) {/**/
        }
      } else if (elem instanceof ILocalVariable) {
        IJavaProject javaProject = elem.getJavaProject();
        ILocalVariable var = (ILocalVariable) elem;
        IType enclosingType = (IType) elem.getAncestor(IJavaElement.TYPE);

        if (enclosingType != null) {
          try {
            String sig = var.getTypeSignature();
            String typeName = JdtUtils.getResolvedTypeName(sig, enclosingType);

            return JdtUtils.findType(javaProject, typeName);
          } catch (JavaModelException jme) {/**/
          }
        }
      } else if (elem instanceof IImportDeclaration) {
        IImportDeclaration importDecl = (IImportDeclaration) elem;
        String typeName = importDecl.getElementName();

        if (!importDecl.isOnDemand()) {
          try {
            return JdtUtils.findType(elem.getJavaProject(), typeName);
          } catch (JavaModelException jme) {/**/
          }
        }
      } else if (elem instanceof IClassFile) {
        try {
          return ((IClassFile) elem).getType();
        } catch (Exception e) {/**/
        }
      }
    }

    return null;
  }

  public static IJavaElement findSelectedJavaElement(ISelection selection) {
    if (selection != null) {
      if (selection instanceof IStructuredSelection) {
        return findSelectedJavaElement((IStructuredSelection) selection);
      } else if (selection instanceof ITextSelection) { return findSelectedJavaElement((ITextSelection) selection); }
    }

    return null;
  }

  public static IJavaElement findSelectedJavaElement(IStructuredSelection selection) {
    if (selection != null) {
      Object obj = selection.getFirstElement();

      if (obj instanceof IJavaElement) { return (IJavaElement) obj; }
    }

    return null;
  }

  public static IJavaElement findSelectedJavaElement(ITextSelection selection) {
    if (selection != null) {
      ITextEditor editor = findSelectedTextEditor();

      if (editor != null) {
        IEditorInput input = editor.getEditorInput();

        if (input instanceof IFileEditorInput) {
          IFile file = ((IFileEditorInput) input).getFile();

          if (TcPlugin.getDefault().hasTerracottaNature(file.getProject())) {
            try {
              IJavaElement[] elems = JdtUtils.codeResolve(editor);
              return elems.length == 1 ? elems[0] : null;
              // return JdtUtils.getElementAtOffset(editor);
            } catch (JavaModelException jme) {/**/
            }
          }
        } else {
          IClassFile classFile = (IClassFile) input.getAdapter(IClassFile.class);

          if (classFile != null) {
            IJavaProject javaProj = classFile.getJavaProject();

            if (TcPlugin.getDefault().hasTerracottaNature(javaProj.getProject())) {
              try {
                IJavaElement[] elems = JdtUtils.codeResolve(editor);
                return elems.length == 1 ? elems[0] : null;
                // return JdtUtils.getElementAtOffset(editor);
              } catch (JavaModelException jme) {/**/
              }
            }
          }
        }
      }
    }

    return null;
  }

  public static String getStatusMessages(Throwable throwable) {
    if (throwable instanceof InvocationTargetException) {
      throwable = ((InvocationTargetException) throwable).getCause();
    }

    String msg = throwable.getMessage();

    if (throwable instanceof CoreException) {
      CoreException ce = (CoreException) throwable;
      IStatus status = ce.getStatus();
      IStatus[] children = status.getChildren();

      if (children.length > 0) {
        msg += "\n";
        for (int i = 0; i < children.length; i++) {
          msg += "\n" + children[i].getMessage();
        }
      }
    }

    return msg;
  }
}
