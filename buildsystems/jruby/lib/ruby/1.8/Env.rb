#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Env.rb -- imports environment variables as global variables, Perlish ;(
# Usage:
#
#  require 'Env'
#  p $USER
#  $USER = "matz"
#  p ENV["USER"]

require 'importenv'

if __FILE__ == $0
  p $TERM
  $TERM = nil
  p $TERM
  p ENV["TERM"]
  $TERM = "foo"
  p ENV["TERM"]
end
