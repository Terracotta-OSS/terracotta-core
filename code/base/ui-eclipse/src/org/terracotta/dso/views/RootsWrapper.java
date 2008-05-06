/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;

public class RootsWrapper {
  private DsoApplication fApp;
  private RootWrapper[] children;
  
  RootsWrapper(DsoApplication app) {
    fApp = app;
  }
  
  RootWrapper[] createRootWrappers() {
    int count = sizeOfRootArray();

    children = new RootWrapper[count];
    for(int i = 0; i < count; i++) {
      children[i] = new RootWrapper(this, i);
    }
    
    return children;
  }
  
  RootWrapper[] getChildren() {
    return children;
  }
  
  RootWrapper getChildAt(int index) {
    return children != null ? children[index] : null;
  }
  
  int sizeOfRootArray() {
    Roots roots = fApp.getRoots();
    return roots != null ? roots.sizeOfRootArray() : 0;
  }
  
  Root getRootArray(int i) {
    Roots roots = fApp.getRoots();
    if(roots != null) {
      return roots.getRootArray(i);
    }
    return null;
  }
  
  void removeRoot(int i) {
    Roots roots = fApp.getRoots();
    if(roots != null) {
      roots.removeRoot(i);
    }
    if(sizeOfRootArray() == 0) {
      fApp.unsetRoots();
    }
  }
}
