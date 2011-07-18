/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;

import demo.sharededitor.events.ListListener;
import demo.sharededitor.ui.FillStyleConsts;
import demo.sharededitor.ui.Texturable;

public abstract class BaseObject implements FillStyleConsts {
	private List listeners;

	public void addListener(ListListener listListener) {
		if (listeners == null) {
			listeners = Collections.synchronizedList(new ArrayList());
		}

		if (!listeners.contains(listListener))
			listeners.add(listListener);
	}

	public void removeListener(ListListener listListener) {
		if ((listeners != null) && (listeners.contains(listListener))) {
			listeners.remove(listListener);
		}
	}

	protected void notifyListeners(Object obj) {
		if (listeners == null)
			return;

		for (Iterator i = listeners.iterator(); i.hasNext();) {
			ListListener listListener = (ListListener) i.next();
			listListener.changed(this, this);
		}
	}

	private boolean isReady() {
		return (getShape() != null);
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
			if (anchors[i].contains(x, y))
				return true;
		}
		return false;
	}

	private int grabbedAnchor;

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

	protected int grabbedAnchor() {
		return grabbedAnchor;
	}

	public boolean isAnchorGrabbed() {
		return (grabbedAnchor >= 0) && (grabbedAnchor < getAnchors().length);
	}

	public abstract void resize(int x, int y);

	public abstract void move(int dx, int dy);

	protected Shape[] getAnchors() {
		return new Shape[0];
	}

	protected abstract Shape getShape();

	public void draw(Graphics2D g, boolean showAnchors) {
		Shape shape = getShape();
		Rectangle bounds = shape.getBounds();
		g.setColor(this.background);

		if (FILLSTYLE_SOLID == this.fillstyle) {
			g.fill(shape);
		}

		if ((FILLSTYLE_TEXTURED == this.fillstyle)
				&& (this instanceof Texturable) && isTextured()
				&& (bounds.width > 0) && (bounds.height > 0)) {
			g.drawImage(getTexture(), bounds.x, bounds.y, bounds.width,
					bounds.height, null);
		}

		g.setStroke(this.stroke);
		g.setColor(this.foreground);
		g.draw(shape);

		if (showAnchors) {
			Shape[] anchors = getAnchors();
			for (int i = 0; i < anchors.length; i++) {
				g.fill(anchors[i]);
			}
		}
	}

	private int fillstyle;

	public synchronized void setFillStyle(int fillstyle) {
		this.fillstyle = fillstyle;
		notifyListeners(this);
	}

	private Color foreground;

	public synchronized void setForeground(Color color) {
		this.foreground = color;
		notifyListeners(this);
	}

	private Color background;

	public synchronized void setBackground(Color color) {
		this.background = color;
		notifyListeners(this);
	}

	private Stroke stroke = new BasicStroke();

	public synchronized void setStroke(Stroke stroke) {
		this.stroke = stroke;
		notifyListeners(this);
	}

	public static final BaseObject createObject(String name) {
		try {
			Class klass = Class.forName("demo.sharededitor.models." + name);
			return (BaseObject) klass.newInstance();
		} catch (Exception ex) {
			throw new InternalError(ex.getMessage());
		}
	}

	private byte[] texture = null;
	private transient Image textureImage = null;

	protected void clearTexture() {
		this.texture = null;
	}

	protected void setTexture(Image image) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			int width = image.getWidth(null);
			if (width > 640) {
				width = 640;
			}

			int height = image.getHeight(null);
			if (height > 480) {
				height = 480;
			}

			Image scaledImage = image.getScaledInstance(width, height,
					Image.SCALE_FAST);
			oos.writeObject(new ImageIcon(scaledImage));

			texture = bos.toByteArray();
			textureImage = null;
		} catch (Exception ex) {
			throw new InternalError("Unable to convert Image to byte[]");
		}
	}

	protected Image getTexture() {
		try {
			if (textureImage == null) {
				ByteArrayInputStream bis = new ByteArrayInputStream(texture);
				ObjectInputStream ois = new ObjectInputStream(bis);
				ImageIcon image = (ImageIcon) ois.readObject();
				textureImage = image.getImage();
			}
			return textureImage;
		} catch (Exception ex) {
			throw new InternalError("Unable to convert byte[] to Image");
		}
	}

	protected boolean isTextured() {
		return (texture != null);
	}

	public boolean isTransient() {
		return false;
	}

	public synchronized void selectAction(boolean flag) {
		// nothing to do here
	}

	public synchronized void alternateSelectAction(boolean flag) {
		// nothing to do here
	}
}