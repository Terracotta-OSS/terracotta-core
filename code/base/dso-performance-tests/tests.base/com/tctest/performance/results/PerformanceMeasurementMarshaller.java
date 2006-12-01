/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.results;

import com.tctest.performance.generate.load.Measurement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class PerformanceMeasurementMarshaller {

  private static final String HEADER_TAG = "**%% HEADER %%**";
  private static final String DATA_TAG   = "**%% DATA %%**";

  public static void marshall(List measurementList, Header rawHeader, File file, String[] lineDescriptions)
      throws IOException {

    if (rawHeader == null) throw new NullPointerException();
    if (lineDescriptions == null) throw new NullPointerException();
    if (measurementList == null) throw new NullPointerException();
    if (!(measurementList.get(0) instanceof Measurement[])) throw new IllegalArgumentException();
    if (!(measurementList.size() == lineDescriptions.length)) throw new IllegalArgumentException();
    Header header = rawHeader.copy();

    PrintWriter out = new PrintWriter(file);
    out.println(new Date(System.currentTimeMillis()));
    out.println(PerformanceMeasurementMarshaller.class.getName());
    out.println(HEADER_TAG);
    out.println("title=" + header.title);
    out.println("duration=" + header.duration);
    out.println("xLabel=" + header.xLabel);
    out.println("yLabel=" + header.yLabel);

    for (int i = 0; i < measurementList.size(); i++) {
      out.println(DATA_TAG);
      out.println(lineDescriptions[i]);
      out.println("\"x\", \"y\"");
      Measurement[] measurements = (Measurement[]) measurementList.get(i);
      int size = measurements.length;
      for (int j = 0; j < size; j++) {
        // FIXME:
        if (measurements[j] == null) continue;
        out.println(measurements[j].x + ", " + measurements[j].y);
      }
    }
    out.flush();
    out.close();
  }

  public static Statistics deMarshall(File file) throws UnsupportedEncodingException, FileNotFoundException,
      IOException {

    Statistics stats = new PerformanceMeasurementMarshaller().new Statistics();
    List lineDescriptions = new ArrayList();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    reader.readLine(); // skip timestamp
    if (!reader.readLine().equals(PerformanceMeasurementMarshaller.class.getName())) throw new UnsupportedEncodingException();

    if (reader.readLine().equals(HEADER_TAG)) {
      Header header = createHeader();
      header.title = reader.readLine().trim().split("=")[1];
      header.duration = Integer.valueOf(reader.readLine().trim().split("=")[1]).intValue();
      header.xLabel = reader.readLine().trim().split("=")[1];
      header.yLabel = reader.readLine().trim().split("=")[1];
      stats.header = header;
    }

    if (reader.readLine().equals(DATA_TAG)) {
      lineDescriptions.add(reader.readLine()); // save line description
      reader.readLine(); // remove column header
      List datasets = new ArrayList();
      List lines = new ArrayList();
      datasets.add(lines);
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.equals(DATA_TAG)) {
          lines = new ArrayList();
          datasets.add(lines);
          lineDescriptions.add(reader.readLine()); // save line description
          reader.readLine(); // remove column header
          continue;
        }
        lines.add(line);
      }

      List measurementList = new ArrayList();
      List data;
      Measurement[] measurements;
      for (int i = 0; i < datasets.size(); i++) {
        data = (List) datasets.get(i);
        int size = data.size();
        measurements = new Measurement[size];
        for (int j = 0; j < size; j++) {
          String[] parts = ((String) data.get(j)).split(", ");
          measurements[j] = new Measurement(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
        }
        measurementList.add(measurements);
      }
      stats.measurements = measurementList;
      stats.lineDescriptions = (String[]) lineDescriptions.toArray(new String[0]);
    }
    return stats;
  }

  public static PerformanceMeasurementMarshaller.Header createHeader() {
    return new PerformanceMeasurementMarshaller().new Header();
  }

  public class Header {
    public String title;
    public String xLabel;
    public String yLabel;
    public int    duration;

    private Header copy() {
      Header newHeader = new Header();
      newHeader.title = title;
      newHeader.xLabel = xLabel;
      newHeader.yLabel = yLabel;
      newHeader.duration = duration;
      return newHeader;
    }
  }

  public class Statistics {
    public List     measurements;
    public String[] lineDescriptions;
    public Header   header;
  }
}
