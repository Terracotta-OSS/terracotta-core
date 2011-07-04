/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;


import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;

/**
 * Invoked when a Java module is changed in an editor. If the module
 * is part of a Terracotta project and the changed element is an
 * ICompilationUnit, inspect it.
 * 
 * @see TcPlugin.inspect
 * @see org.eclipse.jdt.core.JavaCore.addElementChangedListener
 * @see org.eclipse.jdt.core.IElementChangedListener
 */

public class ElementChangedListener implements IElementChangedListener {
  private static final boolean
    m_debug = Boolean.getBoolean("ElementChangedListener.debug");
  
  public void elementChanged(ElementChangedEvent ece) {
    TcPlugin          plugin = TcPlugin.getDefault();
    IJavaElementDelta delta  = ece.getDelta(); 
    int               kind   = delta.getKind();
    int               flags  = delta.getFlags();
    
    if(m_debug) {
      dump(delta);
    }
   
    switch(kind) {
      case IJavaElementDelta.CHANGED:
      case IJavaElementDelta.ADDED:
      {
        if(/*(flags & IJavaElementDelta.F_CONTENT) != 0 ||
           (flags & IJavaElementDelta.F_PRIMARY_RESOURCE) != 0 ||*/
           (flags & IJavaElementDelta.F_AST_AFFECTED) != 0/* ||
           ((flags & IJavaElementDelta.F_CHILDREN) != 0 &&
            (flags & IJavaElementDelta.F_FINE_GRAINED) != 0)*/)
        {
          IJavaElement elem = delta.getElement();
                  
          if(plugin.hasTerracottaNature(elem)) {
            if(elem instanceof ICompilationUnit) {
              TcPlugin.getDefault().inspect((ICompilationUnit)elem);
            }
          }
        }
      }
    }
  }
  
  private static void dump(IJavaElementDelta delta) {
    int          kind  = delta.getKind();
    int          flags = delta.getFlags();
    StringBuffer sb    = new StringBuffer();
    
    sb.append(delta);
    
    switch(kind) {
      case IJavaElementDelta.ADDED:
        sb.append(" ADDED");
        IJavaElement movedFrom = delta.getMovedFromElement();
        if(movedFrom != null) {
          sb.append("moved_from: " + movedFrom);
        }
        break;
      case IJavaElementDelta.REMOVED:
        sb.append(" REMOVED");
        IJavaElement movedTo = delta.getMovedToElement();
        if(movedTo != null) {
          sb.append("moved_to: " + movedTo);
        }
        break;
      case IJavaElementDelta.CHANGED:
        sb.append(" CHANGED");
        break;
    }
    
    if((flags & IJavaElementDelta.F_CONTENT) != 0) {
      sb.append(" F_CONTENT");
    }
    if((flags & IJavaElementDelta.F_MODIFIERS) != 0) {
      sb.append(" F_MODIFIERS");
    }
    if((flags & IJavaElementDelta.F_CHILDREN) != 0) {
      sb.append(" F_CHILDREN");
      
      IJavaElementDelta[] children = delta.getAffectedChildren();
      if(children != null && children.length > 0) {
        sb.append("\n");
        if(children.length == 2) {
          if(children[0].getKind() == IJavaElementDelta.ADDED &&
              children[1].getKind() == IJavaElementDelta.REMOVED)
           {
             IJavaElement fromElem = children[1].getElement();
             IJavaElement toElem   = children[0].getElement();
             
             if(fromElem.getElementType() == toElem.getElementType()) {
               sb.append(fromElem.getElementName() + " moved to " + toElem.getElementName());
             }
           }
        }
        for(int i = 0; i < children.length; i++) {
          dump(children[i]);
        }
      }
    }
    if((flags & IJavaElementDelta.F_MOVED_FROM) == IJavaElementDelta.F_MOVED_FROM) {
      sb.append(" F_MOVED_FROM");
    }
    if((flags & IJavaElementDelta.F_MOVED_TO) == IJavaElementDelta.F_MOVED_TO) {
      sb.append(" F_MOVED_TO");
    }
    if((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) != 0) {
      sb.append(" F_ADDED_TO_CLASSPATH");
    }
    if((flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) != 0) {
      sb.append(" F_REMOVED_FROM_CLASSPATH");
    }
    if((flags & IJavaElementDelta.F_REORDER) != 0) {
      sb.append(" F_REORDER");
    }
    if((flags & IJavaElementDelta.F_OPENED) != 0) {
      sb.append(" F_OPENED");
    }
    if((flags & IJavaElementDelta.F_CLOSED) != 0) {
      sb.append(" F_CLOSED");
    }
    if((flags & IJavaElementDelta.F_SUPER_TYPES) != 0) {
      sb.append(" F_SUPER_TYPES");
    }
    if((flags & IJavaElementDelta.F_SOURCEATTACHED) != 0) {
      sb.append(" F_SOURCEATTACHED");
    }
    if((flags & IJavaElementDelta.F_SOURCEDETACHED) != 0) {
      sb.append(" F_SOURCEDETACHED");
    }
    if((flags & IJavaElementDelta.F_FINE_GRAINED) != 0) {
      sb.append(" F_FINE_GRAINED");
    }
    if((flags & IJavaElementDelta.F_ARCHIVE_CONTENT_CHANGED) != 0) {
      sb.append(" F_ARCHIVE_CONTENT_CHANGED");
    }
    if((flags & IJavaElementDelta.F_PRIMARY_WORKING_COPY) != 0) {
      sb.append(" F_PRIMARY_WORKING_COPY");
    }
    if((flags & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0) {
      sb.append(" F_CLASSPATH_CHANGED");
    }
    if((flags & IJavaElementDelta.F_PRIMARY_RESOURCE) != 0) {
      sb.append(" F_PRIMARY_RESOURCE");
    }

    System.out.println(sb.toString());
  }
}
