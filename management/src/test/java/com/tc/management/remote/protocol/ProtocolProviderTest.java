/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class ProtocolProviderTest {

  @Test
  public void testJmxEnvSetsClassLoaderDelegatingToThreadContextClassLoader() throws Exception {
    Map env = new HashMap();
    ProtocolProvider.addTerracottaJmxProvider(env);

    assertThat(env.get("jmx.remote.default.class.loader"), is(notNullValue()));
    assertThat(env.get("jmx.remote.protocol.provider.class.loader"), is(notNullValue()));

    ClassLoader cl = (ClassLoader)env.get("jmx.remote.default.class.loader");
    assertSame(cl.loadClass("com.tc.management.remote.protocol.ProtocolProvider"), ProtocolProvider.class);

    try {
      cl.loadClass("bla.bla.Blah");
      fail("expected ClassNotFoundException");
    } catch (ClassNotFoundException e) {
      // expected
    }

    ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(new ClassLoader() {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
          if (name.equals("bla.bla.Blah")) {
            return String.class;
          }
          return super.loadClass(name);
        }
      });

      assertSame(cl.loadClass("bla.bla.Blah"), String.class);
    } finally {
      Thread.currentThread().setContextClassLoader(oldContextClassLoader);
    }
  }

}
