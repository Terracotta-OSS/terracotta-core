/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.texteditor.ITextEditor;

import org.terracotta.dso.JdtUtils;
import org.terracotta.dso.TcPlugin;
import org.terracotta.dso.actions.ActionUtil;

import java.util.ArrayList;

/**
 * Example marker resolution.  We may have the need to offer up
 * suggestions to the user about how to deal with problems they've
 * injected into their code or config.
 */

public class NotInstrumentedResolutionGenerator
  implements IMarkerResolutionGenerator2
{
  private static TcPlugin m_plugin = TcPlugin.getDefault();
  
  private IJavaElement m_element;

  private static final String MARKER_ID =
    "org.terracotta.dso.DeclaringTypeNotInstrumentedMarker";
  
  public boolean hasResolutions(IMarker marker) {
    try {
      return marker.getType().equals(MARKER_ID);
    } catch(CoreException ce) {/**/}
    
    return false;
  }

  private IJavaElement getSelectedElement() {
    try {
      ITextEditor editor = ActionUtil.findSelectedTextEditor();
      
      if(editor != null) {
        return JdtUtils.getElementAtOffset(editor);
      }
    } catch(CoreException ce) {/**/}
    
    return null;
  }
  
  public IMarkerResolution[] getResolutions(IMarker marker) {
    ArrayList<IMarkerResolution> list = new ArrayList<IMarkerResolution>();
    
    if((m_element = getSelectedElement()) != null) {
      IJavaProject javaProject = m_element.getJavaProject();
      IProject     project     = javaProject.getProject();
      
      list.add(new InstrumentDeclaringTypeResolution(m_element));
      
      if(m_element.getElementType() == IJavaElement.FIELD) {
        IField field = (IField)m_element;
        
        if(m_plugin.getConfigurationHelper(project).isRoot(field)) {
          list.add(new EnsureNotRootResolution((IField)m_element));
        }
        else if(m_plugin.getConfigurationHelper(project).isTransient(field)) {
          list.add(new EnsureNotTransientResolution((IField)m_element));
        }
      }
      else if(m_element.getElementType() == IJavaElement.METHOD) {
        list.add(new EnsureNotLockedResolution((IMethod)m_element));
      }
    }
    
    return list.toArray(new IMarkerResolution[0]) ;
  }
}

