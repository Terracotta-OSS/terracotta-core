package org.terracotta.conventions;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;

import static org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP;

/**
 * Convention plugin for the Checkstyle analysis tool.
 * This convention:
 * <ul>
 *     <li>establishes the variable substitution used for the project specific supressions file location.
 *     You can see this variable substitution in {@code /config/checkstyle/checkstyle.xml}.</li>
 *     <li>registers the {@code checkstyle} uber-task that depends on all the checkstyle analysis tasks</li>
 * </ul>
 */
public class CheckstyleConvention implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(CheckstylePlugin.class);
    project.getExtensions().configure(CheckstyleExtension.class, checkstyle ->
            checkstyle.getConfigProperties().put("project_config", project.file("config/checkstyle").getAbsolutePath()));

    project.getTasks().register("checkstyle", task -> {
      task.setDescription("Run Checkstyle analysis");
      task.setGroup(VERIFICATION_GROUP);
      task.dependsOn(project.getTasks().withType(Checkstyle.class));
    });
  }
}
