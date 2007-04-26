package com.tctest.rife.elements;

import com.uwyn.rife.engine.Element;
import com.uwyn.rife.engine.annotations.Elem;
import com.uwyn.rife.engine.annotations.Submission;
import com.uwyn.rife.template.Template;

@Elem(submissions = {@Submission(name = "count")})
public class Counter extends Element {
  public void processElement() {
    int counter = 0;
    Template t = getHtmlTemplate("counter");
    
    while (counter < 10) {
      t.setValue("counter", counter);
      print(t);
      pause();
      counter++;
    }
    
    t.setBlock("content", "done");
    print(t);
  }
}