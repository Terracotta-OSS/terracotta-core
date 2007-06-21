/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.actions;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class HelpHandler extends BaseMenuCreator {
  public HelpHandler() {
    super();
  }

  public Menu getMenu(Control parent) {
    Menu menu = null;
    
    buildMenu(menu = new Menu(parent));

    return menu;
  }

  public Menu getMenu(Menu parent) {
    Menu menu = null;
    
    buildMenu(menu = new Menu(parent));

    return menu;
  }

  protected void fillMenu(Menu menu) {
    addMenuAction(menu,
        new HelpAction("Concept and Architecture Guide",
          "http://www.terracotta.org/kit/reflector?kitID=2.4&pageID=ConceptAndArchGuide"));
    addMenuAction(menu,
        new HelpAction("Configuration Guide and Reference",
          "http://www.terracotta.org/kit/reflector?kitID=2.4&pageID=ConfigGuideAndRef"));
    addMenuAction(menu,
        new HelpAction("Slider Tutorial",
          "http://www.terracotta.org/kit/reflector?kitID=2.4&pageID=PojoTutorial"));
    addMenuAction(menu,
        new HelpAction("Plugin Reference",
          "http://www.terracotta.org/kit/reflector?kitID=2.4&pageID=PluginReferenceGuide"));
  }

  protected IJavaElement getJavaElement(ISelection selection) {
    return null;
  }
}
