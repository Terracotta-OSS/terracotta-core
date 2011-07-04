/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.sharededitor.models;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import demo.sharededitor.ui.Fontable;
import demo.sharededitor.ui.Texturable;

public class Text extends BaseObject implements Fontable, Texturable {
	private Shape shape = null;

	protected Shape getShape() {
		return shape;
	}

	private Shape[] anchors = new Shape[0];

	protected Shape[] getAnchors() {
		return anchors;
	}

	public boolean isAt(int x, int y) {
		if (shape == null)
			return false;

		Shape bounds = new Rectangle2D.Double(shape.getBounds().x - 5, shape
				.getBounds().y - 5, shape.getBounds().width + 5, shape
				.getBounds().height + 5);
		return bounds.contains(x, y);
	}

	public synchronized void move(int dx, int dy) {
		this.x += dx;
		this.y += dy;
		notifyListeners(this);
	}

	public synchronized void resize(int x, int y) {
		// we purposely don't allow the resizing operation for Text objects
	}

	public synchronized void clearTexture() {
		super.clearTexture();
	}

	public synchronized void setTexture(Image image) {
		super.setTexture(image);
		notifyListeners(this);
	}

	private String fontName;

	private int fontSize;

	public synchronized void setFontInfo(String name, int size, String text) {
		this.text = text;
		this.fontName = name;
		this.fontSize = size;
		notifyListeners(this);
	}

	public synchronized void setFontName(String name) {
		this.fontName = name;
		notifyListeners(this);
	}

	public synchronized void setFontSize(int size) {
		this.fontSize = size;
		notifyListeners(this);
	}

	private String text;

	public synchronized void setText(String text) {
		this.text = text;
		notifyListeners(this);
	}

	public String getText() {
		return this.text;
	}

	public synchronized void appendToText(char c) {
		if (!this.isInverted)
			this.text += c;
		else {
			this.text = c + "";
			this.isInverted = false;
		}
		notifyListeners(this);
	}

	private int x, y;

	private synchronized void renderText(FontRenderContext frc,
			boolean showCursor) {
		String text = this.text;
		if (showCursor || (text.length() == 0)) {
			if (text.length() > 0)
				text = this.isInverted ? text : text + "|";
			else
				text = "_";
		}

		AttributedString as = new AttributedString(text);
		as.addAttribute(TextAttribute.FONT, new Font(this.fontName, Font.PLAIN,
				this.fontSize));
		AttributedCharacterIterator aci = as.getIterator();
		TextLayout tl = new TextLayout(aci, frc);
		this.shape = tl.getOutline(AffineTransform.getTranslateInstance(x, y));
	}

	public void draw(Graphics2D g, boolean showAnchors) {
		renderText(g.getFontRenderContext(), showAnchors);
		super.draw(g, showAnchors);
		if (showAnchors) {
			Rectangle bounds = shape.getBounds();
			Shape border = new Rectangle2D.Double(bounds.x - 5, bounds.y - 5,
					bounds.width + 5, bounds.height + 5);
			if (this.isInverted) {
				g.setXORMode(Color.LIGHT_GRAY);
				g.fillRect(bounds.x - 5, bounds.y - 5, bounds.width + 5,
						bounds.height + 5);
				g.setPaintMode();
			}
			g.setColor(Color.LIGHT_GRAY);
			g.draw(border);
		}
	}

	private boolean isInverted;

	public synchronized void selectAction(boolean flag) {
		if (!this.isInverted)
			return;
		this.isInverted = false;
	}

	public synchronized void alternateSelectAction(boolean flag) {
		if (this.isInverted)
			return;
		this.isInverted = true;
	}

	public Text() {
		this.isInverted = false;
		this.text = "";
	}
}
