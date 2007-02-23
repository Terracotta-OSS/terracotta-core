
# this script is called by build-terracotta.xml to svn update
# it will try to call "svn update" 3 times before crapping out
require 'tmpdir'
require 'yaml'

def svn_update_with_error_tolerant(topdir)
  error_msg=''  
  3.downto(1) do 
    error_msg=`svn update #{topdir} -q --non-interactive 2>&1`
    return if $? == 0
    sleep(5*60)
  end  
  fail(error_msg)
end

# clean out temp dir
`rm -rf #{Dir.tmpdir}/terracotta*`
`rm -rf /var/tmp/terracotta*`
`rm -rf #{Dir.tmpdir}/open*`
`rm -rf #{Dir.tmpdir}/*.dat`
`rm -rf #{Dir.tmpdir}/sprint*`

# get path to top folder of the repo
topdir = File.join(File.expand_path(File.dirname(__FILE__)), "..", "..")
topdir = File.join(topdir, "..") if topdir =~ /community/

# default build-archive-dir in monkeys
build_archive_dir = "/shares/monkeyoutput"

if ENV['OS'] =~ /win/i
  topdir=`cygpath -u #{topdir}`
  build_archive_dir = "o:"    
end

currently_good_rev = 0
unless ARGV[0] == 'general-monkey'
  begin
    good_rev_file = File.join(build_archive_dir, "currently_good_rev.txt")
    File.open(good_rev_file, "r") do | f |
      currently_good_rev = f.gets.to_i
      puts "Currently good revision: #{currently_good_rev}"
    end
  rescue
    currently_good_rev = 0
  end
end

while true
  svn_update_with_error_tolerant(topdir)
  current_rev = YAML::load(`svn info #{topdir}`)["Revision"].to_i
  exit(0) if currently_good_rev == 0 || current_rev <= currently_good_rev
  sleep(5*60)
end
