/*
@COPYRIGHT@
*/
package demo.sharededitor.models;

import demo.sharededitor.ui.ITexturable;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RectangularShape;
import java.awt.Image;
import java.awt.Shape;

final class Circle
	extends BaseObject
	implements ITexturable
{
	private Ellipse2D.Double shape;

	protected Shape getShape()
	{
		return this.shape;
	}

	protected Shape[] getAnchors()
	{
		return new Shape[] {
			new Ellipse2D.Double(x1 - 5, y2 - 5, 10, 10),
			new Ellipse2D.Double(x2 - 5, y2 - 5, 10, 10),
			new Ellipse2D.Double(x2 - 5, y1 - 5, 10, 10),
			new Ellipse2D.Double(x1 - 5, y1 - 5, 10, 10)
		};
	}

	public synchronized void move(int dx, int dy)
	{
		x1 += dx;
		y1 += dy;
		x2 += dx;
		y2 += dy;
		shape.setFrameFromDiagonal(x1, y1, x2, y2);
		this.notifyListeners(this);
	}

	public synchronized void resize(int x, int y)
	{
		switch (grabbedAnchor())
		{
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
		this.notifyListeners(this);
	}

	public synchronized void clearTexture() {
		super.clearTexture();
		notifyListeners(this);
	}

	public synchronized void setTexture(Image image)
	{
		super.setTexture(image);
		notifyListeners(this);
	}

	public boolean isTransient()
	{
	   RectangularShape bounds = (RectangularShape)shape.getBounds();
	   return (bounds.getHeight() * bounds.getWidth()) < 4;
	}

	private int x1, y1, x2, y2;

	public Circle()
	{
		x1 = y1 = x2 = y2 = 0;
		shape = new Ellipse2D.Double();
		shape.setFrameFromDiagonal(x1, y1, x2, y2);
	}
}
