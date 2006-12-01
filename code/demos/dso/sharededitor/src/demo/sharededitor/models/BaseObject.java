/*
@COPYRIGHT@
*/
package demo.sharededitor.models;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;

import demo.sharededitor.events.IListListener;
import demo.sharededitor.ui.IFillStyleConsts;
import demo.sharededitor.ui.ITexturable;

public abstract class BaseObject implements IFillStyleConsts
{
	private List listeners;

	public void addListener(IListListener listListener)
	{
		if (listeners == null)
			listeners = Collections.synchronizedList(new ArrayList());

		if (!listeners.contains(listListener)) listeners.add(listListener);
	}

	public void removeListener(IListListener listListener)
	{
		if ((listeners != null) && (listeners.contains(listListener)))
			listeners.remove(listListener);
	}

	protected void notifyListeners(Object obj)
	{
		if (listeners == null) return;

		Iterator i = listeners.iterator();
		while (i.hasNext())
		{
			IListListener listListener = (IListListener) i.next();
			listListener.changed(this, this);
		}
	}

	private boolean isReady()
	{
		return (getShape() != null);
	}

	public boolean isAt(int x, int y)
	{
		if (!isReady()) return false;

		Shape shape = getShape();
		if (shape.contains(x, y)) return true;

		Shape[] anchors = getAnchors();
		for (int i = 0; i < anchors.length; i++)
		{
			if (anchors[i].contains(x, y)) return true;
		}
		return false;
	}

	private int grabbedAnchor;

	public synchronized void setGrabbedAnchorAt(int x, int y)
	{
		Shape[] anchors = getAnchors();
		for (int i = 0; i < anchors.length; i++)
		{
			if (anchors[i].contains(x, y))
			{
				grabbedAnchor = i;
				return;
			}
		}
		grabbedAnchor = -1;
	}

	protected int grabbedAnchor()
	{
		return grabbedAnchor;
	}

	public boolean isAnchorGrabbed()
	{
		return (grabbedAnchor >= 0) && (grabbedAnchor < getAnchors().length);
	}

	public abstract void resize(int x, int y);

	public abstract void move(int dx, int dy);

	protected Shape[] getAnchors()
	{
		return new Shape[0];
	}

	protected abstract Shape getShape();

	public void draw(Graphics2D g, boolean showAnchors)
	{
		Shape shape = getShape();
		Rectangle bounds = shape.getBounds();
		g.setColor(this.background);
		if (FILLSTYLE_SOLID == this.fillstyle) g.fill(shape);

		if ((FILLSTYLE_TEXTURED == this.fillstyle)
				&& (this instanceof ITexturable) && isTextured()
				&& (bounds.width > 0) && (bounds.height > 0))
		{
			ImageIcon image = new ImageIcon(getTexture());
			BufferedImage texture = new BufferedImage(bounds.width, bounds.height,
					BufferedImage.TYPE_INT_RGB);
			Graphics tg = texture.getGraphics();
			tg
					.drawImage(image.getImage(), 0, 0, bounds.width, bounds.height,
							null);
			Paint paint = new TexturePaint(texture, bounds);
			g.setPaint(paint);
			g.fill(shape);
		}

		g.setStroke(this.stroke);
		g.setColor(this.foreground);

		g.draw(shape);

		Shape[] anchors = getAnchors();
		for (int i = 0; showAnchors && i < anchors.length; i++)
		{
			g.fill(anchors[i]);
			g.setStroke(new BasicStroke(1));
			g.draw(anchors[i]);
		}
	}

	private int fillstyle;

	public synchronized void setFillStyle(int fillstyle)
	{
		this.fillstyle = fillstyle;
		notifyListeners(this);
	}

	private Color foreground;

	public synchronized void setForeground(Color color)
	{
		this.foreground = color;
		notifyListeners(this);
	}

	private Color background;

	public synchronized void setBackground(Color color)
	{
		this.background = color;
		notifyListeners(this);
	}

	private Stroke stroke = new BasicStroke();

	public synchronized void setStroke(Stroke stroke)
	{
		this.stroke = stroke;
		notifyListeners(this);
	}

	public static final BaseObject createObject(String name)
	{
		try
		{
			Class klass = Class
					.forName("demo.sharededitor.models." + name);
			return (BaseObject) klass.newInstance();
		}
		catch (Exception ex)
		{
			throw new InternalError(ex.getMessage());
		}
	}

	private byte[] texture = null;

	protected synchronized void setTexture(Image image)
	{
		try
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(new ImageIcon(image));
			oos.flush();
			oos.close();
			texture = bos.toByteArray();
		}
		catch (Exception ex)
		{
			throw new InternalError("Unable to convert ImageIcon to byte[]");
		}
	}

	protected Image getTexture()
	{
		try
		{
			ByteArrayInputStream bis = new ByteArrayInputStream(texture);
			ObjectInputStream ois    = new ObjectInputStream(bis);
			ImageIcon image          = (ImageIcon) ois.readObject();
			return image.getImage();
		}
		catch (Exception ex)
		{
			throw new InternalError("Unable to convert byte[] to Image");
		}
	}

	protected boolean isTextured()
	{
		return (texture != null);
	}
	
	public boolean isTransient()
	{
	   return false;
	}

	public synchronized void selectAction(boolean flag)
	{ }
	
	public synchronized void alternateSelectAction(boolean flag)
	{ }
}