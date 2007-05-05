/**
 *
 * All content copyright (c) 2003-2007 Terracotta, Inc.,
 * except as may otherwise be noted in a separate copyright notice.
 * All rights reserved.
 *
 */
package demo.sharededitor.models;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
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

/**
 * Description of the Class
 * 
 * @author Terracotta, Inc.
 */
public abstract class BaseObject implements IFillStyleConsts, ImageObserver {

	private transient ImageIcon image;

	private transient BufferedImage imageCache;

	private List listeners;

	private int grabbedAnchor;

	private int fillstyle;

	private Color foreground;

	private Color background;

	private Stroke stroke = new BasicStroke();

	private byte[] texture = null;

	public boolean imageUpdate(Image img, int x, int y, int width, int height,
			int flags) {
		return false;
	}

	public synchronized void setGrabbedAnchorAt(int x, int y) {
		Shape[] anchors = getAnchors();
		for (int i = 0; i < anchors.length; i++) {
			if (anchors[i].contains(x, y)) {
				grabbedAnchor = i;
				return;
			}
		}
		grabbedAnchor = -1;
	}

	public synchronized void setFillStyle(int fillstyle) {
		this.fillstyle = fillstyle;
		notifyListeners(this);
	}

	public synchronized void setForeground(Color color) {
		this.foreground = color;
		notifyListeners(this);
	}

	public synchronized void setBackground(Color color) {
		this.background = color;
		notifyListeners(this);
	}

	public synchronized void setStroke(Stroke stroke) {
		this.stroke = stroke;
		notifyListeners(this);
	}

	public boolean isAt(int x, int y) {
		if (!isReady()) {
			return false;
		}

		Shape shape = getShape();
		if (shape.contains(x, y)) {
			return true;
		}

		Shape[] anchors = getAnchors();
		for (int i = 0; i < anchors.length; i++) {
			if (anchors[i].contains(x, y)) {
				return true;
			}
		}
		return false;
	}

	public boolean isAnchorGrabbed() {
		return (grabbedAnchor >= 0) && (grabbedAnchor < getAnchors().length);
	}

	public boolean isTransient() {
		return false;
	}

	public void addListener(IListListener listListener) {
		if (listeners == null) {
			listeners = Collections.synchronizedList(new ArrayList());
		}

		synchronized (listeners) {
			if (!listeners.contains(listListener)) {
				listeners.add(listListener);
			}
		}
	}

	public void removeListener(IListListener listListener) {
		if ((listeners != null) && (listeners.contains(listListener))) {
			synchronized (listeners) {
				listeners.remove(listListener);
			}
		}
	}

	public abstract void resize(int x, int y);

	public abstract void move(int dx, int dy);

	public void draw(Graphics2D g, boolean showAnchors) {
		Shape shape = getShape();
		Rectangle bounds = shape.getBounds();
		g.setColor(this.background);
		if (FILLSTYLE_SOLID == this.fillstyle) {
			g.fill(shape);

			g.setStroke(this.stroke);
			g.setColor(this.foreground);

			g.draw(shape);
		}

		if ((FILLSTYLE_TEXTURED == this.fillstyle)
				&& (this instanceof ITexturable) && isTextured()
				&& (bounds.width > 0) && (bounds.height > 0)) {

			if (image == null) {
				image = new ImageIcon(getTexture());

				imageCache = new BufferedImage(bounds.width, bounds.height,
						BufferedImage.TYPE_INT_RGB);
			}

			g.drawImage(image.getImage(), bounds.x, bounds.y, bounds.width,
					bounds.height, image.getImageObserver());
		} else {
			g.setColor(this.foreground);

			g.draw(shape);
		}

		Shape[] anchors = getAnchors();
		for (int i = 0; showAnchors && i < anchors.length; i++) {
			g.fill(anchors[i]);
			g.setStroke(new BasicStroke(1));
			g.draw(anchors[i]);
		}
	}

	public synchronized void selectAction(boolean flag) {
	}

	public synchronized void alternateSelectAction(boolean flag) {
	}

	protected synchronized void setTexture(Image image) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(new ImageIcon(image));
			oos.flush();
			oos.close();
			texture = bos.toByteArray();
		} catch (Exception ex) {
			throw new InternalError("Unable to convert ImageIcon to byte[]");
		}
	}

	protected Shape[] getAnchors() {
		return new Shape[0];
	}

	protected abstract Shape getShape();

	protected Image getTexture() {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(texture);
			ObjectInputStream ois = new ObjectInputStream(bis);
			ImageIcon image = (ImageIcon) ois.readObject();
			return image.getImage();
		} catch (Exception ex) {
			throw new InternalError("Unable to convert byte[] to Image");
		}
	}

	protected boolean isTextured() {
		return (texture != null);
	}

	protected void notifyListeners(Object obj) {
		if (listeners == null) {
			return;
		}

		synchronized (listeners) {
			Iterator i = listeners.iterator();
			while (i.hasNext()) {
				IListListener listListener = (IListListener) i.next();
				listListener.changed(this, this);
			}
		}
	}

	protected int grabbedAnchor() {
		return grabbedAnchor;
	}

	private boolean isReady() {
		return (getShape() != null);
	}

	public static final BaseObject createObject(String name) {
		try {
			Class klass = Class.forName("demo.sharededitor.models." + name);
			return (BaseObject) klass.newInstance();
		} catch (Exception ex) {
			throw new InternalError(ex.getMessage());
		}
	}
}
