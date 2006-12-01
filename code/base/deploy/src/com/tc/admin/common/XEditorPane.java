/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import org.dijon.EditorPane;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

public class XEditorPane extends EditorPane {
  public XEditorPane() {
    super();
    addHyperlinkListener(new Hyperactive());
  }

  class Hyperactive implements HyperlinkListener {
    public void hyperlinkUpdate(HyperlinkEvent e) {
      System.out.println(e);

      if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        XEditorPane pane = (XEditorPane)e.getSource();

        if(e instanceof HTMLFrameHyperlinkEvent) {
          HTMLFrameHyperlinkEvent  evt = (HTMLFrameHyperlinkEvent)e;
          HTMLDocument             doc = (HTMLDocument)pane.getDocument();

          doc.processHTMLFrameHyperlinkEvent(evt);
        }
        else {
          try {
			      pane.setPage(e.getURL());
          }
          catch(Throwable t) {
			      t.printStackTrace();
          }
        }
      }
    }
  }
}
