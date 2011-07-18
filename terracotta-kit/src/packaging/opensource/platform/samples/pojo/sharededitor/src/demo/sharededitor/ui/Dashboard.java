/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.sharededitor.ui;

import demo.sharededitor.controls.Dispatcher;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.filechooser.FileSystemView;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

public final class Dashboard extends JToolBar {
	private static final long serialVersionUID = -7801767824425098852L;
	// private static boolean transparentMode = false;
	private Dispatcher dispatcher;

	public Dashboard(Dispatcher dispatcher) {
		final String IMAGE_PLACEHOLDER = "/images/placeholder.gif";
		final String IMAGE_SELECTOR = "/images/selector.gif";
		final String IMAGE_LINE = "/images/line.gif";
		final String IMAGE_SQUARE = "/images/square.gif";
		final String IMAGE_FILLEDSQUARE = "/images/filledsquare.gif";
		final String IMAGE_CIRCLE = "/images/circle.gif";
		final String IMAGE_FILLEDCIRCLE = "/images/filledcircle.gif";
		final String IMAGE_TEXT = "/images/text.gif";
		final String IMAGE_BEDROOM = "/images/bedroom.jpg";

		this.dispatcher = dispatcher;

		setOrientation(VERTICAL);
		setFloatable(false);
		setLayout(new GridLayout(8, 2));

		ButtonGroup bg = new ButtonGroup();
		JToggleButton b1 = new JToggleButton(new SetDrawToolAction("Selector"));
		b1.setIcon(new ImageIcon(getResource(IMAGE_SELECTOR)));
		b1.setToolTipText("Select shapes");
		bg.add(b1);
		add(b1);

		JButton b0 = new JButton();
		b0.setEnabled(false);
		b0.setIcon(new ImageIcon(getResource(IMAGE_PLACEHOLDER)));
		setWithSolidColorIcon(b0, Color.LIGHT_GRAY);

		b1 = new JToggleButton(new SetDrawToolAction("Line"));
		b1.setIcon(new ImageIcon(getResource(IMAGE_LINE)));
		b1.setToolTipText("Draw lines");
		bg.add(b1);
		add(b1);

		b1 = new JToggleButton(new SetDrawToolAction("Square",
				FillStyleConsts.FILLSTYLE_TRANSPARENT));
		b1.setIcon(new ImageIcon(getResource(IMAGE_SQUARE)));
		b1.setToolTipText("Draw transparent squares and rectangular shapes");
		bg.add(b1);
		add(b1);

		b1 = new JToggleButton(new SetDrawToolAction("Square",
				FillStyleConsts.FILLSTYLE_SOLID));
		b1.setIcon(new ImageIcon(getResource(IMAGE_FILLEDSQUARE)));
		b1.setToolTipText("Draw solid squares and rectangular shapes");
		bg.add(b1);
		add(b1);

		b1 = new JToggleButton(new SetDrawToolAction("Circle",
				FillStyleConsts.FILLSTYLE_TRANSPARENT));
		b1.setIcon(new ImageIcon(getResource(IMAGE_CIRCLE)));
		b1.setToolTipText("Draw transparent circles and oval shapes");
		bg.add(b1);
		add(b1);

		b1 = new JToggleButton(new SetDrawToolAction("Circle",
				FillStyleConsts.FILLSTYLE_SOLID));
		b1.setIcon(new ImageIcon(getResource(IMAGE_FILLEDCIRCLE)));
		b1.setToolTipText("Draw solid circles and oval shapes");
		bg.add(b1);
		add(b1);

		b1 = new JToggleButton(new SetDrawToolAction("Text"));
		b1.setIcon(new ImageIcon(getResource(IMAGE_TEXT)));
		b1.setToolTipText("Render text as images");
		add(b1);
		bg.add(b1);

		b1 = new JToggleButton(new SetDrawToolAction("Image",
				FillStyleConsts.FILLSTYLE_TEXTURED));
		textureToolButton = b1;
		b1.setIcon(new ImageIcon(getResource(IMAGE_PLACEHOLDER)));
		setWithImageIcon(b1, new ImageIcon(getResource(IMAGE_BEDROOM)));
		b1.setToolTipText("Paint with images");
		add(b1);
		bg.add(b1);

		add(new JToolBar.Separator());
		add(new JToolBar.Separator());

		JButton b2 = new JButton(new SetColorAction("Foreground",
				Color.DARK_GRAY));
		b2.setIcon(new ImageIcon(getResource(IMAGE_PLACEHOLDER)));
		b2.setToolTipText("Select line color");
		setWithSolidColorIcon(b2, Color.DARK_GRAY);
		add(b2);

		b2 = new JButton(new SetColorAction("Background", Color.LIGHT_GRAY));
		b2.setIcon(new ImageIcon(getResource(IMAGE_PLACEHOLDER)));
		b2.setToolTipText("Select fill color");
		setWithSolidColorIcon(b2, Color.LIGHT_GRAY);
		add(b2);

		b2 = new JButton(new SetTextureAction());
		this.textureSelectButton = b2;
		b2.setIcon(new ImageIcon(getResource(IMAGE_PLACEHOLDER)));
		setWithImageIcon(b2, new ImageIcon(getResource(IMAGE_BEDROOM)));
		b2.setToolTipText("Select the image to use when painting with images");
		b2.setEnabled(false);
		add(b2);

		// default settings
		dispatcher.setStroke(new BasicStroke(1));
		dispatcher.setFillStyle(FillStyleConsts.FILLSTYLE_SOLID);
		dispatcher.setForeground(Color.DARK_GRAY);
		dispatcher.setBackground(Color.LIGHT_GRAY);
		dispatcher.setTexture(new ImageIcon(getResource(IMAGE_BEDROOM))
				.getImage());
		dispatcher.setFontName("Courier New");
		dispatcher.setFontSize(24);
		dispatcher.setDrawTool("Line");
	}

	private AbstractButton textureSelectButton = null;

	private AbstractButton textureToolButton = null;

	private void setWithSolidColorIcon(AbstractButton b, Color c) {
		final int w = b.getIcon().getIconWidth();
		final int h = b.getIcon().getIconHeight();
		final BufferedImage icon = new BufferedImage(w, h,
				BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = icon.createGraphics();
		final Rectangle r = new Rectangle(0, 0, w, h);
		g.setColor(c);
		g.setBackground(c);
		g.fill(r);
		b.setIcon(new ImageIcon(icon));
	}

	private URL getResource(String path) {
		return getClass().getResource(path);
	}

	private void setWithImageIcon(AbstractButton b, ImageIcon icon) {
		dispatcher.setTexture(icon.getImage());
		int w = b.getIcon().getIconWidth();
		int h = b.getIcon().getIconHeight();
		BufferedImage scaled = new BufferedImage(w, h,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = scaled.createGraphics();
		g.drawImage(icon.getImage(), 0, 0, w, h, null);
		b.setIcon(new ImageIcon(scaled));
	}

	class SetDrawToolAction extends AbstractAction implements FillStyleConsts {
		private static final long serialVersionUID = 1L;

		private String name;

		private int fillstyle;

		public SetDrawToolAction(String name) {
			this.name = name;
		}

		public SetDrawToolAction(String name, int fillstyle) {
			this.name = name;
			this.fillstyle = fillstyle;
		}

		public void actionPerformed(ActionEvent e) {
			dispatcher.setDrawTool(name);
			dispatcher.setFillStyle(fillstyle);
			textureSelectButton.setEnabled(FILLSTYLE_TEXTURED == fillstyle);
		}
	}

	class SetColorAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		private String name;

		private Color color;

		public SetColorAction(String name, Color color) {
			this.name = name;
			this.color = color;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				Method m = dispatcher.getClass().getMethod("set" + name,
						new Class[] { Color.class });
				Color selected = JColorChooser.showDialog(Dashboard.this, name,
						color);

				if (selected == null)
					return;

				m.invoke(dispatcher, new Object[] { selected });
				Dashboard.this.setWithSolidColorIcon((AbstractButton) e
						.getSource(), selected);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new InternalError("Unable to set " + name + " color.");
			}
		}
	}

	class SetTextureAction extends AbstractAction implements FillStyleConsts {
		private static final long serialVersionUID = 1L;
		//private String name;
		public void actionPerformed(ActionEvent e) {
			JFileChooser jc = new JFileChooser(FileSystemView
					.getFileSystemView());
			jc.setFileFilter(new ImageFileFilter());

			if (JFileChooser.APPROVE_OPTION == jc
					.showOpenDialog(Dashboard.this)) {
				String imagefile = jc.getSelectedFile().getAbsolutePath();
				Dashboard.this.setWithImageIcon((AbstractButton) e.getSource(),
						new ImageIcon(imagefile));
				Dashboard.this.setWithImageIcon(
						(AbstractButton) textureToolButton, new ImageIcon(
								imagefile));
			}
		}

		class ImageFileFilter extends javax.swing.filechooser.FileFilter {
			public boolean accept(java.io.File f) {
				final String filename = f.getName().toLowerCase();
				return filename.endsWith(".jpeg") || filename.endsWith(".jpg")
						|| filename.endsWith(".gif")
						|| filename.endsWith(".png");
			}

			public String getDescription() {
				return "JPEG, JPG, GIF, & PNG Images";
			}
		}
	}
}
