package com.tc.admin.common;

import java.awt.LayoutManager;

import org.dijon.Container;

public class XContainer extends Container {
  private XTreeNode m_node;

  public XContainer() {
    super();
  }

  public XContainer(LayoutManager layout) {
    super(layout);
  }

  public XContainer(XTreeNode node) {
    this();
    setNode(node);
  }

  public void setNode(XTreeNode node) {
    m_node = node;
  }

  public XTreeNode getNode() {
    return m_node;
  }

  public void tearDown() {
    removeAll();
    setNode(null);
  }
}
