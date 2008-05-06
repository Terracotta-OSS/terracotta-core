/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.terracottatech.config.Application;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.TcConfigDocument.TcConfig;

public class ConfigContentProvider implements ITreeContentProvider {
  RootsWrapper rootsWrapper;
  LocksWrapper locksWrapper;
  AutolocksWrapper autolocksWrapper;
  NamedLocksWrapper namedLocksWrapper;
  TransientFieldsWrapper transientFieldsWrapper;
  InstrumentedClassesWrapper instrumentedClassesWrapper;
  IncludesWrapper includesWrapper;
  ExcludesWrapper excludesWrapper;
  DistributedMethodsWrapper distributedMethodsWrapper;
  AdditionalBootJarClassesWrapper additionalBootJarClassesWrapper;
  
  public Object[] getChildren(Object parentElement) {
    if(parentElement instanceof RootsWrapper) {
      return ((RootsWrapper)parentElement).createRootWrappers();
    } else if(parentElement instanceof TransientFieldsWrapper) {
      return ((TransientFieldsWrapper)parentElement).createTransientFieldWrappers();
    } else if(parentElement instanceof AdditionalBootJarClassesWrapper) {
      return ((AdditionalBootJarClassesWrapper)parentElement).createBootClassWrappers();
    } else if(parentElement instanceof DistributedMethodsWrapper) {
      return ((DistributedMethodsWrapper)parentElement).createDistributedMethodWrappers();
    } else if(parentElement instanceof LocksWrapper) {
      LocksWrapper locks = (LocksWrapper)parentElement;
      return new Object[] {
        autolocksWrapper = new AutolocksWrapper(locks), 
        namedLocksWrapper = new NamedLocksWrapper(locks)};
    } else if(parentElement instanceof InstrumentedClassesWrapper) {
      InstrumentedClassesWrapper ic = ((InstrumentedClassesWrapper)parentElement);
      return new Object[] {
        includesWrapper = new IncludesWrapper(ic),
        excludesWrapper = new ExcludesWrapper(ic)};
    } else if(parentElement instanceof AutolocksWrapper) {
      return ((AutolocksWrapper)parentElement).createAutolockWrappers();
    } else if(parentElement instanceof NamedLocksWrapper) {
      return ((NamedLocksWrapper)parentElement).createNamedLockWrappers();
    } else if(parentElement instanceof IncludesWrapper) {
      return ((IncludesWrapper)parentElement).createIncludeWrappers();
    } else if(parentElement instanceof ExcludesWrapper) {
      return ((ExcludesWrapper)parentElement).createExcludeWrappers();
    }
    
    return null;
  }

  public Object getParent(Object element) {
    return null;
  }

  public boolean hasChildren(Object element) {
    return element instanceof TreeRoot ||
           element instanceof RootsWrapper && ((RootsWrapper)element).sizeOfRootArray() > 0 ||
           element instanceof LocksWrapper ||
           element instanceof AutolocksWrapper && ((AutolocksWrapper)element).sizeOfAutolockArray() > 0 ||
           element instanceof NamedLocksWrapper && ((NamedLocksWrapper)element).sizeOfNamedLockArray() > 0 ||
           element instanceof InstrumentedClassesWrapper ||
           element instanceof IncludesWrapper && ((IncludesWrapper)element).sizeOfIncludeArray() > 0 ||
           element instanceof ExcludesWrapper && ((ExcludesWrapper)element).sizeOfExcludeArray() > 0 ||
           element instanceof TransientFieldsWrapper && ((TransientFieldsWrapper)element).sizeOfFieldNameArray() > 0 ||
           element instanceof AdditionalBootJarClassesWrapper && ((AdditionalBootJarClassesWrapper)element).sizeOfIncludeArray() > 0 ||
           element instanceof DistributedMethodsWrapper && ((DistributedMethodsWrapper)element).sizeOfMethodExpressionArray() > 0;
  }

  public Object[] getElements(Object inputElement) {
    if(inputElement == TreeRoot.EMPTY_ROOT) {
      return new Object[0];
    } else if(inputElement instanceof TreeRoot) {
      TcConfig config = ((TreeRoot)inputElement).getRoot();
      if(config == null) {
        return new Object[0];
      } else {
        Application app = config.getApplication();
        if(app == null) app = config.addNewApplication();
        DsoApplication dsoApp = app.getDso();
        if(dsoApp == null) dsoApp = app.addNewDso();
        
        return new Object[] {
            rootsWrapper = new RootsWrapper(dsoApp),
            locksWrapper = new LocksWrapper(dsoApp),
            transientFieldsWrapper = new TransientFieldsWrapper(dsoApp),
            instrumentedClassesWrapper = new InstrumentedClassesWrapper(dsoApp),
            distributedMethodsWrapper = new DistributedMethodsWrapper(dsoApp),
            additionalBootJarClassesWrapper = new AdditionalBootJarClassesWrapper(dsoApp)
          };
      }
    }
    return null;
  }

  public void dispose() {
    /**/
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    /**/
  }
}
