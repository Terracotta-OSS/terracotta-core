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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONArray;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.terracotta.management.cli.Command;
import org.terracotta.management.cli.CommandInvocationException;

import com.jayway.jsonpath.JsonPath;

/**
 * @author Ludovic Orban
 */
public abstract class AbstractHttpCommand implements Command<Context> {

  protected HttpClient httpclient = null;
  
  public final void execute(Context context) throws CommandInvocationException {
    httpclient = HttpServices.getHttpClient();
    try {
      doExecute(context);
    } catch (Exception e) {
      if (e instanceof CommandInvocationException) {
        throw (CommandInvocationException) e;
      }
      throw new CommandInvocationException("HTTP request failed: " + getRootCauseMessage(e), e);
    } finally {
      try {
        docleanUp();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  protected abstract void doExecute(Context context) throws Exception;
  protected abstract void docleanUp() throws Exception;

  protected void processEntity(HttpEntity entity, Header contentType, Context context) throws IOException, CommandInvocationException {
    if (entity == null) {
      return;
    }

    InputStream is = entity.getContent();
    String encoding = entity.getContentEncoding() != null ? entity.getContentEncoding().getValue() : "UTF-8";

    byte[] bytes;
    try {
      bytes = toBytes(is);
    } finally {
      is.close();
    }

    List<String> jsonQueries = context.getJsonQueries();
    List<Object> jsonOutputs = new ArrayList<Object>();


    boolean containsResult = false;
    if (jsonQueries.isEmpty()) {
      DisplayServices.println(bytes, encoding, contentType.getValue());
      containsResult = bytes.length > 0;
    } else if (contentType == null || !contentType.getValue().equals("application/json")) {
      throw new RuntimeException("Cannot execute JSON query on wrong content type. " + contentType);
    } else {
      for (String jsonQuery : jsonQueries) {
        Object jsonOut = JsonPath.read(new String(bytes, encoding), jsonQuery);
        jsonOutputs.add(jsonOut);
      }


      if (jsonOutputs.size() > 0 && jsonOutputs.get(0) instanceof JSONArray) {
        JSONArray jsonArray = (JSONArray)jsonOutputs.get(0);
        for (int i = 0; i < jsonArray.size(); i++) {
          for (int j = 0; j < jsonOutputs.size(); j++) {
            Object output = jsonOutputs.get(j);
            JSONArray jsonArray2 = (JSONArray)output;
            DisplayServices.print(jsonArray2.get(i));
            containsResult = true;
            if (j < jsonOutputs.size() - 1) {
              System.out.print("\t");
            }
          }
          System.out.println("");
        }
      } else {
        Object o = jsonOutputs.get(0);
        if (o != null) {
          String output = o.toString();
          DisplayServices.println(output);
          containsResult = output.length() > 0;
        }
      }
    }

    if (!containsResult && context.isFailOnEmpty()) {
      throw new CommandInvocationException("No results received");
    }
  }

  private byte[] toBytes(InputStream is) throws IOException {
    BufferedInputStream bis = new BufferedInputStream(is);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    while (true) {
      int read = bis.read();
      if (read == -1) break;
      baos.write(read);
    }

    baos.close();
    return baos.toByteArray();
  }

  protected String getRootCauseMessage(Throwable t) {
    if (t.getCause() == null || t.getCause() == t) {
      return t.getMessage();
    }
    return getRootCauseMessage(t.getCause());
  }
}
