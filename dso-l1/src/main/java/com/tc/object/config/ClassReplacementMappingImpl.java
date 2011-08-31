/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.util.Assert;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ClassReplacementMappingImpl implements ClassReplacementMapping {
  private final Map<String, List<Replacement>> classNamesMapping               = new HashMap();

  private final Map<String, String>            classNamesSlashesReverseMapping = new HashMap();
  private final Map<String, String>            asmTypesReverseMapping          = new HashMap();

  public synchronized void addMapping(String originalClassName, String replacementClassName, URL replacementResource,
                                      ClassReplacementTest test) {
    // XXX: why are these not errors?
    if (null == originalClassName || 0 == originalClassName.length() || null == replacementClassName
        || 0 == replacementClassName.length()) { return; }

    List<Replacement> replacements = classNamesMapping.get(originalClassName);
    if (replacements != null) {
      verifyTests(originalClassName, replacements);
    } else {
      replacements = new ArrayList<Replacement>();
      classNamesMapping.put(originalClassName, replacements);
    }

    replacements.add(new Replacement(replacementClassName, replacementResource, test));

    String originalClassNameSlashes = originalClassName.replace('.', '/');
    String replacementClassNameSlashes = replacementClassName.replace('.', '/');
    classNamesSlashesReverseMapping.put(replacementClassNameSlashes, originalClassNameSlashes);
    asmTypesReverseMapping.put(ensureAsmType(replacementClassNameSlashes), ensureAsmType(originalClassNameSlashes));
  }

  public synchronized Replacement getReplacement(String originalClassName, ClassLoader loader) {
    List<Replacement> replacements = classNamesMapping.get(originalClassName);
    if (replacements == null) { return null; }

    for (Replacement r : replacements) {
      ClassReplacementTest test = r.getTest();
      if (test == null) {
        Assert.assertEquals(1, replacements.size());
        return r;
      }

	  // XXX: should we run all the tests to see if one than one "accepts" it?
      if (test.accepts(originalClassName, loader)) { return r; }
    }

    return null;
  }

  public synchronized String getOriginalClassNameSlashes(String replacement) {
    String original = classNamesSlashesReverseMapping.get(replacement);
    if (null == original) {
      original = replacement;
    }
    return original;
  }

  public synchronized String getOriginalAsmType(String replacement) {
    String original = asmTypesReverseMapping.get(replacement);
    if (null == original) {
      original = replacement;
    }
    return original;
  }

  public synchronized String ensureOriginalAsmTypes(String s) {
    if (s != null) {
      Iterator it = asmTypesReverseMapping.entrySet().iterator();
      Map.Entry entry;
      String original;
      String replacement;
      while (it.hasNext()) {
        entry = (Map.Entry) it.next();
        original = (String) entry.getKey();
        replacement = (String) entry.getValue();

        if (s.indexOf(original) != -1) {
          s = s.replaceAll(original, replacement);
        }
      }
    }
    return s;
  }

  private static void verifyTests(String origClasName, List<Replacement> replacements) {
    // verify that existing mappings have tests associated with them
    for (Replacement r : replacements) {
      if (r.getTest() == null) {
        // 
        throw new IllegalStateException("Class replacement already exists for " + origClasName
                                        + " but it was not registered with a replacement test");
      }
    }
  }

  private static String ensureAsmType(String classNameSlashes) {
    if (null == classNameSlashes || 0 == classNameSlashes.length()) { return classNameSlashes; }

    if (classNameSlashes.charAt(0) != 'L' && classNameSlashes.charAt(classNameSlashes.length() - 1) != ';') {
      classNameSlashes = 'L' + classNameSlashes + ';';
    }
    return classNameSlashes;
  }

}