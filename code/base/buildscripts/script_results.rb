#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# A subclass of StandardError to use when the build fails.
class BuildFailedError < StandardError
  # Nothing here yet
end

# An object which knows about all failures we've encountered during our build,
# and which can report those as a string, tell us whether or not the build has
# failed, or throw an exception if the build failed.
class ScriptResults
  # Creates a new object. Initially, the build will be considered to have
  # passed.
  def initialize
    @failed_reasons = [ ]
  end
    
  # Tells this object that the build has failed for a particular reason. If this
  # method is called multiple times, all reasons will be collected together and
  # presented by #to_s, #each, etc.
  def failed(why)
    @failed_reasons << why
  end
    
  # Has this build failed?
  def failed?
    ! @failed_reasons.empty?
  end
    
  # Raises a BuildFailedError if this build has failed.
  def throw_exception_if_failed
    raise BuildFailedError, to_s if failed?
  end
    
  # Returns a result code -- zero if the build has not failed, and nonzero
  # if it has. (Actually returns the number of failures if the build has failed.)
  def result_code
    @failed_reasons.length
  end
    
  # Runs the given block over each of the failure reasons.
  def each(&proc)
    @failed_reasons.each(&proc)
  end
    
  # Produces a human-readable string describing the results of the build.
  def to_s
    if failed?
      out = "Build failed:\n"            
      @failed_reasons.each { |reason| out += reason + "\n\n" }            
    else
      out = "Build passed."
    end
        
    out
  end
end
