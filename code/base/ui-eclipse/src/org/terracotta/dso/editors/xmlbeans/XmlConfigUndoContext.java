/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.eclipse.core.resources.IProject;

import com.tc.util.event.UpdateEvent;
import com.tc.util.event.UpdateEventListener;

import java.util.HashMap;
import java.util.Map;

public final class XmlConfigUndoContext {

  private static final Map<IProject, XmlConfigUndoContext> m_contexts = new HashMap<IProject, XmlConfigUndoContext>();
  private final XmlConfigContext                           m_xmlConfigContext;
  private final Map<Integer, Undo>                         m_undoTable;

  private XmlConfigUndoContext(IProject project) {
    this.m_xmlConfigContext = XmlConfigContext.getInstance(project);
    this.m_undoTable = new HashMap<Integer, Undo>();
    registerUndoListeners();
  }

  public static synchronized XmlConfigUndoContext getInstance(IProject project) {
    if (m_contexts.containsKey(project)) return m_contexts.get(project);
    return new XmlConfigUndoContext(project);
  }

  public void undo(int eventType) {
    Undo undo = m_undoTable.get(new Integer(eventType));
    if (undo == null) return;
    int prev = ++undo.current % 2;
    XmlConfigEvent event = undo.events[prev];
    if (event != null) m_xmlConfigContext.notifyListeners(event);
  }

  private void registerUndoListeners() {
    registerUndoListener(XmlConfigEvent.SERVER_NAME);
    registerUndoListener(XmlConfigEvent.SERVER_HOST);
    registerUndoListener(XmlConfigEvent.SERVER_DSO_PORT);
    registerUndoListener(XmlConfigEvent.SERVER_JMX_PORT);
  }

  private void registerUndoListener(int type) {
    m_undoTable.put(new Integer(type), new Undo());
    m_xmlConfigContext.addListener(new UpdateEventListener() {
      public void handleUpdate(UpdateEvent e) {
        XmlConfigEvent event = (XmlConfigEvent) e;
        Undo undo = m_undoTable.get(new Integer(event.type));
        undo.current = ++undo.current % 2;
        undo.events[undo.current] = event;
      }
    }, type);
  }

  // --------------------------------------------------------------------------------

  private class Undo {
    int              current = 0;                    // other is previous value
    XmlConfigEvent[] events  = new XmlConfigEvent[2];
  }
}
