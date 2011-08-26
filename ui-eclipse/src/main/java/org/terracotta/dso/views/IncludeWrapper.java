/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.views;

import com.terracottatech.config.Include;
import com.terracottatech.config.OnLoad;

public class IncludeWrapper {
  private InstrumentedClassesWrapper m_parent;
  private int m_index;
  
  IncludeWrapper(InstrumentedClassesWrapper parent, int index) {
    m_parent = parent;
    m_index = index;
  }
  
  int getIndex() {
    return m_index;
  }
  
  void remove() {
    m_parent.removeInclude(m_index);
  }
  
  InstrumentedClassesWrapper getParent() {
    return m_parent;
  }
  
  Include getInclude() {
    return m_parent.getIncludeArray(m_index);
  }
  
  void setClassExpression(String classExpression) {
    getInclude().setClassExpression(classExpression);
  }
  
  String getClassExpression() {
    return getInclude().getClassExpression();
  }
  
  void setHonorTransient(boolean honor) {
    Include include = getInclude();

    if(honor) {
      include.setHonorTransient(honor);
    } else {
      if(include.isSetHonorTransient()) {
        include.unsetHonorTransient();
      }
    }
  }
  
   boolean getHonorTransient() {
     return getInclude().getHonorTransient();
   }
   
  boolean isSetOnLoad() {
    return getInclude().isSetOnLoad();
  }
  
  void unsetOnLoad() {
    Include include = getInclude();
    if(include.isSetOnLoad()) {
      include.unsetOnLoad();
    }
  }
  
  OnLoad ensureOnLoad() {
    Include include = getInclude();
    OnLoad onload = include.getOnLoad();
    return onload != null ? onload : include.addNewOnLoad();
  }
  
  boolean isSetOnLoadExecute() {
    OnLoad onload = getInclude().getOnLoad();
    return onload != null && onload.isSetExecute();
  }
  
  void setOnLoadExecute(String code) {
    OnLoad onload = ensureOnLoad();
    onload.setExecute(code);
    if(onload.isSetMethod()) {
      onload.unsetMethod();
    }
  }

  boolean isSetOnLoadMethod() {
    OnLoad onload = getInclude().getOnLoad();
    return onload != null && onload.isSetMethod();
  }
  
  void setOnLoadMethod(String method) {
    OnLoad onload = ensureOnLoad();
    onload.setMethod(method);
    if(onload.isSetExecute()) {
      onload.unsetExecute();
    }
  }

  public String toString() {
    return getClassExpression();
  }
}
