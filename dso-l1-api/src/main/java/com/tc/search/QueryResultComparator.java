/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search;

import com.tc.object.metadata.NVPair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class QueryResultComparator implements Comparator<IndexQueryResult> {

  private final Collection<Comparator<IndexQueryResult>> components = new ArrayList<Comparator<IndexQueryResult>>();

  public QueryResultComparator(Collection<NVPair> sortBy) {
    for (NVPair sortAttributePair : sortBy) {
      final String attributeName = sortAttributePair.getName();
      final boolean isDesc = SortOperations.DESCENDING == sortAttributePair.getObjectValue();
      components.add(new Comparator<IndexQueryResult>() {

        @Override
        public int compare(IndexQueryResult res1, IndexQueryResult res2) {
          List<NVPair> o1 = res1.getSortAttributes();
          List<NVPair> o2 = res2.getSortAttributes();

          if (o1.size() != o2.size()) throw new IllegalArgumentException(String
              .format("Non-equal sorting for query results: %s, %s", res1, res2));
          int n = 0;
          for (NVPair sortField1 : o1) {
            if (sortField1.getName().equals(attributeName)) {
              NVPair sortField2 = o2.get(n);
              if (!(sortField1.getName().equals(sortField2.getName()) && sortField1.getType() == sortField2.getType())) throw new IllegalArgumentException(
                                                                                                                                                           String
                                                                                                                                                               .format("Query results contain incompatible sort fields: %s, %s",
                                                                                                                                                                       sortField1,
                                                                                                                                                                       sortField2));
              Comparable v1 = (Comparable) sortField1.getObjectValue();
              return v1.compareTo(sortField2.getObjectValue()) * (isDesc ? -1 : 1);
            }
            n++;
          }
          throw new IllegalArgumentException(String
              .format("Unable to locate sort attribute %s in result sort fields %s", attributeName, o1));
        }

      });
    }
  }

  @Override
  public int compare(IndexQueryResult res1, IndexQueryResult res2) {
    for (Comparator<IndexQueryResult> comp : components) {
      int res = comp.compare(res1, res2);
      if (res != 0) return res;
    }
    return 0;
  }

}
