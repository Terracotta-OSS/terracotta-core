#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Represents an 'archive tag': a family of relative paths that let you store
# archives associated with a build (build logs, build directory archive, etc.)
# in a consistently-named location.
class ArchiveTag
  # Creates a new ArchiveTag. build_environment should be the current BuildEnvironment.
  def initialize(build_environment)
    @build_environment = build_environment
  end
    
  # Returns a FilePath object representing where you should place a product of
  # the build that has base filename 'filename' and extension 'extension'. Currently,
  # this returns something like:
  #
  #    main/rev1.3092/standard-unit/rhel4/rh4mo0/2006/09/27/filename-main-standard-unit-rev1.3092-rh4mo0-2006-09-27-17-38-37.extension
  def to_path(filename, extension)
    user = @build_environment.build_username
    host = @build_environment.build_hostname
    revision = @build_environment.os_revision
    version = @build_environment.version
    branch = @build_environment.current_branch
    os_type = @build_environment.os_type(:nice)
    monkey_label = @build_environment.monkey_label
        
    timestamp = @build_environment.build_timestamp.strftime("%Y%m%d-%H%M")
    FilePath.new(branch, "rev%s" % revision,
      [ filename, branch, monkey_label, "rev%s" % revision, host, timestamp].join("-") + "." + extension)
  end
end
