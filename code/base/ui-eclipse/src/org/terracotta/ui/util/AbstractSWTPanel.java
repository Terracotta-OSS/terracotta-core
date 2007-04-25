/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.util;

import org.eclipse.swt.widgets.Composite;

public abstract class AbstractSWTPanel extends Composite implements SWTComponentModel {

  protected volatile boolean m_isActive;

  public AbstractSWTPanel(Composite parent, int style) {
    super(parent, style);
  }

  public synchronized boolean isActive() {
    return m_isActive;
  }

  public synchronized void setActive(boolean activate) {
    m_isActive = activate;
  }
}
