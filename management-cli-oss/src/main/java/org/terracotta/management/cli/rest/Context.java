/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package org.terracotta.management.cli.rest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class Context {
  private final String url;
  private final boolean failOnEmpty;
  private final List<String> jsonQueries = new ArrayList<String>();
  private final String data;
  private final String username;
  private final String password;

  public Context(String url, List<String> jsonQueries, String data, String username, String password, boolean failOnEmpty) {
    this.url = url;
    this.jsonQueries.addAll(jsonQueries);
    this.data = data;
    this.username = username;
    this.password = password;
    this.failOnEmpty = failOnEmpty;
  }

  public String getUrl() {
    return url;
  }

  public List<String> getJsonQueries() {
    return jsonQueries;
  }

  public String getData() {
    return data;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public boolean isFailOnEmpty() {
    return failOnEmpty;
  }
}
