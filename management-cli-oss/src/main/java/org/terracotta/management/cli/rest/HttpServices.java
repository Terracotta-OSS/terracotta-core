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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * @author Ludovic Orban
 */
public class HttpServices {

  private static DefaultHttpClient httpClient;

  public static void initHttpClient() throws IOException, ClassNotFoundException {
    if (httpClient == null) {
      httpClient = new DefaultHttpClient();
      //CookieStore cookieStore = (CookieStore) load("rest-client-cookies.bin");
      //if (cookieStore != null) {
      //  httpClient.setCookieStore(cookieStore);
      //}
    }
  }

  public static HttpClient getHttpClient() {
    return httpClient;
  }

  public static void disposeOfHttpClient() throws IOException {
    if (httpClient != null) {
      //save("rest-client-cookies.bin", httpClient.getCookieStore());
      httpClient = null;
    }
  }


  private static Object load(String filename) throws IOException, ClassNotFoundException {
    File path = new File(System.getProperty("user.home"), ".tc/mgmt");
    File file = new File(path, filename);

    if (!file.exists()) {
      return null;
    }

    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
    try {
      return ois.readObject();
    } finally {
      ois.close();
    }
  }

  private static void save(String filename, Object o) throws IOException {
    File path = new File(System.getProperty("user.home"), ".tc/mgmt");
    File file = new File(path, filename);
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
    try {
      objectOutputStream.writeObject(o);
    } finally {
      objectOutputStream.close();
    }
  }

}
