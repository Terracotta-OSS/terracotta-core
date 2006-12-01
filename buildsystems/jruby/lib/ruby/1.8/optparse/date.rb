#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require 'optparse'
require 'date'

OptionParser.accept(DateTime) do |s,|
  begin
    DateTime.parse(s) if s
  rescue ArgumentError
    raise OptionParser::InvalidArgument, s
  end
end
OptionParser.accept(Date) do |s,|
  begin
    Date.parse(s) if s
  rescue ArgumentError
    raise OptionParser::InvalidArgument, s
  end
end
