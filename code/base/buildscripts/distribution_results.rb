#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class DistributionResults
  attr_reader :build_dir
  attr_reader :archive_dir
    
  def initialize(build_dir)
    @build_dir   = build_dir
    @archive_dir = FilePath.new(File.dirname(build_dir.canonicalize.to_s), 'packages')
  end
    
  def clean(ant)
    @build_dir.delete
  end
end
