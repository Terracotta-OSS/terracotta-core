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

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.terracotta.management.cli.CommandInvocationException;

/**
 * @author Ludovic Orban
 */
class PostCommand extends BaseOssHttpCommand {

  @Override
  public void doExecute(Context context) throws Exception {
    doPost(context);
  }

  private void doPost(Context context) throws IOException, CommandInvocationException {

    HttpClient httpclient = HttpServices.getHttpClient();
    HttpPost httpPost = new HttpPost(context.getUrl());
    
    if (context.getData() != null) {
      httpPost.setHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
      httpPost.setEntity(new StringEntity(context.getData()));
    }
    HttpResponse response = httpclient.execute(httpPost);
    processEntity(response.getEntity(), response.getFirstHeader("Content-Type"), context);
    
  }

  @Override
  public String helpMessage() {
    return "perform HTTP POST";
  }
}
