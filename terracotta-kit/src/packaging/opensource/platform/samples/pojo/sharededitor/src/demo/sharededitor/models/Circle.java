/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.sharededitor.models;

import demo.sharededitor.ui.Texturable;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RectangularShape;
import java.awt.Image;
import java.awt.Shape;

final class Circle extends BaseObject implements Texturable {
  private Ellipse2D.Double shape;

  protected Shape getShape() {
    return this.shape;
  }

  private transient Shape[] anchors = null;

  private Shape[] updateAnchors() {
    if (anchors == null) {
      anchors = new Shape[] {
          new Ellipse2D.Double(x1 - 5, y2 - 5, 10, 10),
          new Ellipse2D.Double(x2 - 5, y2 - 5, 10, 10),
          new Ellipse2D.Double(x2 - 5, y1 - 5, 10, 10),
          new Ellipse2D.Double(x1 - 5, y1 - 5, 10, 10) };
      return anchors;
    }

    ((Ellipse2D.Double) anchors[0]).x = x1 - 5;
    ((Ellipse2D.Double) anchors[0]).y = y2 - 5;
    ((Ellipse2D.Double) anchors[1]).x = x2 - 5;
    ((Ellipse2D.Double) anchors[1]).y = y2 - 5;
    ((Ellipse2D.Double) anchors[2]).x = x2 - 5;
    ((Ellipse2D.Double) anchors[2]).y = y1 - 5;
    ((Ellipse2D.Double) anchors[3]).x = x1 - 5;
    ((Ellipse2D.Double) anchors[3]).y = y1 - 5;
    return anchors;
  }

  protected Shape[] getAnchors() {
    return updateAnchors();
  }

  public void move(int dx, int dy) {
    synchronized (this) {
      x1 += dx;
      y1 += dy;
      x2 += dx;
      y2 += dy;
      shape.setFrameFromDiagonal(x1, y1, x2, y2);
      updateAnchors();
    }
    this.notifyListeners(this);
  }

  public void resize(int x, int y) {
    synchronized (this) {
      switch (grabbedAnchor()) {
      case 0:
        x1 = x;
        y2 = y;
        break;
      case 1:
        x2 = x;
        y2 = y;
        break;
      case 2:
        x2 = x;
        y1 = y;
        break;
      case 3:
        x1 = x;
        y1 = y;
        break;
      }
      shape.setFrameFromDiagonal(x1, y1, x2, y2);
      updateAnchors();
    }
    this.notifyListeners(this);
  }

  public void clearTexture() {
    synchronized (this) {
      super.clearTexture();
    }
  }

  public void setTexture(Image image) {
    synchronized (this) {
      super.setTexture(image);
    }
    notifyListeners(this);
  }

  public boolean isTransient() {
    RectangularShape bounds = (RectangularShape) shape.getBounds();
    return (bounds.getHeight() * bounds.getWidth()) < 4;
  }

  private int x1, y1, x2, y2;

  public Circle() {
    x1 = y1 = x2 = y2 = 0;
    shape = new Ellipse2D.Double();
    shape.setFrameFromDiagonal(x1, y1, x2, y2);
  }
}
