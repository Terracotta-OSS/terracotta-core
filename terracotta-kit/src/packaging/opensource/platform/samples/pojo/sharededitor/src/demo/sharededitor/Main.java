/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.sharededitor;

import demo.sharededitor.controls.Dispatcher;
import demo.sharededitor.models.ObjectManager;
import demo.sharededitor.ui.Dashboard;
import demo.sharededitor.ui.Renderer;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public final class Main extends JFrame {
  private static final long serialVersionUID = 1L;

  public Main() {
    super("Shared Editor");

    ObjectManager objmgr = new ObjectManager();
    Renderer renderer = new Renderer();
    Dispatcher dispatcher = new Dispatcher(objmgr, renderer);
    Dashboard controller = new Dashboard(dispatcher);
    Container content = getContentPane();
    JPanel display = new JPanel();

    display.setLayout(new BorderLayout(5, 5));
    display.add(renderer, BorderLayout.CENTER);
    content.add(display, BorderLayout.CENTER);
    content.add(controller, BorderLayout.EAST);
    renderer.setPreferredSize(new Dimension(600, 50));

    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setDefaultLookAndFeelDecorated(true);

    pack();

    setResizable(false);
    setVisible(true);
  }

  public static void main(String[] args) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new Main();
      }
    });
  }
}
