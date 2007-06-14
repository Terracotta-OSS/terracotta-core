/*
@COPYRIGHT@
*/
package demo.sharededitor.models;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.Shape;

final class Line extends BaseObject
{
	private Line2D.Double shape;
	
	protected Shape getShape()
	{
		return shape;
	}
	
	protected Shape[] getAnchors()
	{
		return new Shape[] {
			new Ellipse2D.Double(shape.x2 - 5, shape.y2 - 5, 10, 10),
			new Ellipse2D.Double(shape.x1 - 5, shape.y1 - 5, 10, 10)
		};
	}
	
	public boolean isAt(int x, int y)
	{
		return (shape.ptLineDist(x, y) <= 5) || super.isAt(x, y);
	}
	
	public synchronized void move(int dx, int dy)
	{
		shape.x1 += dx;
		shape.y1 += dy;
		shape.x2 += dx;
		shape.y2 += dy;
		this.notifyListeners(this);
	}
	
	public synchronized void resize(int x, int y)
	{
		switch (grabbedAnchor())
		{
			case 0:
				shape.x2 = x;
				shape.y2 = y;
				break;
			case 1:
				shape.x1 = x;
				shape.y1 = y;
				break;
		}
		this.notifyListeners(this);
	}

	public boolean isTransient()
	{
	   double dx = shape.x1 - shape.x2;
	   double dy = shape.y1 - shape.y2;
	   return Math.sqrt((dx * dx) + (dy * dy)) < 4;
	}
	
	public Line()
	{
		shape = new Line2D.Double();
		shape.x1 = 0;
		shape.y1 = 0;
		shape.x2 = 0;
		shape.y2 = 0;
	}
}
