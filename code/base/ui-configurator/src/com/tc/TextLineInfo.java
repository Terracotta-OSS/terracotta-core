/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

public class TextLineInfo implements java.io.Serializable {
  private int[] m_lines;
  
  public TextLineInfo() {
    super();
  }
  
  public TextLineInfo(File file)
    throws ConcurrentModificationException,
           IOException
  {
    this();
    setFile(file);
  }

  public TextLineInfo(Reader reader)
    throws ConcurrentModificationException,
           IOException
  {
    this();
    setReader(reader);
  }
  
  public void setFile(File file)
    throws ConcurrentModificationException,
           IOException
  {
    initLines(new InputStreamReader(new FileInputStream(file)));
  }

  public void setReader(Reader reader)
    throws ConcurrentModificationException,
           IOException
  {
    initLines(reader);
  }
  
  private void initLines(Reader reader)
    throws ConcurrentModificationException,
           IOException
  {
    ArrayList list = new ArrayList();
    
    try {
      BufferedReader br = new BufferedReader(reader);
      String         s;
      
      while((s = br.readLine()) != null) {
        list.add(s);
      }
    } catch(IOException e) {
      IOUtils.closeQuietly(reader);
      throw e;
    } catch(ConcurrentModificationException e) {
      IOUtils.closeQuietly(reader);
      throw e;
    } finally {
      IOUtils.closeQuietly(reader);
    }
    
    int size = list.size();
    m_lines = new int[size];
    for(int i = 0; i < size; i++) {
      m_lines[i] = ((String)list.get(i)).length()+1;
    }
  }

  public int lineSize(int line) {
    if(line < 0 || line > m_lines.length) {
      return 0;
    }
    return m_lines[line];
  }
  
  public int offset(int line) {
    int result = 0;
    
    if(line == 0) return 0;
    
    for(int i = 0; i < line; i++) {
      result += m_lines[i];
    }

    return result;
  }
  
  public int offset(int line, int col) {
    if(line < 0 || line > m_lines.length) {
      return 0;
    }
    int result = offset(line);
    if(col > 0) {
      result += col;
    }
    return result;
  }
}
