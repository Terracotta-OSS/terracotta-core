/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util;

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
