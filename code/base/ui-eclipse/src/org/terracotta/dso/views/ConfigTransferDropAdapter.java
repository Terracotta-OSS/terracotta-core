/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.packageview.SelectionTransferDropAdapter;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;

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
    
    IJavaElement inputElement = getInputElement(getSelection());
    if (inputElement != null) {
      switch(inputElement.getElementType()) {
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
    IJavaElement input = getInputElement(getSelection());
    
    switch(input.getElementType()) {
      case IJavaElement.FIELD:
        if(target instanceof RootsWrapper ||
           target instanceof RootWrapper) {
          fPart.addRoot((IField)input);
        } else if(target instanceof TransientFieldsWrapper ||
                  target instanceof TransientFieldWrapper) {
          fPart.addTransientField((IField)input);
        }
        break;
      case IJavaElement.PACKAGE_DECLARATION:
      case IJavaElement.PACKAGE_FRAGMENT:
        if(target instanceof AutolocksWrapper ||
           target instanceof AutolockWrapper) {
          fPart.addAutolock(input);
        } else if(target instanceof NamedLocksWrapper ||
                  target instanceof NamedLockWrapper) {
          fPart.addNamedLock(input);
        } else if(target instanceof IncludesWrapper ||
                  target instanceof IncludeWrapper) {
          fPart.addInclude(input);
        } else if(target instanceof ExcludesWrapper ||
                  target instanceof ExcludeWrapper) {
          fPart.addExclude(input);
        }
        break;
      case IJavaElement.METHOD:
        if(target instanceof DistributedMethodsWrapper ||
           target instanceof DistributedMethodWrapper) {
          fPart.addDistributedMethod((IMethod)input);
        } else if(target instanceof AutolocksWrapper ||
                  target instanceof AutolockWrapper) {
          fPart.addAutolock(input);
        } else if(target instanceof NamedLocksWrapper ||
                  target instanceof NamedLockWrapper) {
          fPart.addNamedLock(input);
        }
        break;
      case IJavaElement.CLASS_FILE:
        try {
          if(target instanceof AdditionalBootJarClassesWrapper) {
            fPart.addAdditionalBootJarClass(((IClassFile)input).getType());
          } else if(target instanceof AutolocksWrapper ||
                    target instanceof AutolockWrapper) {
            fPart.addAutolock(input);
          } else if(target instanceof NamedLocksWrapper ||
                    target instanceof NamedLockWrapper) {
            fPart.addNamedLock(input);
          } else if(target instanceof IncludesWrapper ||
                    target instanceof IncludeWrapper) {
            fPart.addInclude(input);
          } else if(target instanceof ExcludesWrapper ||
                    target instanceof ExcludeWrapper) {
            fPart.addExclude(input);
          }
        } catch(JavaModelException jme) {/**/}
        break;
      case IJavaElement.COMPILATION_UNIT:
        if(target instanceof AdditionalBootJarClassesWrapper) {
          fPart.addAdditionalBootJarClass(((ICompilationUnit)input).findPrimaryType());
        } else if(target instanceof AutolocksWrapper ||
                  target instanceof AutolockWrapper) {
          fPart.addAutolock(input);
        } else if(target instanceof NamedLocksWrapper ||
                  target instanceof NamedLockWrapper) {
          fPart.addNamedLock(input);
        } else if(target instanceof IncludesWrapper ||
                  target instanceof IncludeWrapper) {
          fPart.addInclude(input);
        } else if(target instanceof ExcludesWrapper ||
                  target instanceof ExcludeWrapper) {
          fPart.addExclude(input);
        }
        break;
      case IJavaElement.TYPE:
        if(target instanceof AdditionalBootJarClassesWrapper) {
          fPart.addAdditionalBootJarClass((IType)input);
        } else if(target instanceof AutolocksWrapper ||
                  target instanceof AutolockWrapper) {
          fPart.addAutolock(input);
        } else if(target instanceof NamedLocksWrapper ||
                  target instanceof NamedLockWrapper) {
          fPart.addNamedLock(input);
        } else if(target instanceof IncludesWrapper ||
                  target instanceof IncludeWrapper) {
          fPart.addInclude(input);
        } else if(target instanceof ExcludesWrapper ||
                  target instanceof ExcludeWrapper) {
          fPart.addExclude(input);
        }
        break;
    }
  }
  
  private static IJavaElement getInputElement(ISelection selection) {
    Object single = SelectionUtil.getSingleElement(selection);
    if (single == null)
      return null;
    return getCandidate(single);
  }
    
  public static IJavaElement getCandidate(Object input) {
    if(!(input instanceof IJavaElement)) {
      return null;
    }
    return (IJavaElement)input;
  }
}
