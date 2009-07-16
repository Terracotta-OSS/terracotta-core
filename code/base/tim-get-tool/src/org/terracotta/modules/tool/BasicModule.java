/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import java.net.URI;
import java.util.Map;

public class BasicModule extends AttributesModule implements Installable {

  private final Module owner;

  public BasicModule(Module owner, Map<String, Object> attributes, URI relativeUrlBase) {
    super(attributes, relativeUrlBase);
    this.owner = owner;
  }

  public Module owner() {
    return owner;
  }

}
