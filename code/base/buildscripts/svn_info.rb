
class SvnInfo
  def initialize(dir)    
    @os_svninfo = {}
    @os_svninfo["Last Changed Rev"] = "unknown"
    @os_svninfo["Last Changed Author"] = "unknown"
    @os_svninfo["Last Changed Date"] = "unknown"
    @os_svninfo["URL"] = "unknown"  
    svn_info = `svn info '#{dir}' 2>&1`
    if $? == 0 && !svn_info.blank?        
      begin
        @os_svninfo.merge!(YAML::load(svn_info))
      rescue
        STDERR.puts("Error retrieving SVN info: #{svn_info}")
      end
    end   
  end
  
  def last_changed_rev
    @os_svninfo["Last Changed Rev"]
  end
  
  def last_changed_author
    @os_svninfo["Last Changed Author"]
  end
  
  def last_changed_date
    @os_svninfo["Last Changed Date"]
  end
  
  def url
    @os_svninfo["URL"]
  end
  
  def current_revision
    last_changed_rev
  end
end
