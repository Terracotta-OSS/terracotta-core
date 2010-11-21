
# this script is called by build-terracotta.xml to svn update
# it will try to call "svn update" 3 times before crapping out
require 'tmpdir'
require 'yaml'

class SvnUpdate

  def initialize(monkey_name)
    @monkey_name = monkey_name

    @os_topdir = File.join(File.expand_path(File.dirname(__FILE__)), "..", "..")
    @is_ee_branch = @os_topdir =~ /community/ ? true : false
    @ee_topdir = File.join(@os_topdir, "..") if @is_ee_branch
    
    if ENV['OS'] =~ /Windows/i
      @os_topdir=`cygpath -u #{@os_topdir}`.chomp
      @ee_topdir=`cygpath -u #{@ee_topdir}`.chomp if @is_ee_branch
    end

    @os_svninfo = YAML.load(`svn info #{@os_topdir}`)
    @ee_svninfo = YAML.load(`svn info #{@ee_topdir}`) if @is_ee_branch
    log "ee svninfo: #{@ee_svninfo}" if @is_ee_branch
    
    @branch  = get_branch()
    build_archive_dir = ENV['OS'] =~ /Windows/i ? 'o:/archive' : '/shares/monkeyshare/output/archive'
    
    rev_file = File.join(build_archive_dir, "monkey-police", @branch, "good_rev.yml")
    if File.exists?(rev_file)
      File.open(rev_file) { |f| @good_revisions = YAML.load(f) }
    else
      @good_revisions = { "os" => 0, "ee" => 0}
    end
    log "good revisions #{@good_revisions}"
    clean_up_temp_dir
  end
  
  def update
    do_update(@os_topdir, @os_svninfo, @good_revisions['os'])
    do_update(@ee_topdir, @ee_svninfo, @good_revisions['ee']) if @is_ee_branch
  end 
  
  private
  def clean_up_temp_dir
    `rm -rf #{Dir.tmpdir}/terracotta*`
    `rm -rf /var/tmp/terracotta*`
    `rm -rf #{Dir.tmpdir}/open*`
    `rm -rf #{Dir.tmpdir}/*.dat`
    `rm -rf #{Dir.tmpdir}/sprint*`
    `rm -rf #{Dir.tmpdir}/asm*`
    `rm -rf #{Dir.tmpdir}/Jetty*`
  end

  def log(msg)
    File.open(File.join(Dir.tmpdir, "svnupdate.log"), "a") do |f|
      f.puts("#{Time.now}: #{msg}")
    end
  end
  
  # assume OS and EE branch names are the same
  def get_branch    
    branch = case @os_svninfo['URL']
    when /trunk/ then "trunk"
    when /branches\/private\/([^\/]+)/ then $1
    when /branches\/([^\/]+)/ then $1
    when /patches\/([^\/]+)/ then $1
    when /tags\/([^\/]+)/ then $1
    else fail("Can't determine which branch I'm operating on")
    end
    branch
  end

  
  def get_revision(svninfo)
    svninfo["Last Changed Rev"].to_i
  end
  
  
  # attempt svn update 3 times if encounter errors
  def svn_update_with_error_tolerant(path, revision)
    msg = ''
    command = "svn update -r#{revision} --ignore-externals '#{path}' 2>&1"
    log "command #{command}"
    3.downto(1) do
      msg = `#{command}`
      log "result: #{msg}"
      return if $? == 0
      sleep(5*60)
      `svn cleanup '#{path}'`
    end
    fail(msg)
  end

  def do_update(path, svninfo, good_rev)
    current_rev = get_revision(svninfo)
    log "path: #{path}"
    log "current rev: #{current_rev}"
    log "good_rev: #{good_rev}"
    if @monkey_name == "monkey-police" || @monkey_name == "artifacts" || @monkey_name == "test-monkey" || good_rev == 0
      svn_update_with_error_tolerant(path, "HEAD")
      return
    end
    
    if current_rev < good_rev
      svn_update_with_error_tolerant(path, good_rev)
      return
    end
  end
  
end # class SvnUpdate

svn = SvnUpdate.new(ARGV[0])
svn.update
