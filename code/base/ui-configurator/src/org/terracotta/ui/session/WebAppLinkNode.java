/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import com.tc.admin.common.XTreeNode;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

public class WebAppLinkNode extends XTreeNode {
  private boolean                m_ready;
  private boolean                m_armed;

  private static final ImageIcon ICON = new ImageIcon(WebAppLinkNode.class
                                          .getResource("/com/tc/admin/icons/occ_match.gif"));

  public WebAppLinkNode(String path) {
    super(path);
    setRenderer(new WebAppLinkNodeRenderer(this));
    setIcon(ICON);
    setReady(false);
  }

  public String getLink() {
    return (String) getUserObject();
  }

  public void setReady(boolean isReady) {
    m_ready = isReady;
    WebAppLinkNodeRenderer walnr = (WebAppLinkNodeRenderer) getRenderer();
    walnr.setEnabled(isReady);
    setArmed(m_armed);
  }

  public boolean isReady() {
    return m_ready;
  }

  Color getColor() {
    Color c = null;
    if (isReady()) {
      c = isArmed() ? Color.red : Color.blue;
    }
    return c;
  }

  public void setArmed(boolean armed) {
    m_armed = armed;
    WebAppLinkNodeRenderer walnr = (WebAppLinkNodeRenderer) getRenderer();
    Color fg = getColor();
    walnr.setTextSelectionColor(fg);
    walnr.setTextNonSelectionColor(fg);
    nodeChanged();
  }

  public boolean isArmed() {
    return m_armed;
  }
}

class WebAppLinkNodeRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
  private WebAppLinkNode m_node;

  public WebAppLinkNodeRenderer(WebAppLinkNode node) {
    super();

    m_node = node;

    // drawDashedFocusIndicator = false;
    backgroundSelectionColor = getBackgroundNonSelectionColor();
    borderSelectionColor = null;
    textSelectionColor = node.getColor();
    textNonSelectionColor = textSelectionColor;
  }

  public void paint(Graphics g) {
    super.paint(g);

    if (m_node.isReady() && !m_node.isArmed()) {
      FontMetrics fm = g.getFontMetrics();
      String text = getText();
      int x = getLabelStart();
      int y = getHeight();
      int w = SwingUtilities.computeStringWidth(fm, text);
      int h = 1;

      g.fillRect(x, y - 1, w, h);
    }
  }

  private int getLabelStart() {
    Icon currentI = getIcon();
    if (currentI != null && getText() != null) { return currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1); }
    return 0;
  }
}
