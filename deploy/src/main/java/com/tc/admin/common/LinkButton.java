/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.Border;

public class LinkButton {
  private static final Color  LINK_COLOR   = Color.blue;
  private static final Border LINK_BORDER  = BorderFactory.createEmptyBorder(0, 0, 1, 0);
  private static final Border HOVER_BORDER = BorderFactory.createMatteBorder(0, 0, 1, 0, LINK_COLOR);

  private static class LinkMouseListener extends MouseAdapter {
    public void mouseEntered(MouseEvent e) {
      ((JComponent) e.getComponent()).setBorder(HOVER_BORDER);
    }

    public void mouseReleased(MouseEvent e) {
      ((JComponent) e.getComponent()).setBorder(LINK_BORDER);
    }

    public void mouseExited(MouseEvent e) {
      ((JComponent) e.getComponent()).setBorder(LINK_BORDER);
    }
  }

  public static JButton makeLink(String text, ActionListener listener) {
    JButton button = new JButton(text);
    button.addActionListener(listener);
    return makeLink(button);
  }
  
  public static JButton makeLink(JButton button) {
    button.setBorder(LINK_BORDER);
    button.setForeground(LINK_COLOR);
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    button.setFocusPainted(false);
    button.setRequestFocusEnabled(false);
    button.setContentAreaFilled(false);
    button.addMouseListener(new LinkMouseListener());
    return button;
  }
}
