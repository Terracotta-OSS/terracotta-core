/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.PlatformUI;

import org.dijon.Container;

import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;

import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class ConfigurationEditorPanel extends Container {
  private transient ArrayList                     m_listenerList; 
  private transient XmlObjectStructureChangeEvent m_changeEvent;
  
  public ConfigurationEditorPanel() {
    super();
  }

  public static void ensureXmlObject(JComponent comp) {
    ConfigurationEditorPanel parent = (ConfigurationEditorPanel)
      SwingUtilities.getAncestorOfClass(ConfigurationEditorPanel.class, comp);
  
    if(parent != null) {
      parent.ensureXmlObject();
    }
  }
  
  public void ensureXmlObject() {
    ConfigurationEditorPanel parent = (ConfigurationEditorPanel)
      SwingUtilities.getAncestorOfClass(ConfigurationEditorPanel.class, this);
    
    if(parent != null) {
      parent.ensureXmlObject();
    }
  }

  public void setDirty() {
    ConfigurationEditorRoot editorRoot = getConfigurationEditorRoot();
    final IProject          project    = editorRoot.getProject();

    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
      public void run(){
        TcPlugin            plugin = TcPlugin.getDefault();
        ConfigurationEditor editor = plugin.getConfigurationEditor(project);
    
        if(editor != null) {
          editor._setDirty();
        }
      }
    });
  }

  /**
   * Retrieve our top-level Swing parent.
   */
  private ConfigurationEditorRoot getConfigurationEditorRoot() {
    if(this instanceof ConfigurationEditorRoot) {
      return (ConfigurationEditorRoot)this;
    }
    else {
      return (ConfigurationEditorRoot)getAncestorOfClass(ConfigurationEditorRoot.class);
    }
  }

  public synchronized void addXmlObjectStructureListener(XmlObjectStructureListener listener) {
    if(listener != null) {
      if(m_listenerList == null) {
        m_listenerList = new ArrayList();
      }
      m_listenerList.add(listener);
    }
  }

  public synchronized void removeXmlObjectStructureListener(XmlObjectStructureListener listener) {
    if(listener != null) {
      if(m_listenerList != null) {
        m_listenerList.remove(listener);
      }
    }
  }
  
  private XmlObjectStructureChangeEvent getChangeEvent(XmlObject source) {
    if(m_changeEvent == null) {
      m_changeEvent = new XmlObjectStructureChangeEvent(source);
    }
    else {
      m_changeEvent.setXmlObject(source);
    }

    return m_changeEvent;
  }
  
  private XmlObjectStructureListener[] getListenerArray() {
    return (XmlObjectStructureListener[])
      m_listenerList.toArray(new XmlObjectStructureListener[0]);
  }
    
  protected void fireXmlObjectStructureChanged(XmlObjectStructureChangeEvent e) {
    if(m_listenerList != null) {
      XmlObjectStructureListener[] listeners = getListenerArray();
      
      for(int i = 0; i < listeners.length; i++) {
        listeners[i].structureChanged(e);
      }
    }
  }
  
  protected void fireXmlObjectStructureChanged(XmlObject source) {
    fireXmlObjectStructureChanged(getChangeEvent(source));
  }
  
  protected int parseInt(String s) {
    try {
      return Integer.parseInt(s);
    } catch(Exception e) {return 0;}
  }
}
