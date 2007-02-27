
# this script is called by build-terracotta.xml to svn update
# it will try to call "svn update" 3 times before crapping out
require 'tmpdir'
require 'yaml'

def get_current_rev(topdir)
  YAML::load(`svn info #{topdir}`)["Last Changed Rev"].to_i
end

def svn_update_with_error_tolerant(topdir, revision)
  error_msg=''  
  3.downto(1) do 
    error_msg=`svn update #{topdir} -r #{revision} -q --non-interactive 2>&1`
    return get_current_rev(topdir) if $? == 0
    sleep(5*60)
  end  
  fail(error_msg)
end

def get_current_good_rev(file)
  currently_good_rev = 'HEAD'
  begin
    File.open(file, "r") do | f |
      currently_good_rev = f.gets.to_i    
    end
  rescue
    currently_good_rev = 'HEAD'
  end
  currently_good_rev
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

good_rev_file = File.join(build_archive_dir, "currently_good_rev.txt")
monkey_name = ARGV[0]

current_rev = get_current_rev(topdir)
current_good_rev = (monkey_name == "general-monkey") ? 'HEAD' : get_current_good_rev(good_rev_file)

if current_good_rev == 'HEAD' || current_rev <= current_good_rev 
  svn_update_with_error_tolerant(topdir, current_good_rev)
end
