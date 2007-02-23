/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.views;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.terracottatech.config.Application;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.TcConfigDocument.TcConfig;

public class ConfigContentProvider implements ITreeContentProvider {
  ConfigViewPart fPart;
  
  ConfigContentProvider(ConfigViewPart fPart) {
    this.fPart = fPart;
  }
  
  public Object[] getChildren(Object parentElement) {
    if(parentElement instanceof RootsWrapper) {
      return createRootWrappers((RootsWrapper)parentElement);
    } else if(parentElement instanceof TransientFieldsWrapper) {
      return createTransientFieldWrappers((TransientFieldsWrapper)parentElement);
    } else if(parentElement instanceof AdditionalBootJarClassesWrapper) {
      return createBootClassWrappers((AdditionalBootJarClassesWrapper)parentElement);
    } else if(parentElement instanceof DistributedMethodsWrapper) {
      return createDistributedMethodWrappers((DistributedMethodsWrapper)parentElement);
    } else if(parentElement instanceof LocksWrapper) {
      LocksWrapper locks = (LocksWrapper)parentElement;
      return new Object[] {new AutolocksWrapper(locks), new NamedLocksWrapper(locks)};
    } else if(parentElement instanceof InstrumentedClassesWrapper) {
      InstrumentedClassesWrapper ic = ((InstrumentedClassesWrapper)parentElement);
      return new Object[] {new IncludesWrapper(ic), new ExcludesWrapper(ic)};
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

  static DistributedMethodWrapper[] createDistributedMethodWrappers(DistributedMethodsWrapper distributedMethods) {
    int count = distributedMethods.sizeOfMethodExpressionArray();
    DistributedMethodWrapper[] result = new DistributedMethodWrapper[count];
    
    for(int i = 0; i < count; i++) {
      result[i] = new DistributedMethodWrapper(distributedMethods, i);
    }
    
    return result;
  }

  static RootWrapper[] createRootWrappers(RootsWrapper rootsWrapper) {
    int count = rootsWrapper.sizeOfRootArray();
    RootWrapper[] result = new RootWrapper[count];
    
    for(int i = 0; i < count; i++) {
      result[i] = new RootWrapper(rootsWrapper, i);
    }
    
    return result;
  }

  static TransientFieldWrapper[] createTransientFieldWrappers(TransientFieldsWrapper transientFields) {
    int count = transientFields.sizeOfFieldNameArray();
    TransientFieldWrapper[] result = new TransientFieldWrapper[count];
    
    for(int i = 0; i < count; i++) {
      result[i] = new TransientFieldWrapper(transientFields, i);
    }
    
    return result;
  }

  static BootClassWrapper[] createBootClassWrappers(AdditionalBootJarClassesWrapper abjc) {
    int count = abjc.sizeOfIncludeArray();
    BootClassWrapper[] result = new BootClassWrapper[count];
    
    for(int i = 0; i < count; i++) {
      result[i] = new BootClassWrapper(abjc, i);
    }
    
    return result;
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
        DsoApplication dsoApp = app.getDso();
        
        return new Object[] {
            new RootsWrapper(dsoApp),
            new LocksWrapper(dsoApp),
            new TransientFieldsWrapper(dsoApp),
            new InstrumentedClassesWrapper(dsoApp),
            new DistributedMethodsWrapper(dsoApp),
            new AdditionalBootJarClassesWrapper(dsoApp)
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
