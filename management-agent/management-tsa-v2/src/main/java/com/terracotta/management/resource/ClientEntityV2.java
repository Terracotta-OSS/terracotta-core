/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link org.terracotta.management.resource.AbstractEntityV2} representing a topology's client
 * from the management API.
 *
 * @author Ludovic Orban
 */
public class ClientEntityV2 extends AbstractTsaEntityV2 {

  private String                    productVersion;
  private final Map<String, Object> attributes = new HashMap<String, Object>();

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public String getProductVersion() {
    return productVersion;
  }

  public void setProductVersion(String productVersion) {
    this.productVersion = productVersion;
  }
}
