/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.proxy;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO document class
 * 
 * @author Jonas Bon&#233;r
 */
public class ClassBytecodeRepository {
  // private static final String BYTECODE_DIR = "_pbc";
  private static final byte[]                  EMPTY_BYTE_ARRAY  = new byte[] {};

  private static final ClassBytecodeRepository soleInstance      = new ClassBytecodeRepository();

  // DSO Shared Root
  private final Map                            nameToBytecodeMap = new HashMap();

  public static void storeBytecode(final byte[] bytes, final String name) {
    synchronized (soleInstance.nameToBytecodeMap) {
      soleInstance.nameToBytecodeMap.put(name, bytes);
      // File dir = new File(ClassBytecodeRepository.BYTECODE_DIR);
      // dir.mkdirs();
      // String fileName = (ClassBytecodeRepository.BYTECODE_DIR + File.separator + name).replace('.', '_');
      // try {
      // FileOutputStream os = new FileOutputStream(fileName);
      // os.write(bytes);
      // os.close();
      // } catch (Exception e) {
      // throw new WrappedRuntimeException(e);
      // }
    }
  }

  public static byte[] findBytecodeBy(final String name) {
    synchronized (soleInstance.nameToBytecodeMap) {
      Object bytesOrNull = soleInstance.nameToBytecodeMap.get(name);
      if (bytesOrNull == null) {
        return EMPTY_BYTE_ARRAY;
      } else {
        return (byte[]) bytesOrNull;
      }

      // String fileName = ClassBytecodeRepository.BYTECODE_DIR + File.separator + proxyName.replace('.', '_');
      // File file = new File(fileName);
      // byte[] bytes = new byte[] {};
      // try {
      // InputStream is = new FileInputStream(file);
      // long length = file.length();
      // if (length > Integer.MAX_VALUE) { throw new RuntimeException("file to large to fit into a byte array"); }
      // bytes = new byte[(int) length];
      // int offset = 0;
      // int numRead = 0;
      // while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
      // offset += numRead;
      // }
      // if (offset < bytes.length) { throw new RuntimeException("could not completely read file " + file.getName()); }
      // is.close();
      // } catch (Exception ignore) {
      // }
      // return bytes;
    }
  }
}
