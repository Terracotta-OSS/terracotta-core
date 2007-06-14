/*
@COPYRIGHT@
*/
package demo.sharededitor.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

import demo.sharededitor.events.IListListener;
import demo.sharededitor.models.BaseObject;
import demo.sharededitor.models.ObjectManager;

public final class Renderer
	extends JComponent
	implements IListListener
{
	public static final long serialVersionUID = 0;

	public Renderer()
	{
		objmgr = null;
		this.setDoubleBuffered(true);
	}

	private ObjectManager objmgr;

	public void changed(Object source, Object obj)
	{
		this.objmgr = (ObjectManager)source;
		this.repaint();
	}

	public void paint(Graphics g)
	{
		super.paint(g);
		
		Image image = createImage(getSize().width, getSize().height);
		Graphics2D g2 = (Graphics2D)image.getGraphics();
		g2.setBackground(Color.WHITE);
		g2.clearRect(0, 0, getSize().width, getSize().height);

		if (objmgr == null) {
			return;
		}

		BaseObject[] objList = objmgr.list();
		for(int i=0; i<objList.length; i++)
		{
			BaseObject obj = objList[i];
			obj.draw(g2, objmgr.isGrabbed(obj));
		}
		
		Shape border = new Rectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1);
		g2.setColor(Color.DARK_GRAY);
		g2.draw(border);
		
		g.drawImage(image, 0, 0, null);
	}
}
