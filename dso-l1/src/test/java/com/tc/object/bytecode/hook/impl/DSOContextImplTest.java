package com.tc.object.bytecode.hook.impl;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class DSOContextImplTest {

  @Test
  public void testAccessorToSecretIsThere() {
    try {
      // see com.terracotta.toolkit.express.TerracottaInternalClientImpl.setSecretHackOMFG()
      final Method getSecret = DSOContextImpl.class.getDeclaredMethod("getSecret");
      assertThat(Modifier.isPublic(getSecret.getModifiers()), is(true));
      assertThat(getSecret.getReturnType(), sameInstance((Object) byte[].class));
    } catch (NoSuchMethodException e) {
      fail("This method might seem useless, but is used through reflection");
    }
  }
}
