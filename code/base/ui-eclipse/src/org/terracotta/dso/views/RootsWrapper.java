/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Root;
import com.terracottatech.config.Roots;

public class RootsWrapper {
  private DsoApplication fApp;
  
  RootsWrapper(DsoApplication app) {
    fApp = app;
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
