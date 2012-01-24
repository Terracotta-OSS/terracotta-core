/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.configuration;


public interface PresentationFactory {
  Presentation create(PresentationContext context);
}
