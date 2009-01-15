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
  private boolean                ready;
  private boolean                armed;

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
    ready = isReady;
    WebAppLinkNodeRenderer walnr = (WebAppLinkNodeRenderer) getRenderer();
    walnr.setEnabled(isReady);
    setArmed(armed);
  }

  public boolean isReady() {
    return ready;
  }

  Color getColor() {
    Color c = null;
    if (isReady()) {
      c = isArmed() ? Color.red : Color.blue;
    }
    return c;
  }

  public void setArmed(boolean armed) {
    this.armed = armed;
    WebAppLinkNodeRenderer walnr = (WebAppLinkNodeRenderer) getRenderer();
    Color fg = getColor();
    walnr.setTextSelectionColor(fg);
    walnr.setTextNonSelectionColor(fg);
    nodeChanged();
  }

  public boolean isArmed() {
    return armed;
  }
}

class WebAppLinkNodeRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
  private WebAppLinkNode node;

  public WebAppLinkNodeRenderer(WebAppLinkNode node) {
    super();

    this.node = node;

    backgroundSelectionColor = getBackgroundNonSelectionColor();
    borderSelectionColor = null;
    textSelectionColor = node.getColor();
    textNonSelectionColor = textSelectionColor;
  }

  public void paint(Graphics g) {
    super.paint(g);

    if (node.isReady() && !node.isArmed()) {
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
