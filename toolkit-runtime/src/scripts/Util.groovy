public class Util {
  static def preparePom(project) {
    def ant = new AntBuilder()
    def filteredPom = new File(project.build.directory, "dependency-reduced-pom.xml");
    ant.replace(file: filteredPom,
                token: '${toolkit-api-version}',
                value: project.properties['toolkit-api-version'])
  }
}