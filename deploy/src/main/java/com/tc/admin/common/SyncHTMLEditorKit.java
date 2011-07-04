/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.common;

import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

public class SyncHTMLEditorKit extends HTMLEditorKit {
  public SyncHTMLEditorKit() {
    super();
  }

  @Override
  public Document createDefaultDocument() {
    Document doc = super.createDefaultDocument();
    if (doc instanceof AbstractDocument) {
      // force synchronous loading, fixes DEV-5112
      // there are JEditorPane threading bugs, see http://forums.sun.com/thread.jspa?threadID=5391050
      ((AbstractDocument) doc).setAsynchronousLoadPriority(-1);
    }
    return doc;
  }
}
