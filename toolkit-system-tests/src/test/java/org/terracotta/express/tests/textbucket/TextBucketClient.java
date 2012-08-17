/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.textbucket;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.express.tests.util.ClusteredStringBuilder;
import org.terracotta.express.tests.util.ClusteredStringBuilderFactory;
import org.terracotta.express.tests.util.ClusteredStringBuilderFactoryImpl;
import org.terracotta.toolkit.Toolkit;

import junit.framework.Assert;

public class TextBucketClient extends ClientBase {

  private static final String TEXT_BUCKET_CLIENT = "TextBucketClient";
  public static int           NODE_COUNT         = 6;

  public TextBucketClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new TextBucketClient(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    ClusteredStringBuilderFactory csbFactory = new ClusteredStringBuilderFactoryImpl(toolkit);
    ClusteredStringBuilder bucket = csbFactory.getClusteredStringBuilder("TextBucketClientBucket");
    int index = getBarrierForAllClients().await();
    if (index < NODE_COUNT - 1) {
      bucket.append(TEXT_BUCKET_CLIENT);
      getBarrierForAllClients().await();
    } else {
      // Last Client
      getBarrierForAllClients().await();
      verify(bucket.toString());
    }

  }

  private void verify(String string) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < NODE_COUNT - 1; i++) {
      sb.append(TEXT_BUCKET_CLIENT);
    }
    Assert.assertEquals(sb.toString(), string);

  }
}
