package com.tc.admin.common;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

public class PagedView extends XContainer implements ComponentListener {
  private String             currentPage;

  public static final String PROP_CURRENT_PAGE = "CurrentPage";

  public PagedView() {
    super(new CardLayout());
  }

  @Override
  public void setLayout(LayoutManager layout) {
    if (layout == null || !(layout instanceof CardLayout)) {
      layout = new CardLayout();
    }
    super.setLayout(layout);
  }

  public void setPage(String page) {
    if (page != null && !page.equals(currentPage)) {
      String oldPage = this.currentPage;
      this.currentPage = page;
      ((CardLayout) getLayout()).show(this, page);
      firePropertyChange(PROP_CURRENT_PAGE, oldPage, page);
    }
    revalidate();
    repaint();
  }

  public String getPage() {
    return currentPage;
  }

  public Component getPage(String name) {
    if (name == null) return null;
    for (Component comp : getComponents()) {
      if (name.equals(comp.getName())) { return comp; }
    }
    return null;
  }

  public boolean hasPage(String name) {
    if (name == null) return false;
    for (Component comp : getComponents()) {
      if (name.equals(comp.getName())) { return true; }
    }
    return false;
  }

  public void addPage(Component page) {
    super.add(page, page.getName(), -1);
    page.addComponentListener(this);
  }

  @Override
  public Component add(Component comp) {
    super.add(comp, comp.getName(), -1);
    comp.addComponentListener(this);
    return comp;
  }

  @Override
  public Component add(Component comp, int index) {
    super.add(comp, comp.getName(), index);
    comp.addComponentListener(this);
    return comp;
  }

  public void removePage(String name) {
    Component page = getPage(name);
    if (page != null) {
      remove(page);
      page.removeComponentListener(this);
    }
  }

  @Override
  public void remove(Component comp) {
    if (comp != null) {
      super.remove(comp);
      comp.removeComponentListener(this);
    }
  }

  @Override
  public void remove(int index) {
    Component comp = getComponent(index);
    super.remove(index);
    comp.removeComponentListener(this);
  }

  public void componentHidden(ComponentEvent e) {
    /**/
  }

  public void componentMoved(ComponentEvent e) {
    /**/
  }

  public void componentResized(ComponentEvent e) {
    /**/
  }

  public void componentShown(ComponentEvent e) {
    Component c = e.getComponent();
    setPage(c.getName());
  }
}
