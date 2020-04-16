/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 */
package com.tc.async.api;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Interface for handling either single events or multiple events at one time. For more information of this kind of
 * stuff Google SEDA -Staged Event Driven Architecture
 */
public interface EventHandler<EC> extends PostInit {

  /**
   * Handle one event at a time. Called by the StageController
   * 
   * @param context
   * @throws EventHandlerException
   */
  public void handleEvent(EC context) throws EventHandlerException;

  /**
   * Handle multiple events at once in a batch. This can be more performant because it avoids context switching
   * 
   * @param context
   * @throws EventHandlerException
   */
  public void handleEvents(Collection<EC> context) throws EventHandlerException;

  /**
   * Shut down the stage
   */
  public void destroy();
  
  public static <EC> Sink<EC> directSink(EventHandler<EC> handler) {
    return (e)->{
      try {
        handler.handleEvent(e);
      } catch (EventHandlerException ee) {
        throw new RuntimeException(ee);
      }
    };
  }

  public static <EC> EventHandler<EC> consumer(Consumer<EC> handler) {
    return new AbstractEventHandler<EC>() {
      @Override
      public void handleEvent(EC context) throws EventHandlerException {
        handler.accept(context);
      }
    };
  }
}
