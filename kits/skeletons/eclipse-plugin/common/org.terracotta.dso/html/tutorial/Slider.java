package tutorial;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JSlider;

public class Slider {
  DefaultBoundedRangeModel rangeModel;
  
  public static void main(String[] args) {
    new Slider();
  }
  
  Slider() {
    JFrame frame = new JFrame("Slider Test");

    rangeModel = new DefaultBoundedRangeModel();
    JSlider slider = new JSlider(rangeModel);
    frame.getContentPane().add(slider);
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }
}
