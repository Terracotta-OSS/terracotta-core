/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso.editors.xml;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ColorManager {
  protected Map<RGB, Color> fColorTable = new HashMap<RGB, Color>(10);

  public void dispose() {
    Iterator e = fColorTable.values().iterator();
    while (e.hasNext())
      ((Color) e.next()).dispose();
  }

  public Color getColor(RGB rgb) {
    Color color = fColorTable.get(rgb);
    if (color == null) {
      color = new Color(Display.getCurrent(), rgb);
      fColorTable.put(rgb, color);
    }
    return color;
  }
}
