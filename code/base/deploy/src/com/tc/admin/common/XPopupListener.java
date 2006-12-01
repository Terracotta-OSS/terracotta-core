/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 * A utility helper for components that want to have a context menu.
 */

public class XPopupListener extends MouseAdapter {
  protected JComponent m_target;
  protected JPopupMenu m_popupMenu;

  public XPopupListener() {
    super();
  }
  
  public XPopupListener(JComponent target) {
    this();
    m_target = target;
  }
  
  public void setTarget(JComponent target) {
    if(m_target != null) {
      if(m_popupMenu != null) {
        m_target.removeMouseListener(this);
        m_target.remove(m_popupMenu);
      }
    }
    
    if((m_target = target) != null) {
      if(m_popupMenu != null) {
        m_target.add(m_popupMenu);
        m_target.addMouseListener(this);
      }
    }
  }
  
  public void mousePressed(MouseEvent e) {
    testPopup(e);
  }

  public void mouseReleased(MouseEvent e) {
    testPopup(e);
  }

  public void testPopup(MouseEvent e) {
    if(e.isPopupTrigger()) {
      m_popupMenu.show(m_target, e.getX(), e.getY());
    }
  }

  public void setPopupMenu(JPopupMenu popupMenu) {
    if(m_popupMenu != null) {
      m_target.removeMouseListener(this);
      m_target.remove(m_popupMenu);
    }

    if((m_popupMenu = popupMenu) != null) {
      m_target.add(popupMenu);
      m_target.addMouseListener(this);
    }
  }

  public JPopupMenu getPopupMenu() {
    return m_popupMenu;
  }
}
