/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import com.tc.admin.common.XTreeCellRenderer;
import com.tc.admin.common.XTreeNode;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

public class WebAppLinkNode extends XTreeNode {
  private static ImageIcon ICON;
  
  static {
    String uri = "/com/tc/admin/icons/occ_match.gif";
    URL    url = WebAppLinkNode.class.getResource(uri);
    
    if(url != null) {
      ICON = new ImageIcon(url);
    }
  }
  
  private boolean m_ready;
  private boolean m_armed;
  
  public WebAppLinkNode(String path) {
    super(path);

    setRenderer(new WebAppLinkNodeRenderer(this));
    setIcon(ICON);
    setReady(false);
  }
  
  public String getLink() {
    return (String)getUserObject();
  }
  
  public void setReady(boolean isReady) {
    m_ready = isReady;

    WebAppLinkNodeRenderer walnr = (WebAppLinkNodeRenderer)getRenderer();
    walnr.getComponent().setEnabled(isReady);
    setArmed(m_armed);
  }
  
  public boolean isReady() {
    return m_ready;
  }
  
  Color getColor() {
    Color c = null;
    
    if(isReady()) {
      c = isArmed() ? Color.red : Color.blue;
    }
    
    return c;
  }
  
  public void setArmed(boolean armed) {
    m_armed = armed;
    WebAppLinkNodeRenderer walnr = (WebAppLinkNodeRenderer)getRenderer();
    Color fg = getColor();
    walnr.setTextSelectionColor(fg);
    walnr.setTextNonSelectionColor(fg);
    nodeChanged();
  }
  
  public boolean isArmed() {
    return m_armed;
  }
}

class WebAppLinkNodeRenderer extends XTreeCellRenderer {
  private WebAppLinkNode m_node;
  
  public WebAppLinkNodeRenderer(WebAppLinkNode node) {
    super();
    
    m_node = node;
    
    drawDashedFocusIndicator = false;
    backgroundSelectionColor = getBackgroundNonSelectionColor();
    borderSelectionColor     = null;
    textSelectionColor       = node.getColor();
    textNonSelectionColor    = textSelectionColor;
  }

  protected Renderer createRenderer() {
    return new WebLinkRenderer(); 
  }
  
  class WebLinkRenderer extends Renderer {
    WebLinkRenderer() {
      super();
    }
    
    public void paint(Graphics g) {
      super.paint(g);
      
      if(m_node.isReady() && !m_node.isArmed()) {
        FontMetrics fm   = g.getFontMetrics();
        String      text = getText();
        int         x    = getLabelStart();
        int         y    = getHeight();
        int         w    = SwingUtilities.computeStringWidth(fm, text);
        int         h    = 1;
  
        g.fillRect(x, y - 1, w, h);
      }
    }
  }
}

