/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.exception;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper for extracting proximate cause and ultimate cause from exceptions.
 */
public class ExceptionHelperImpl implements ExceptionHelper {
  
  private final List helpers = new LinkedList();
  private final ExceptionHelper nullHelper = new NullExceptionHelper();
  
  @Override
  public boolean accepts(Throwable t) {
    return true;
  }
  
  /**
   * Add another helper to this helper.
   * @param helper Helper
   */
  public void addHelper(ExceptionHelper helper) {
    helpers.add(helper);
  }

  @Override
  public Throwable getProximateCause(Throwable t) {
    return getHelperFor(t).getProximateCause(t);
  }

  @Override
  public Throwable getUltimateCause(Throwable t) {
    Throwable rv = getProximateCause(t);
    while (rv != getProximateCause(rv)) {
      rv = getProximateCause(rv);
    }
    return rv;
    //return getHelperFor(t).getUltimateCause(t);
  }
  
  private ExceptionHelper getHelperFor(Throwable t) {
    ExceptionHelper helper;
    for (Iterator i = helpers.iterator(); i.hasNext(); ) {
      helper = (ExceptionHelper) i.next();
      if (helper.accepts(t)) return helper;
    }
    return nullHelper;
  }
  
  private static final class NullExceptionHelper implements ExceptionHelper{

    @Override
    public boolean accepts(Throwable t) {
      return true;
    }
    
    @Override
    public Throwable getProximateCause(Throwable t) {
      return t;
    }

    @Override
    public Throwable getUltimateCause(Throwable t) {
      return t;
    }
    
  }

}
