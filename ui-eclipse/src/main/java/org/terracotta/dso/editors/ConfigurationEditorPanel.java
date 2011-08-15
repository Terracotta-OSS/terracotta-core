/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors;

import org.apache.xmlbeans.SchemaProperty;
import org.apache.xmlbeans.SchemaStringEnumEntry;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.terracotta.dso.IConfigurationListener;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.editors.xmlbeans.XmlBooleanToggle;
import org.terracotta.dso.editors.xmlbeans.XmlIntegerField;
import org.terracotta.dso.editors.xmlbeans.XmlIntegerSpinner;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureChangeEvent;
import org.terracotta.dso.editors.xmlbeans.XmlObjectStructureListener;
import org.terracotta.dso.editors.xmlbeans.XmlStringEnumCombo;
import org.terracotta.dso.editors.xmlbeans.XmlStringField;
import org.terracotta.ui.util.AbstractSWTPanel;
import org.terracotta.ui.util.SWTUtil;

import java.util.ArrayList;

import javax.xml.namespace.QName;

public abstract class ConfigurationEditorPanel extends AbstractSWTPanel
  implements IConfigurationListener,
             DisposeListener
{
  private transient ArrayList<XmlObjectStructureListener> m_listenerList; 
  private transient XmlObjectStructureChangeEvent m_changeEvent;

  public ConfigurationEditorPanel(Composite parent, int style) {
    super(parent, style);
    setLayout(new FillLayout());
    addDisposeListener(this);
    TcPlugin.getDefault().addConfigurationListener(this);
  }

  public static void ensureXmlObject(Composite comp) {
    ConfigurationEditorPanel parent = (ConfigurationEditorPanel)
      SWTUtil.getAncestorOfClass(ConfigurationEditorPanel.class, comp);
  
    if(parent != null) {
      parent.ensureXmlObject();
    }
  }
  
  public void ensureXmlObject() {
    ConfigurationEditorPanel parent = (ConfigurationEditorPanel)
      SWTUtil.getAncestorOfClass(ConfigurationEditorPanel.class, this);
    
    if(parent != null) {
      parent.ensureXmlObject();
    }
  }

  private ConfigurationEditorRoot getConfigurationEditorRoot() {
    if(this instanceof ConfigurationEditorRoot) {
      return (ConfigurationEditorRoot)this;
    }
    else {
      return (ConfigurationEditorRoot)SWTUtil.getAncestorOfClass(ConfigurationEditorRoot.class, this);
    }
  }

  public IProject getProject() {
    return getConfigurationEditorRoot().getProject();
  }
  
  public synchronized void addXmlObjectStructureListener(XmlObjectStructureListener listener) {
    if(listener != null) {
      if(m_listenerList == null) {
        m_listenerList = new ArrayList<XmlObjectStructureListener>();
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
    return m_listenerList.toArray(new XmlObjectStructureListener[0]);
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
  
  private static SchemaType getParentSchemaType(Class parentType) throws Exception {
    return (SchemaType) parentType.getField("type").get(null);
  }

  private static SchemaProperty getSchemaProperty(Class parentType, String elementName) throws Exception {
    QName qname = QName.valueOf(elementName);
    SchemaType type = getParentSchemaType(parentType);
    SchemaProperty property = type.getElementProperty(qname);
    if (property == null) property = type.getAttributeProperty(qname);
    return property;
  }

  private static SchemaType getPropertySchemaType(Class parentType, String elementName) throws Exception {
    return getSchemaProperty(parentType, elementName).getType();
  }

  static String[] getListDefaults(Class parentType, String elementName) {
    try {
      SchemaStringEnumEntry[] enumEntries = getPropertySchemaType(parentType, elementName).getStringEnumEntries();
      String[] values = new String[enumEntries.length];
      for (int i = 0; i < enumEntries.length; i++) {
        values[i] = enumEntries[i].getString();
      }
      return values;
    } catch (Exception e) {
      return new String[0];
    }
  }
  
  void initStringField(Text field, Class parentType, String fieldName) {
    XmlStringField xmlField = XmlStringField.newInstance(field); 
    field.setData(xmlField);
    xmlField.init(parentType, fieldName);
  }

  void setStringField(Text text, String value) {
    assert(text != null);
    text.setText(value);
    text.selectAll();
    text.forceFocus();
    Object data = text.getData();
    if(data instanceof XmlStringField) {
      ((XmlStringField) data).set();
    }
  }
  
  void initIntegerField(Text field, Class parentType, String fieldName) {
    XmlIntegerField xmlField = XmlIntegerField.newInstance(field); 
    field.setData(xmlField);
    xmlField.init(parentType, fieldName);
  }

  void initBooleanField(Button field, Class parentType, String fieldName) {
    XmlBooleanToggle xmlField = XmlBooleanToggle.newInstance(field); 
    field.setData(xmlField);
    xmlField.init(parentType, fieldName);
  }

  void initIntegerSpinnerField(Spinner field, Class parentType, String fieldName) {
    XmlIntegerSpinner xmlField = XmlIntegerSpinner.newInstance(field); 
    field.setData(xmlField);
    xmlField.init(parentType, fieldName);
  }
  
  void initStringEnumCombo(Combo field, Class parentType, String fieldName) {
    XmlStringEnumCombo xmlField = XmlStringEnumCombo.newInstance(field); 
    field.setData(xmlField);
    xmlField.init(parentType, fieldName);
  }

  public void widgetDisposed(DisposeEvent e) {
    TcPlugin.getDefault().removeConfigurationListener(this);
  }
  
  void fireServerChanged(int index) {
    TcPlugin.getDefault().fireServerChanged(getProject(), index);
  }

  void fireServersChanged() {
    TcPlugin.getDefault().fireServersChanged(getProject());
  }

  void fireRootChanged(int index) {
    TcPlugin.getDefault().fireRootChanged(getProject(), index);
  }

  void fireRootsChanged() {
    TcPlugin.getDefault().fireRootsChanged(getProject());
  }

  void fireDistributedMethodsChanged() {
    TcPlugin.getDefault().fireDistributedMethodsChanged(getProject());
  }

  void fireDistributedMethodChanged(int index) {
    TcPlugin.getDefault().fireDistributedMethodChanged(getProject(), index);
  }

  void fireBootClassesChanged() {
    TcPlugin.getDefault().fireBootClassesChanged(getProject());
  }

  void fireBootClassChanged(int index) {
    TcPlugin.getDefault().fireBootClassChanged(getProject(), index);
  }

  void fireTransientFieldsChanged() {
    TcPlugin.getDefault().fireTransientFieldsChanged(getProject());
  }

  void fireTransientFieldChanged(int index) {
    TcPlugin.getDefault().fireTransientFieldChanged(getProject(), index);
  }

  void fireNamedLockChanged(int index) {
    TcPlugin.getDefault().fireNamedLockChanged(getProject(), index);
  }

  void fireNamedLocksChanged() {
    TcPlugin.getDefault().fireNamedLocksChanged(getProject());
  }

  void fireAutolockChanged(int index) {
    try {
      TcPlugin.getDefault().fireAutolockChanged(getProject(), index);
    } catch(Throwable t) {
      t.printStackTrace();
    }
  }

  void fireAutolocksChanged() {
    TcPlugin.getDefault().fireAutolocksChanged(getProject());
  }

  void fireIncludeRuleChanged(int index) {
    TcPlugin.getDefault().fireIncludeRuleChanged(getProject(), index);
  }

  void fireIncludeRulesChanged() {
    TcPlugin.getDefault().fireIncludeRulesChanged(getProject());
  }

  void fireExcludeRuleChanged(int index) {
    TcPlugin.getDefault().fireExcludeRuleChanged(getProject(), index);
  }

  void fireExcludeRulesChanged() {
    TcPlugin.getDefault().fireExcludeRulesChanged(getProject());
  }

  void fireInstrumentationRulesChanged() {
    TcPlugin.getDefault().fireInstrumentationRulesChanged(getProject());
  }

  void fireClientChanged() {
    TcPlugin.getDefault().fireClientChanged(getProject());
  }

  void fireModuleRepoChanged(int index) {
    TcPlugin.getDefault().fireModuleRepoChanged(getProject(), index);
  }

  void fireModuleReposChanged() {
    TcPlugin.getDefault().fireModuleReposChanged(getProject());
  }

  void fireModuleChanged(int index) {
    TcPlugin.getDefault().fireModuleChanged(getProject(), index);
  }

  void fireModulesChanged() {
    TcPlugin.getDefault().fireModulesChanged(getProject());
  }

  // IConfigurationListener
  
  public void configurationChanged(IProject project) {/**/}

  public void serverChanged(IProject project, int index) {/**/}
  public void serversChanged(IProject project) {/**/}
    
  public void rootChanged(IProject project, int index) {/**/}
  public void rootsChanged(IProject project) {/**/}

  public void distributedMethodsChanged(IProject project) {/**/}
  public void distributedMethodChanged(IProject project, int index) {/**/}

  public void bootClassesChanged(IProject project) {/**/}
  public void bootClassChanged(IProject project, int index) {/**/}

  public void transientFieldsChanged(IProject project) {/**/}
  public void transientFieldChanged(IProject project, int index) {/**/}

  public void namedLockChanged(IProject project, int index) {/**/}
  public void namedLocksChanged(IProject project) {/**/}

  public void autolockChanged(IProject project, int index) {/**/}
  public void autolocksChanged(IProject project) {/**/}

  public void includeRuleChanged(IProject project, int index) {/**/}
  public void includeRulesChanged(IProject project) {/**/}
  public void excludeRuleChanged(IProject project, int index) {/**/}
  public void excludeRulesChanged(IProject project) {/**/}
  public void instrumentationRulesChanged(IProject project) {/**/}
    
  public void clientChanged(IProject project) {/**/}
  public void moduleReposChanged(IProject project) {/**/}
  public void moduleRepoChanged(IProject project, int index) {/**/}
  public void moduleChanged(IProject project, int index) {/**/}
  public void modulesChanged(IProject project) {/**/}
}
