/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

public class ArrowIcon implements Icon, SwingConstants {
  private int   direction;
  private Color shadow;

  public ArrowIcon(int direction, Color shadow) {
    setDirection(direction);
    this.shadow = shadow;
  }

  public ArrowIcon() {
    this(SOUTH);
  }

  public ArrowIcon(int direction) {
    this(direction, UIManager.getColor("controlShadow"));
  }

  public int getDirection() {
    return direction;
  }

  public void setDirection(int dir) {
    direction = dir;
  }

  public int getIconWidth() {
    return 12;
  }

  public int getIconHeight() {
    return 12;
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    int w = getIconWidth();
    int h = getIconHeight();

    int size = Math.max(Math.min(h / 3, w / 3), 2);

    paintTriangle(g, x + (w - size) / 2, y + (h - size) / 2, size);
  }

  public void paintTriangle(Graphics g, int x, int y, int size) {
    Color oldColor = g.getColor();
    int mid = (size / 2) - 1;
    int j = 0;
    int i;

    g.translate(x, y);
    g.setColor(shadow);

    switch (direction) {
      case NORTH:
        for (i = 0; i < size; i++) {
          g.drawLine(mid - i, i, mid + i, i);
        }
        break;
      case SOUTH:
        j = 0;
        for (i = size - 1; i >= 0; i--) {
          g.drawLine(mid - i, j, mid + i, j);
          j++;
        }
        break;
      case WEST:
        for (i = 0; i < size; i++) {
          g.drawLine(i, mid - i, i, mid + i);
        }
        break;
      case EAST:
        j = 0;
        for (i = size - 1; i >= 0; i--) {
          g.drawLine(j, mid - i, j, mid + i);
          j++;
        }
        break;
    }

    g.translate(-x, -y);
    g.setColor(oldColor);
  }
}
