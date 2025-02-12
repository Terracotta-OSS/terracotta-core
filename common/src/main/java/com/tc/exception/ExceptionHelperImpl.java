/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.exception;

import java.util.LinkedList;
import java.util.List;

/**
 * Helper for extracting proximate cause and ultimate cause from exceptions.
 */
public class ExceptionHelperImpl implements ExceptionHelper {
  
  private final List<ExceptionHelper> helpers = new LinkedList<ExceptionHelper>();
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
    for (ExceptionHelper helper : helpers) {
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
