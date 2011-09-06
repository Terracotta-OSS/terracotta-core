/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.io.IOException;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

public class XEditorPane extends JEditorPane implements HyperlinkListener {
  public XEditorPane() {
    super();
    addHyperlinkListener(this);
  }

  public void hyperlinkUpdate(HyperlinkEvent e) {
    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
      XEditorPane pane = (XEditorPane) e.getSource();

      if (e instanceof HTMLFrameHyperlinkEvent) {
        HTMLDocument doc = (HTMLDocument) pane.getDocument();
        doc.processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
      } else {
        try {
          pane.setPage(e.getURL());
        } catch (IOException ioe) {
          throw new RuntimeException(e.getURL().toExternalForm(), ioe);
        }
      }
    }
  }
}
