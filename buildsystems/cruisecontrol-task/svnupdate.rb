
# this script is called by build-terracotta.xml to svn update
# it will try to call "svn update" 3 times before crapping out
require 'tmpdir'

# clean up tmp folder
`rm -rf #{Dir.tmpdir}/terracotta*`
`rm -rf #{Dir.tmpdir}/open*`
`rm -rf #{Dir.tmpdir}/*.dat`

topdir=File.join(File.expand_path(File.dirname(__FILE__)), "..", "..")
if ENV['OS'] =~ /win/i
    topdir=`cygpath -u #{topdir}`
end

error_msg=''

3.downto(1) do 
    error_msg=`svn update #{topdir} -q --non-interactive 2>&1`
    exit(0) if $? == 0
    sleep(300)
end

fail(error_msg)
