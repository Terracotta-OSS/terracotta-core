/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

public interface IMapEntry extends IObject {
  IObject getKey();
  IObject getValue();
}
