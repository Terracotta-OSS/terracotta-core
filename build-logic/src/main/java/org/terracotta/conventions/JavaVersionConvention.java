package org.terracotta.conventions;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.terracotta.build.plugins.JavaVersionPlugin;

public class JavaVersionConvention implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(JavaVersionPlugin.class);

    JavaVersionPlugin.JavaVersions javaVersions = project.getExtensions().getByType(JavaVersionPlugin.JavaVersions.class);

    project.getPlugins().withType(JavaPlugin.class, javaPlugin ->
        project.getExtensions().configure(JavaPluginExtension.class, java ->
            java.toolchain(toolchain ->
                toolchain.getLanguageVersion().convention(javaVersions.getCompileVersion()))));
  }
}
