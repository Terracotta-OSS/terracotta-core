/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.sharededitor.ui;

public interface Fontable {
  void setFontInfo(String name, int size, String text);

  void setFontName(String name);

  void setFontSize(int size);

  void setText(String text);

  String getText();

  void appendToText(char c);
}
