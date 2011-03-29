#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Represents the environment that this build is running in. Adds a number of
# methods to the base Environment class that are specific to building code
# out of our source environment, as opposed to a more general Ruby or JRuby
# environment.

# Forward-declares the Environment class, so we can inherit from it.
class Environment
end

# A subclass of Environment that adds methods specific to building our code
# out of our source environment, as opposed to a more general Ruby or JRuby
# environment.
class BuildEnvironment < Environment
  attr_reader :ee_svninfo, :os_svninfo
  
  # Creates a new instance, given a Platform object and a configuration source.
  def initialize(platform, config_source, os_root_dir, ee_root_dir=nil)
    super(platform)
    @config_source = config_source
    @build_timestamp = Time.now
    @os_svninfo = SvnInfo.new(os_root_dir)
    @ee_svninfo = SvnInfo.new(ee_root_dir)
  end

  def is_ee_branch?
    @ee_svninfo.valid? ? true : false
  end
  
  def combo_revision
    "#{@ee_svninfo.revision}-#{@os_svninfo.revision}"
  end
  
  def ee_revision
    @ee_svninfo.revision
  end
  
  def os_revision
    @os_svninfo.revision
  end
  
  def os_last_changed_author
    @os_svninfo.last_changed_author
  end

  def ee_last_changed_author
    @os_svninfo.last_changed_author
  end
  
  # What branch are we building from? 
  # read from tc.build-control.branch in build-config.global
  # if not, try to parse from "svn info" command
  def current_branch
    return @branch unless @branch.blank?
    @branch = case @os_svninfo.url
    when /trunk/ then "trunk"
    when /branches\/private\/([^\/]+)/ then $1
    when /branches\/([^\/]+)/ then $1
    when /patches\/([^\/]+)/ then $1
    when /tags\/([^\/]+)/ then $1
    else @config_source["branch"]
    end                    
    @branch || "unkown"
  end

  
  # What host are we building on?
  def build_hostname
    hostname
  end
  
  # Are we running as a Jenkins/Hudson job?
  def jenkins?
    (ENV['HUDSON_HOME'] || ENV['JENKINS_HOME']) && ENV['JOB_NAME']
  end

  # What's the name of the user we're building as?
  def username
    if @user.nil?
      @user = JavaSystem.getProperty("user.name")
    end

    @user
  end

  # Just calls #username.
  def build_username
    username
  end

  # What version are we building? This returns '[none]' if no 'version'
  # property is set in the configuration source supplied in the constructor.
  def version
    return @version unless @version.nil?
    @version = @config_source['version'] || maven_version() || 'unknown'  
    @version
  end
  
  # return maven artifacts version defined in build-config.global
  def maven_version
    @config_source['maven.version'] || 'unknown'
  end

  # return api version defined in build-config.global
  def tim_api_version
    @config_source['tim-api.version'] || 'unknown'
  end

  def edition(flavor)
    flavor =~ /enterprise/i ? 'Enterprise' : 'Opensource'
  end
  
  # When was this build started? This returns a timestamp (a Time object) that's created in
  # the constructor of this object.
  def build_timestamp
    @build_timestamp
  end

  def build_timestamp_string
    @build_timestamp.strftime('%Y%m%d-%H%m%S')
  end

  # What's the label of the currently-running monkey? Returns 'unknown' if we're not in a
  # monkey environment.
  def monkey_label
    @config_source['monkey-name'] || 'monkey-label-unknown'
  end

  # What are the default JVM arguments we should supply to spawned programs? *Note*: this may
  # or may not actually be used in any kind of reasonably-consistent fashion. You have been
  # warned.
  def default_jvmargs
    [ ]
  end
  
  def patch_branch?
   # Example of patch branch https://svn.terracotta.org/repo/internal/enterprise/patches/patched_3.4.0
   #                         https://svn.terracotta.org/repo/tc/dso/patches/patched_3.4.0
   return @os_svninfo.url =~ /patches/
  end

end
