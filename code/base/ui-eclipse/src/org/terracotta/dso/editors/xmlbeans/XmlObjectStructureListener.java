/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import java.util.EventListener;

public interface XmlObjectStructureListener extends EventListener {
  void structureChanged(XmlObjectStructureChangeEvent e);
}
