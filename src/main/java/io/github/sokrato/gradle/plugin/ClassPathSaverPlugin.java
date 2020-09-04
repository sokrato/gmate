package io.github.sokrato.gradle.plugin;

import io.github.sokrato.gradle.plugin.task.ClassPathSaverTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ClassPathSaverPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        ClassPathSaverPluginExtension ext = project.getExtensions()
                .create(ClassPathSaverPluginExtension.class,
                        "classPathSaver",
                        ClassPathSaverPluginExtension.class);

        project.afterEvaluate(p -> {
            p.getTasks()
                    .create("saveClassPath", ClassPathSaverTask.class)
                    .init(p, ext);
        });
    }
}
