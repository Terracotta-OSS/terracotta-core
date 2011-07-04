/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IActionFilter;

import org.terracotta.dso.JavaElementActionFilter;

class JavaElementAdapter implements IAdapterFactory {
  private IActionFilter m_actionFilter = new JavaElementActionFilter();

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if(IActionFilter.class.equals(adapterType)) {
      return m_actionFilter;
    }
    return null;
  }

  public Class[] getAdapterList() {
    return new Class[] {IActionFilter.class};
  }
}

