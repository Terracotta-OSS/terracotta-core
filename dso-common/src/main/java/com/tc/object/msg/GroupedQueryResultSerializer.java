/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.terracottatech.search.GroupedIndexQueryResultImpl;
import com.terracottatech.search.GroupedQueryResult;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.aggregator.AbstractAggregator;
import com.terracottatech.search.aggregator.Aggregator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupedQueryResultSerializer extends IndexQueryResultSerializer<GroupedQueryResult> {
  private final class GroupedResultBuilder extends IndexQueryResultBuilder {
    private List<Aggregator> aggregators;
    private Set<NVPair>      groupByAttributes;

    @Override
    protected GroupedQueryResult build() {
      return new GroupedIndexQueryResultImpl(attributes, sortAttributes, groupByAttributes, aggregators);
    }

    private GroupedResultBuilder setAggregators(List<Aggregator> aggregators) {
      this.aggregators = aggregators;
      return this;
    }

    private GroupedResultBuilder setGroupByAttributes(Set<NVPair> groupByAttributes) {
      this.groupByAttributes = groupByAttributes;
      return this;
    }

  }

  @Override
  public void serialize(GroupedQueryResult result, TCByteBufferOutput output) {
    output.writeInt(result.getGroupedAttributes().size());
    for (NVPair pair : result.getGroupedAttributes()) {
      NVPAIR_SERIALIZER.serialize(pair, output, NULL_SERIALIZER);
    }

    output.writeInt(result.getAggregators().size());
    for (Aggregator agg : result.getAggregators()) {
      try {
        agg.serializeTo(output);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    super.serialize(result, output);
  }

  @Override
  GroupedQueryResult deserializeFrom(TCByteBufferInput input) throws IOException {
    int size = input.readInt();

    Set<NVPair> groupByAttributes = new HashSet<NVPair>(size);

    for (int i = 0; i < size; i++) {
      NVPair pair = NVPAIR_SERIALIZER.deserialize(input, NULL_SERIALIZER);
      groupByAttributes.add(pair);
    }

    int aggregatorCount = input.readInt();
    List<Aggregator> aggregators = new ArrayList<Aggregator>(aggregatorCount);
    for (int i = 0; i < aggregatorCount; i++) {
      Aggregator aggregator = AbstractAggregator.deserializeInstance(input);
      aggregators.add(aggregator);
    }

    GroupedResultBuilder builder = (GroupedResultBuilder) buildCommonFields(input);
    return builder.setAggregators(aggregators).setGroupByAttributes(groupByAttributes).build();
  }

  @Override
  protected IndexQueryResultBuilder builder() {
    return new GroupedResultBuilder();
  }

}
