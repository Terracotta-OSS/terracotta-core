/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.BaseHelper;
import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XTreeNode;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public class RootsHelper extends BaseHelper {
  private static RootsHelper m_helper = new RootsHelper();
  private Icon               m_rootsIcon;
  private Icon               m_rootIcon;
  private Icon               m_fieldIcon;
  private Icon               m_cycleIcon;
  
  public static RootsHelper getHelper() {
    return m_helper;
  }

  public Icon getRootsIcon() {
    if(m_rootsIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"hierarchicalLayout.gif");
      
      if(url != null) {
        m_rootsIcon = new ImageIcon(url);
      }
    }

    return m_rootsIcon;
  }

  public Icon getRootIcon() {
    if(m_rootIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"genericvariable_obj.gif");
      
      if(url != null) {
        m_rootIcon = new ImageIcon(url);
      }
    }

    return m_rootIcon;
  }

  public Icon getFieldIcon() {
    if(m_fieldIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"field_protected_obj.gif");
      
      if(url != null) {
        m_fieldIcon = new ImageIcon(url);
      }
    }

    return m_fieldIcon;
  }

  public Icon getCycleIcon() {
    if(m_cycleIcon == null) {
      URL url = getClass().getResource(ICONS_PATH+"obj_cycle.gif");
      
      if(url != null) {
        m_cycleIcon = new ImageIcon(url);
      }
    }

    return m_cycleIcon;
  }

  public DSORoot[] getRoots(ConnectionContext cc)
    throws MBeanException,
           AttributeNotFoundException,
           InstanceNotFoundException,
           ReflectionException,
           IOException
  {
    ObjectName[] rootNames = getRootNames(cc);
    int          count     = (rootNames != null) ? rootNames.length : 0;
    DSORoot[]    result    = new DSORoot[count];

    for(int i = 0; i < count; i++) {
      result[i] = new DSORoot(cc, rootNames[i]);
    }

    return result;
  }

  public ObjectName[] getRootNames(ConnectionContext cc)
    throws MBeanException,
           AttributeNotFoundException,
           InstanceNotFoundException,
           ReflectionException,
           IOException
  {
    ObjectName dso = DSOHelper.getHelper().getDSOMBean(cc);
    return (ObjectName[])cc.getAttribute(dso, "Roots");
  }

  public String[] trimFields(String[] fields) {
    if(fields != null && fields.length > 0) {
      ArrayList list = new ArrayList();
      String    field;

      for(int i = 0; i < fields.length; i++) {
        field = fields[i];

        if(!field.startsWith("this$")) {
          list.add(field);
        }
      }

      return (String[])list.toArray(new String[0]);
    }

    return new String[]{};
  }

  public XTreeNode createFieldNode(ConnectionContext cc, DSOObject field) {
    if(field instanceof DSOMapEntryField) {
      return new MapEntryNode(cc, (DSOMapEntryField)field);
    }

    if(field instanceof DSOField) {
      return new FieldTreeNode(cc, (DSOField)field);
    }

    return new XTreeNode("NoSuchObject");
  }
}
