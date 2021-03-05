/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.voter;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.WrappedParameter;
import com.beust.jcommander.internal.Lists;
import org.terracotta.voter.options.OptionsParsing;
import org.terracotta.voter.options.Usage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.System.lineSeparator;

public class CustomJCommander extends JCommander {

  public CustomJCommander(OptionsParsing object) {
    super(object);
    setUsageFormatter(new UsageFormatter(this, object));
  }


  @Override
  public Map<String, JCommander> getCommands() {
    // force an ordering of commands by name
    return new TreeMap<>(super.getCommands());
  }


  private static class UsageFormatter extends DefaultUsageFormatter {
    private final JCommander commander;
    private final OptionsParsing object;

    private UsageFormatter(JCommander commander, OptionsParsing object) {
      super(commander);
      this.commander = commander;
      this.object = object;
    }

    @Override
    public void usage(StringBuilder out, String indent) {
      appendOptions(commander, out, indent);
    }


    private void appendOptions(JCommander jCommander, StringBuilder out, String indent) {
      out.append("Usage: ");
      String usage = object.getClass().getAnnotation(Usage.class).value();
      out.append(usage.replace(lineSeparator(), lineSeparator() + "    " + indent)).append(lineSeparator());
      // Align the descriptions at the "longestName" column
      List<ParameterDescription> sorted = Lists.newArrayList();
      int colSize = Integer.MIN_VALUE;
      for (ParameterDescription pd : jCommander.getParameters()) {
        if (!pd.getParameter().hidden()) {
          sorted.add(pd);
          int length = pd.getParameterAnnotation().names()[0].length();
          if (length > colSize) {
            colSize = length;
          }
        }
      }

      // Display all the names and descriptions
      if (sorted.size() > 0) {

        // Sort the options by only considering the first displayed option
        // which will be the one with 1 dash (i.e. -help).
        sorted.sort(Comparator.comparing(pd -> pd.getParameterAnnotation().names()[0]));

        out.append(indent).append("Options:").append(lineSeparator());

        for (ParameterDescription pd : sorted) {
          WrappedParameter parameter = pd.getParameter();
          out.append(indent)
              .append("    ")
              .append(pad(pd.getParameterAnnotation().names()[0], colSize))
              .append(parameter.required() ? " (required)    " : " (optional)    ")
              .append(pd.getDescription())
              .append(lineSeparator());
        }
      }
    }
  }

  private static String pad(String str, int colSize) {
    while (str.length() < colSize) {
      str += " ";
    }
    return str;
  }
}