/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.longrunning;


public class Tree {
  private Tree[] children;

  public void makeTree(int breadth, int depth) {
    children = new Tree[breadth];
    if (depth < 1) return;
    for (int b = 0; b < breadth; b++) {
      children[b] = new Tree();
      children[b].makeTree(breadth, depth -1);
    }
  }
}