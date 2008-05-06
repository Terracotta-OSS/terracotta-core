#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - copy the top level notices file for the enterprise kit
  protected
  def postscript(ant, build_environment, product_directory, *args)
    ent_top = FilePath.new(@basedir, '..', '..', '..').canonicalize
    notices_dir = FilePath.new(ent_top, 'kits', 'source', 'docs', 'notices').canonicalize
    ant.copy(:todir => product_directory.to_s, :overwrite => true) {
      ant.fileset(:dir => notices_dir.to_s, :includes => '*.txt, *.pdf')
    }
  end
end
