/*
@COPYRIGHT@
 */
package demo.sharededitor.controls;

//import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import javax.swing.event.MouseInputAdapter;

import demo.sharededitor.events.ListListener;
import demo.sharededitor.models.BaseObject;
import demo.sharededitor.models.ObjectManager;
import demo.sharededitor.ui.Fontable;
import demo.sharededitor.ui.Texturable;
import demo.sharededitor.ui.Renderer;

public final class Dispatcher extends MouseInputAdapter implements KeyListener {
	private ObjectManager objmgr;

	private transient Renderer renderer;

	private transient int lx;

	private transient int ly;

	private transient Image texture;

	private transient String drawTool;

	private transient Color foreground;

	private transient Color background;

	private transient int fillstyle;

	private transient Stroke stroke;

	private transient String fontName;

	private transient int fontSize;

	public Dispatcher(ObjectManager objects, Renderer renderer) {
		this.renderer = renderer;
		this.renderer.addMouseListener(this);
		this.renderer.addMouseMotionListener(this);
		this.renderer.addKeyListener(this);

		this.objmgr = objects;
		this.objmgr.setListener((ListListener) this.renderer);
	}

	public synchronized void mousePressed(MouseEvent e) {
		this.renderer.requestFocusInWindow();
		int x = e.getX();
		int y = e.getY();
		trackXY(x, y);

		if (objmgr.canGrabAt(x, y))
			objmgr.grabAt(x, y, !e.isControlDown());
		else {
			BaseObject obj = objmgr.create(x, y, this.drawTool);
			obj.setFillStyle(this.fillstyle);
			obj.setForeground(this.foreground);
			obj.setBackground(this.background);
			obj.setStroke(this.stroke);
			if ((obj instanceof Texturable)
					&& (BaseObject.FILLSTYLE_TEXTURED == fillstyle)) {
				Texturable to = (Texturable) obj;
				to.setTexture(this.texture);
			}

			if (obj instanceof Fontable) {
				Fontable fo = (Fontable) obj;
				fo.setFontInfo(this.fontName, this.fontSize, "");
			}
		}
	}

	public synchronized void mouseDragged(MouseEvent e) {
		if (objmgr.lastGrabbed() == null)
			return;

		int x = e.getX();
		int y = e.getY();
		BaseObject current = objmgr.lastGrabbed();

		if (current.isAnchorGrabbed())
			current.resize(x, y);
		else
			current.move(x - lx, y - ly);

		trackXY(x, y);
	}

	public synchronized void mouseClicked(MouseEvent e) {
		BaseObject current = objmgr.grabAt(e.getX(), e.getY(), !e
				.isControlDown());
		if (current != null) {
			switch (e.getClickCount()) {
			case 1:
				current.selectAction(true);
				break;
			case 2:
				current.alternateSelectAction(true);
				break;
			}
		}
		trackXY(e.getX(), e.getY());
	}

	public synchronized void mouseMoved(MouseEvent e) {
		trackXY(e.getX(), e.getY());
	}

	public synchronized void mouseReleased(MouseEvent e) {
		trackXY(e.getX(), e.getY());
		BaseObject current = objmgr.lastGrabbed();
		if ((current != null) && current.isTransient()) {
			objmgr.deleteSelection();
			objmgr.selectAllWithin(current);
		}
	}

	public synchronized void keyPressed(KeyEvent e) {
		processCommand(e);
	}

	public synchronized void keyTyped(KeyEvent e) {
		processCommand(e);
	}

	public synchronized void keyReleased(KeyEvent e) {
		// no need to do anything here
	}

	private void processCommand(KeyEvent e) {
		if (KeyEvent.KEY_TYPED == e.getID()) {
			if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxy1234567890!@#$%^&*() _+=-`~[]\\{}|:;'?/><,.\""
					.indexOf(e.getKeyChar()) != -1)
				sendCharToObject(e.getKeyChar());
		} else {
			int keyCode = e.getKeyCode();
			if (e.isControlDown()) {
				switch (keyCode) {
				case KeyEvent.VK_BACK_SPACE:
					this.objmgr.deleteSelection();
					break;
				case KeyEvent.VK_I:
					this.objmgr.invertSelection();
					break;
				case KeyEvent.VK_A:
					this.objmgr.toggleSelection();
					break;
				}
			} else {
				switch (keyCode) {
				case KeyEvent.VK_BACK_SPACE:
					sendKeyToObject(keyCode);
					break;
				case KeyEvent.VK_DELETE:
					this.objmgr.deleteSelection();
					break;
				case KeyEvent.VK_ENTER:
				case KeyEvent.VK_ESCAPE:
					this.objmgr.clearSelection();
					break;
				}
			}
		}
	}

	private void sendCharToObject(char c) {
		BaseObject current = objmgr.lastGrabbed();
		if ((current != null) && (current instanceof Fontable)) {
			((Fontable) current).appendToText(c);
			//String text = ((IFontable) current).getText();
			//((IFontable) current).setText(text + c);
		}
	}

	private void sendKeyToObject(int keyCode) {
		BaseObject current = objmgr.lastGrabbed();
		if ((current != null) && (current instanceof Fontable)) {
			String text = ((Fontable) current).getText();
			if (text.length() == 0)
				return;
			((Fontable) current).setText(text.substring(0, text.length() - 1));
		}
	}

	private void trackXY(int x, int y) {
		lx = x;
		ly = y;
	}

	public synchronized void setDrawTool(String name) {
		this.renderer.requestFocusInWindow();
		this.drawTool = name;
	}

	public synchronized void setTexture(Image texture) {
		this.renderer.requestFocusInWindow();
		this.texture = texture;
		BaseObject current = objmgr.lastGrabbed();
		if ((current != null) && (current instanceof Texturable)
				&& (BaseObject.FILLSTYLE_TEXTURED == fillstyle)) {
			((Texturable) current).clearTexture();
			((Texturable) current).setTexture(texture);
		}
	}

	public synchronized void setFontName(String fontName) {
		this.renderer.requestFocusInWindow();
		this.fontName = fontName;
		BaseObject current = objmgr.lastGrabbed();
		if ((current != null) && (current instanceof Fontable))
			((Fontable) current).setFontName(fontName);
	}

	public synchronized void setFontSize(int fontSize) {
		this.renderer.requestFocusInWindow();
		this.fontSize = fontSize;
		BaseObject current = objmgr.lastGrabbed();
		if ((current != null) && (current instanceof Fontable))
			((Fontable) current).setFontSize(fontSize);
	}

	public synchronized void setFillStyle(int fillstyle) {
		this.renderer.requestFocusInWindow();
		this.fillstyle = fillstyle;
	}

	public synchronized void setStroke(Stroke stroke) {
		this.renderer.requestFocusInWindow();
		this.stroke = stroke;
		BaseObject current = objmgr.lastGrabbed();
		if (current != null)
			current.setStroke(stroke);
	}

	public synchronized void setForeground(Color foreground) {
		this.renderer.requestFocusInWindow();
		this.foreground = foreground;
		BaseObject current = objmgr.lastGrabbed();
		if (current != null)
			current.setForeground(foreground);
	}

	public synchronized void setBackground(Color background) {
		this.renderer.requestFocusInWindow();
		this.background = background;
		BaseObject current = objmgr.lastGrabbed();
		if (current != null)
			current.setBackground(background);
	}
}
