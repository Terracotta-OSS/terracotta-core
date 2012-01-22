/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  public Throwable getProximateCause(Throwable t) {
    return getHelperFor(t).getProximateCause(t);
  }

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

    public boolean accepts(Throwable t) {
      return true;
    }
    
    public Throwable getProximateCause(Throwable t) {
      return t;
    }

    public Throwable getUltimateCause(Throwable t) {
      return t;
    }
    
  }

}
