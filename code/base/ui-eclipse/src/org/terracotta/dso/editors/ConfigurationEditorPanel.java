/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.terracotta.dso.editors.xmlbeans.XmlConfigEvent;
import org.terracotta.ui.util.AbstractSWTPanel;

import com.tc.util.event.UpdateEvent;

public abstract class ConfigurationEditorPanel extends AbstractSWTPanel {

  public ConfigurationEditorPanel(Composite parent, int style) {
    super(parent, style);
    setLayout(new FillLayout());
  }

  protected final XmlConfigEvent castEvent(UpdateEvent e) {
    return (XmlConfigEvent) e;
  }

  protected abstract void refreshContent();
  
  protected abstract void detach();
   
}
