/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.tree.TreeCellRenderer;

public abstract class AbstractTreeCellRenderer implements TreeCellRenderer {
  protected boolean selected;
  protected boolean hasFocus;
  protected boolean drawDashedFocusIndicator;
  protected Color   treeBGColor;
  protected Color   focusBGColor;
  protected Icon    closedIcon;
  protected Icon    leafIcon;
  protected Icon    openIcon;
  protected Color   textSelectionColor;
  protected Color   textNonSelectionColor;
  protected Color   backgroundSelectionColor;
  protected Color   backgroundNonSelectionColor;
  protected Color   borderSelectionColor;

  public AbstractTreeCellRenderer() {
    setLeafIcon(UIManager.getIcon("Tree.leafIcon"));
    setClosedIcon(UIManager.getIcon("Tree.closedIcon"));
    setOpenIcon(UIManager.getIcon("Tree.openIcon"));

    setTextSelectionColor(UIManager.getColor("Tree.selectionForeground"));
    setTextNonSelectionColor(UIManager.getColor("Tree.textForeground"));
    setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
    setBackgroundNonSelectionColor(UIManager.getColor("Tree.textBackground"));
    setBorderSelectionColor(UIManager.getColor("Tree.selectionBorderColor"));

    Object value = UIManager.get("Tree.drawDashedFocusIndicator");
    drawDashedFocusIndicator = (value != null && ((Boolean) value).booleanValue());
  }

  public Icon getDefaultOpenIcon() {
    return UIManager.getIcon("Tree.openIcon");
  }

  public Icon getDefaultClosedIcon() {
    return UIManager.getIcon("Tree.closedIcon");
  }

  public Icon getDefaultLeafIcon() {
    return UIManager.getIcon("Tree.leafIcon");
  }

  public void setOpenIcon(Icon newIcon) {
    openIcon = newIcon;
  }

  public Icon getOpenIcon() {
    return openIcon;
  }

  public void setClosedIcon(Icon newIcon) {
    closedIcon = newIcon;
  }

  public Icon getClosedIcon() {
    return closedIcon;
  }

  public void setLeafIcon(Icon newIcon) {
    leafIcon = newIcon;
  }

  public Icon getLeafIcon() {
    return leafIcon;
  }

  public void setTextSelectionColor(Color newColor) {
    textSelectionColor = newColor;
  }

  public Color getTextSelectionColor() {
    return textSelectionColor;
  }

  public void setTextNonSelectionColor(Color newColor) {
    textNonSelectionColor = newColor;
  }

  public Color getTextNonSelectionColor() {
    return textNonSelectionColor;
  }

  public void setBackgroundSelectionColor(Color newColor) {
    backgroundSelectionColor = newColor;
  }

  public Color getBackgroundSelectionColor() {
    return backgroundSelectionColor;
  }

  public void setBackgroundNonSelectionColor(Color newColor) {
    backgroundNonSelectionColor = newColor;
  }

  public Color getBackgroundNonSelectionColor() {
    return backgroundNonSelectionColor;
  }

  public void setBorderSelectionColor(Color newColor) {
    borderSelectionColor = newColor;
  }

  public Color getBorderSelectionColor() {
    return borderSelectionColor;
  }

  public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                         boolean leaf, int row, boolean focused) {
    this.hasFocus = focused;

    JComponent comp = getComponent();
    Color c;

    if (sel) {
      comp.setForeground((c = getTextSelectionColor()) != null ? c : tree.getForeground());
      comp.setBackground((c = getBackgroundSelectionColor()) != null ? c : tree.getBackground());
    } else {
      comp.setForeground((c = getTextNonSelectionColor()) != null ? c : tree.getForeground());
      comp.setBackground((c = getBackgroundNonSelectionColor()) != null ? c : tree.getBackground());
    }

    comp.setFont(tree.getFont());
    comp.setEnabled(tree.isEnabled());

    selected = sel;

    setValue(tree, value, sel, expanded, leaf, row, focused);

    return comp;
  }

  public abstract JComponent getComponent();

  public abstract void setValue(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,
                                boolean focused);

  protected void paintFocus(Graphics g, int x, int y, int w, int h) {
    Color bsColor = getBorderSelectionColor();

    if (bsColor != null && (selected || !drawDashedFocusIndicator)) {
      g.setColor(bsColor);
      g.drawRect(x, y, w - 1, h - 1);
    }

    if (drawDashedFocusIndicator) {
      Color color;

      if (selected) {
        color = getBackgroundSelectionColor();
      } else if ((color = getBackgroundNonSelectionColor()) == null) {
        color = getComponent().getBackground();
      }

      if (treeBGColor != color) {
        treeBGColor = color;
        focusBGColor = new Color(~color.getRGB());
      }

      g.setColor(focusBGColor);
      BasicGraphicsUtils.drawDashedRect(g, x, y, w, h);
    }
  }
}
