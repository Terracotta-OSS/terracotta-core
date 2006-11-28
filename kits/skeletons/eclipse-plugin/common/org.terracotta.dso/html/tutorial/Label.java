package tutorial;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Label {
  DefaultBoundedRangeModel rangeModel;
  JLabel label;
  
  public static void main(String[] args) {
    new Label();
  }
  
  Label() {
    JFrame frame = new JFrame("Label Test");

    rangeModel = new DefaultBoundedRangeModel();
    rangeModel.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        label.setText(rangeModel.getValue() + "");
      }
    });
    label = new JLabel(rangeModel.getValue() + "");
    frame.getContentPane().add(label);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setSize(frame.getWidth() + 100, frame.getHeight());
    frame.setVisible(true);
  }
}
