/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.dso;

import com.tc.admin.ConnectionContext;
import com.tc.admin.common.XTreeNode;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

public class MapEntryNode extends XTreeNode implements DSOObjectTreeNode {
  protected ConnectionContext m_cc;
  protected DSOMapEntryField  m_field;
  protected XTreeNode         m_keyNode;
  protected XTreeNode         m_valueNode;

  public MapEntryNode(ConnectionContext cc, DSOMapEntryField field) {
    super(field);

    m_cc    = cc;
    m_field = field;
    
    initChildren();  
  }
  
  public DSOObject getDSOObject() {
    return m_field;
  }

  protected void initChildren() {
    RootsHelper helper = RootsHelper.getHelper();
    DSOObject   key    = m_field.getKey();
    DSOObject   value  = m_field.getValue();
    
    add(helper.createFieldNode(m_cc, key));
    
    XTreeNode valueNode;
    if(value != null) {
      valueNode = helper.createFieldNode(m_cc, value);
    } else {
      valueNode = new XTreeNode("value=null");
    }
    add(valueNode);
    
    if(key == null) {
      SwingUtilities.invokeLater(new AncestorReaper());
    }
  }

  class AncestorReaper implements Runnable {
    public void run() {
      XTreeNode node = (XTreeNode)getParent();

      while(node != null) {
        if(node instanceof FieldTreeNode) {
          FieldTreeNode ftn = (FieldTreeNode)node;

          if(ftn.getField().isValid()) {
            ftn.refreshChildren();
            return;
          }
        }
        else if(node instanceof RootTreeNode) {
          ((RootTreeNode)node).refresh();
          return;
        }

        node = (XTreeNode)node.getParent();
      }
    }
  }

  public Icon getIcon() {
    return RootsHelper.getHelper().getFieldIcon();
  }

  public void tearDown() {
    super.tearDown();

    m_cc        = null;
    m_field     = null;
    m_keyNode   = null;
    m_valueNode = null;
  }
}


