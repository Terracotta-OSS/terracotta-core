/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.sharededitor.models;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.Shape;

final class Line extends BaseObject {
	private Line2D.Double shape;

	protected Shape getShape() {
		return shape;
	}

	private transient Shape[] anchors = null;

	private Shape[] updateAnchors() {
		if (anchors == null) {
			anchors = new Shape[] {
					new Ellipse2D.Double(shape.x2 - 5, shape.y2 - 5, 10, 10),
					new Ellipse2D.Double(shape.x1 - 5, shape.y1 - 5, 10, 10) };
			return anchors;
		}

		((Ellipse2D.Double) anchors[0]).x = shape.x2 - 5;
		((Ellipse2D.Double) anchors[0]).y = shape.y2 - 5;
		((Ellipse2D.Double) anchors[1]).x = shape.x1 - 5;
		((Ellipse2D.Double) anchors[1]).y = shape.y1 - 5;
		return anchors;
	}

	protected Shape[] getAnchors() {
		return updateAnchors();
	}

	public boolean isAt(int x, int y) {
		return (shape.ptSegDist(x, y) <= 5) || super.isAt(x, y);
	}

	public void move(int dx, int dy) {
		synchronized (this) {
			shape.x1 += dx;
			shape.y1 += dy;
			shape.x2 += dx;
			shape.y2 += dy;
			updateAnchors();
		}
		this.notifyListeners(this);
	}

	public void resize(int x, int y) {
		synchronized (this) {
			switch (grabbedAnchor()) {
			case 0:
				shape.x2 = x;
				shape.y2 = y;
				break;
			case 1:
				shape.x1 = x;
				shape.y1 = y;
				break;
			}
			updateAnchors();
		}
		this.notifyListeners(this);
	}

	public boolean isTransient() {
		double dx = shape.x1 - shape.x2;
		double dy = shape.y1 - shape.y2;
		return Math.sqrt((dx * dx) + (dy * dy)) < 4;
	}

	public Line() {
		shape = new Line2D.Double();
		shape.x1 = 0;
		shape.y1 = 0;
		shape.x2 = 0;
		shape.y2 = 0;
	}
}
