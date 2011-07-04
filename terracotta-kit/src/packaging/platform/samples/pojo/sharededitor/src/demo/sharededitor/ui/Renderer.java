/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.sharededitor.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.JComponent;

import demo.sharededitor.events.ListListener;
import demo.sharededitor.models.BaseObject;
import demo.sharededitor.models.ObjectManager;

public final class Renderer extends JComponent implements ListListener {
	public static final long serialVersionUID = 0;

	public Renderer() {
		setDoubleBuffered(true);
		objmgr = null;
	}

	private ObjectManager objmgr;

	private Image drawingArea;

	public void changed(Object source, Object obj) {
		this.objmgr = (ObjectManager) source;
		this.repaint();
	}

	public void paint(Graphics g) {
		if (drawingArea == null) {
			drawingArea = createImage(getSize().width, getSize().height);
		}

		Graphics2D g2 = (Graphics2D) drawingArea.getGraphics();
		g2.setBackground(Color.WHITE);
		g2.clearRect(0, 0, getSize().width, getSize().height);

		if (objmgr == null) {
			return;
		}

		BaseObject[] objList = objmgr.list();
		for (int i = 0; i < objList.length; i++) {
			BaseObject obj = objList[i];
			obj.draw(g2, objmgr.isGrabbed(obj));
		}

		g2.setColor(Color.DARK_GRAY);
		g2.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
		g2.dispose();

		g.drawImage(drawingArea, 0, 0, null);
	}
}
