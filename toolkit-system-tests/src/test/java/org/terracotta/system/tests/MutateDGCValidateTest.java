/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.test.util.TestBaseUtil;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;

import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.TestConfig;

import java.util.ArrayList;
import java.util.List;

public class MutateDGCValidateTest extends AbstractToolkitTestBase {

  @Override
  protected String getTestDependencies() {
    return TestBaseUtil.jarFor(StringUtils.class);
  }

  public MutateDGCValidateTest(TestConfig testConfig) {
    super(testConfig, MutateDGCValidateTestMutator.class, MutateDGCValidateTestValidator.class);
    testConfig.getClientConfig().setParallelClients(false);
    testConfig.getL2Config().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> extraJars = new ArrayList<String>();
    extraJars.add(TestBaseUtil.jarFor(BoundedBuffer.class));
    extraJars.add(TestBaseUtil.jarFor(ParseException.class));
    return extraJars;
  }
}
