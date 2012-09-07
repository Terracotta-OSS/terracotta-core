/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.toolkit;

import java.io.File;
import java.util.LinkedList;

public class FilesDFSWalker {

  private final File path;

  public static interface FileVisitor {
    public void visitFile(File file);
  }

  public FilesDFSWalker(File path) {
    this.path = path;
  }

  public static FilesDFSWalker newWalker(File path) {
    return new FilesDFSWalker(path);
  }

  public void accept(FileVisitor visitor) {
    LinkedList<File> stack = new LinkedList<File>();
    stack.addFirst(path);
    while (!stack.isEmpty()) {
      File current = stack.removeFirst();
      visitor.visitFile(current);
      if (current.isDirectory()) {
        for (File child : current.listFiles()) {
          stack.addFirst(child);
        }
      }
    }
  }

}
