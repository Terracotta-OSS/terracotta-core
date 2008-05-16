/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;
import com.tc.util.AATree.AANode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AATreeTest extends TCTestCase {

  public void testTraversal() {
    AATree t = new AATree();
    t.insert(new Integer(25));
    t.insert(new Integer(10));
    t.insert(new Integer(1));
    t.insert(new Integer(4));
    t.insert(new Integer(15));
    t.insert(new Integer(8));
    t.insert(new Integer(11));
    t.insert(new Integer(10));
    t.insert(new Integer(9));
    t.insert(new Integer(13));
    t.insert(new Integer(2));
    t.insert(new Integer(23));
    t.insert(new Integer(35));
    t.insert(new Integer(33));
    t.insert(new Integer(17));
    t.insert(new Integer(29));
    t.insert(new Integer(19));

    List arrInorder = new ArrayList();
    List arrIterator = new ArrayList();

    Inorder(t.getRoot(), arrInorder);

    Iterator i = t.iterator();

    Comparable prev = new Integer(0);
    while (i.hasNext()) {
      Comparable curr = (Comparable) i.next();
      // assert that iteration gives sorted result
      Assert.eval(curr.compareTo(prev) > 0);
      arrIterator.add(curr);
    }

    // assert that iterator does inorder traversal
    Assert.eval(arrInorder.equals(arrIterator));
  }

  private void Inorder(AANode root, List arrInorder) {
    if (root == AATree.nullNode) return;
    Inorder(root.left, arrInorder);
    arrInorder.add(root.element);
    Inorder(root.right, arrInorder);
  }

}
