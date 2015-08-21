/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.text;

public class Banner {

  public static void errorBanner(String message) {
    System.err.println(makeBanner(message, "ERROR"));
    System.err.flush();
  }

  public static void warnBanner(String message) {
    System.err.println(makeBanner(message, "WARNING"));
    System.err.flush();
  }

  public static void infoBanner(String message) {
    System.out.println(makeBanner(message, "INFO"));
    System.out.flush();
  }
  
  private static final int MAX_LINE  = 72;
  private static final int BOX_WIDTH = MAX_LINE + 4;

  public static String makeBanner(String message, String type) {
    if (message == null) {
      message = "<no message>";
    }

    final int topStars = BOX_WIDTH - (type.length() + 2); // +2 for spaces on either side of "type"
    final int begin = topStars / 2;
    final int end = (topStars % 2 == 0) ? begin : begin + 1;

    StringBuffer buf = new StringBuffer();
    buf.append("\n");

    for (int i = 0; i < begin; i++) {
      buf.append('*');
    }

    buf.append(' ').append(type).append(' ');

    for (int i = 0; i < end; i++) {
      buf.append('*');
    }

    String[] lines = message.split("\n");

    for (int i = 0; i < lines.length; i++) {
      String[] words = lines[i].split(" ");

      int word = 0;
      if (words.length == 0) buf.append("\n* ");
      while (word < words.length) {
        int length = words[word].length();
        buf.append("\n* ").append(words[word]);
        word++;

        while (length <= MAX_LINE && word < words.length) {
          int next = words[word].length() + 1; // +1 for space
          if (length + next <= MAX_LINE) {
            buf.append(' ').append(words[word++]);
            length += next;
          } else {
            break;
          }
        }
      }
    }
    buf.append("\n");
    for (int i = 0; i < BOX_WIDTH; i++) {
      buf.append('*');
    }
    buf.append("\n");

    return buf.toString();
  }
}
