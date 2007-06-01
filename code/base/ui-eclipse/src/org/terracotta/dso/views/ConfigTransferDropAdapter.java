/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.SelectionTransferDropAdapter;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.terracotta.dso.TcPlugin;

import java.util.Iterator;
import java.util.List;

class ConfigTransferDropAdapter extends SelectionTransferDropAdapter {
  private static final int OPERATION = DND.DROP_LINK;
  private ConfigViewPart fPart;

  public ConfigTransferDropAdapter(ConfigViewPart viewPart, StructuredViewer viewer) {
    super(viewer);
    setFullWidthMatchesItem(false);
    fPart = viewPart;
  }

  public void validateDrop(Object target, DropTargetEvent event, int operation) {
    event.detail= DND.DROP_NONE;
    initializeSelection();
    
    IJavaElement[] inputElements = getInputElements(getSelection());
    if (inputElements != null && inputElements.length > 0) {
      switch(inputElements[0].getElementType()) {
        case IJavaElement.FIELD:
          if(target instanceof RootsWrapper ||
             target instanceof RootWrapper ||
             target instanceof TransientFieldsWrapper ||
             target instanceof TransientFieldWrapper) {
            event.detail = OPERATION;
          }
          break;
        case IJavaElement.PACKAGE_DECLARATION:
        case IJavaElement.PACKAGE_FRAGMENT:
          if(target instanceof AutolocksWrapper ||
             target instanceof AutolockWrapper ||
             target instanceof NamedLocksWrapper ||
             target instanceof NamedLockWrapper ||
             target instanceof IncludesWrapper ||
             target instanceof IncludeWrapper ||
             target instanceof ExcludesWrapper ||
             target instanceof ExcludeWrapper) {
            event.detail = OPERATION;
          }
          break;
        case IJavaElement.METHOD:
          if(target instanceof DistributedMethodsWrapper ||
             target instanceof DistributedMethodWrapper ||
             target instanceof AutolocksWrapper ||
             target instanceof AutolockWrapper ||
             target instanceof NamedLocksWrapper ||
             target instanceof NamedLockWrapper) {
            event.detail = OPERATION;
          }
          break;
        case IJavaElement.CLASS_FILE:
        case IJavaElement.COMPILATION_UNIT:
        case IJavaElement.TYPE:
          if(target instanceof AdditionalBootJarClassesWrapper ||
             target instanceof AutolocksWrapper ||
             target instanceof AutolockWrapper ||
             target instanceof NamedLocksWrapper ||
             target instanceof NamedLockWrapper ||
             target instanceof IncludesWrapper ||
             target instanceof IncludeWrapper ||
             target instanceof ExcludesWrapper ||
             target instanceof ExcludeWrapper) {
            event.detail = OPERATION;
          }
          break;
      }
    }
  }
  
  public boolean isEnabled(DropTargetEvent event) {
    return true;
  }

  public void drop(Object target, DropTargetEvent event) {
    List list = SelectionUtil.toList(getSelection());
    IProject project = fPart.m_javaProject.getProject();
    TcPlugin plugin = TcPlugin.getDefault();
    
    if(plugin.getConfiguration(project) == TcPlugin.BAD_CONFIG) {
      Shell  shell = Display.getDefault().getActiveShell();
      String title = "Terracotta Plugin";
      String msg   = "The configuration source is not parsable and cannot be\n used until these errors are resolved.";
      
      MessageDialog.openWarning(shell, title, msg);
      try {
        plugin.openConfigurationEditor(project);
      } catch(Exception e) {
        // TODO:
      }
      return;
    }

    IJavaElement element = (IJavaElement)list.get(0);
    switch(element.getElementType()) {
      case IJavaElement.FIELD:
        if(target instanceof RootsWrapper ||
           target instanceof RootWrapper) {
          fPart.addRoots((IField[])list.toArray(new IField[0]));
        } else if(target instanceof TransientFieldsWrapper ||
                  target instanceof TransientFieldWrapper) {
          fPart.addTransientFields((IField[])list.toArray(new IField[0]));
        }
        break;
      case IJavaElement.PACKAGE_DECLARATION:
      case IJavaElement.PACKAGE_FRAGMENT:
        if(target instanceof AutolocksWrapper ||
           target instanceof AutolockWrapper) {
          fPart.addAutolocks((IJavaElement[])list.toArray(new IJavaElement[0]));
        } else if(target instanceof NamedLocksWrapper ||
                  target instanceof NamedLockWrapper) {
          fPart.addNamedLocks((IJavaElement[])list.toArray(new IJavaElement[0]));
        } else if(target instanceof IncludesWrapper ||
                  target instanceof IncludeWrapper) {
          fPart.addIncludes((IJavaElement[])list.toArray(new IJavaElement[0]));
        } else if(target instanceof ExcludesWrapper ||
                  target instanceof ExcludeWrapper) {
          fPart.addExcludes((IJavaElement[])list.toArray(new IJavaElement[0]));
        }
        break;
      case IJavaElement.METHOD:
        if(target instanceof DistributedMethodsWrapper ||
           target instanceof DistributedMethodWrapper) {
          fPart.addDistributedMethods((IMethod[])list.toArray(new IMethod[0]));
        } else if(target instanceof AutolocksWrapper ||
                  target instanceof AutolockWrapper) {
          fPart.addAutolocks((IMethod[])list.toArray(new IMethod[0]));
        } else if(target instanceof NamedLocksWrapper ||
                  target instanceof NamedLockWrapper) {
          fPart.addNamedLocks((IMethod[])list.toArray(new IMethod[0]));
        }
        break;
      case IJavaElement.CLASS_FILE:
        try {
          if(target instanceof AdditionalBootJarClassesWrapper) {
            IType[] types = new IType[list.size()];
            for(int i = 0; i < list.size(); i++) {
              types[i] = ((IClassFile)list.get(i)).getType();
            }
            fPart.addAdditionalBootJarClasses(types);
          } else if(target instanceof AutolocksWrapper ||
                    target instanceof AutolockWrapper) {
            fPart.addAutolocks((IJavaElement[])list.toArray(new IJavaElement[0]));
          } else if(target instanceof NamedLocksWrapper ||
                    target instanceof NamedLockWrapper) {
            fPart.addNamedLocks((IJavaElement[])list.toArray(new IJavaElement[0]));
          } else if(target instanceof IncludesWrapper ||
                    target instanceof IncludeWrapper) {
            fPart.addIncludes((IJavaElement[])list.toArray(new IJavaElement[0]));
          } else if(target instanceof ExcludesWrapper ||
                    target instanceof ExcludeWrapper) {
            fPart.addExcludes((IJavaElement[])list.toArray(new IJavaElement[0]));
          }
        } catch(JavaModelException jme) {/**/}
        break;
      case IJavaElement.COMPILATION_UNIT:
        if(target instanceof AdditionalBootJarClassesWrapper) {
          IType[] types = new IType[list.size()];
          for(int i = 0; i < list.size(); i++) {
            types[i] = ((ICompilationUnit)list.get(i)).findPrimaryType();
          }
          fPart.addAdditionalBootJarClasses(types);
        } else if(target instanceof AutolocksWrapper ||
                  target instanceof AutolockWrapper) {
          fPart.addAutolocks((IJavaElement[])list.toArray(new IJavaElement[0]));
        } else if(target instanceof NamedLocksWrapper ||
                  target instanceof NamedLockWrapper) {
          fPart.addNamedLocks((IJavaElement[])list.toArray(new IJavaElement[0]));
        } else if(target instanceof IncludesWrapper ||
                  target instanceof IncludeWrapper) {
          fPart.addIncludes((IJavaElement[])list.toArray(new IJavaElement[0]));
        } else if(target instanceof ExcludesWrapper ||
                  target instanceof ExcludeWrapper) {
          fPart.addExcludes((IJavaElement[])list.toArray(new IJavaElement[0]));
        }
        break;
      case IJavaElement.TYPE:
        if(target instanceof AdditionalBootJarClassesWrapper) {
          fPart.addAdditionalBootJarClasses((IType[])list.toArray(new IType[0]));
        } else if(target instanceof AutolocksWrapper ||
                  target instanceof AutolockWrapper) {
          fPart.addAutolocks((IType[])list.toArray(new IType[0]));
        } else if(target instanceof NamedLocksWrapper ||
                  target instanceof NamedLockWrapper) {
          fPart.addNamedLocks((IType[])list.toArray(new IType[0]));
        } else if(target instanceof IncludesWrapper ||
                  target instanceof IncludeWrapper) {
          fPart.addIncludes((IType[])list.toArray(new IType[0]));
        } else if(target instanceof ExcludesWrapper ||
                  target instanceof ExcludeWrapper) {
          fPart.addExcludes((IType[])list.toArray(new IType[0]));
        }
        break;
    }
  }
  
  private static IJavaElement[] getInputElements(ISelection selection) {
    List list = SelectionUtil.toList(selection);
    if (list == null)
      return null;
    return getCandidates(list);
  }

  public static IJavaElement[] getCandidates(List input) {
    Iterator iter = input.iterator();
    int type = -1;
    
    while(iter.hasNext()) {
      Object element = iter.next();
      if(!(element instanceof IJavaElement)) {
        return null;
      }
      IJavaElement javaElement = (IJavaElement)element;
      int elementType = javaElement.getElementType();
      if(type == -1) {
        type = elementType;
      } else if(type != elementType) {
        return null;
      }
    }
    
    return (IJavaElement[])input.toArray(new IJavaElement[0]);
  }
}
