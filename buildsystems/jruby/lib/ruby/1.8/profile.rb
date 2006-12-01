#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require 'profiler'

END {
  Profiler__::print_profile(STDERR)
}
Profiler__::start_profile
