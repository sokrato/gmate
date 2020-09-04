package io.github.sokrato.gradle.plugin.task;

import io.github.sokrato.gradle.plugin.ClassPathSaverPluginExtension;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ClassPathSaverTask extends DefaultTask {
    private Logger logger;
    private Project project;
    private ClassPathSaverPluginExtension ext;

    public void init(Project project, ClassPathSaverPluginExtension ext) {
        this.project = project;
        this.logger = project.getLogger();
        this.ext = ext;

        dependsOn(ext.dependsOn.toArray());
        setGroup("dev");
        setDescription("saves classpath in file");

        doLast(this::saveClassPaths);
    }

    private void saveClassPaths(Task task) {
        Map<String, Set<File>> paths = buildSourceSetsClassPaths(project);
        Path root = ensureDirectory(ext.dir).toPath();
        ext.configs.forEach(name -> {
            if (!paths.containsKey(name)) {
                logger.warn("sourceSet not defined: {}", name);
                return;
            }
            String cp = paths.get(name)
                    .stream()
                    .map(File::toString)
                    .collect(Collectors.joining(":"));

            File target = root.resolve(name).toFile();
            save(target, cp);
            logger.warn("saved {} classpath in {}", name, target);
        });
    }

    private Map<String, Set<File>> buildSourceSetsClassPaths(Project project) {
        Map<String, Set<File>> paths = new HashMap<>();

        SourceSetContainer sourceSets = project.getConvention()
                .getPlugin(JavaPluginConvention.class)
                .getSourceSets();

        sourceSets.getAsMap().forEach((name, set) -> {
            paths.put(name, set.getOutput().getFiles());
        });

        if (paths.containsKey("main")) {
            Set<File> files = new HashSet<>(paths.get("main"));

            Set<File> rt = project.getConfigurations()
                    .getByName("runtimeClasspath")
                    .getFiles();
            files.addAll(rt);
            paths.put("main", files);
        }

        if (paths.containsKey("test")) {
            Set<File> files = new HashSet<>(paths.get("test"));
            files.addAll(paths.getOrDefault("main", Collections.emptySet()));

            Set<File> testRT = project.getConfigurations()
                    .getByName("testRuntimeClasspath")
                    .getFiles();
            files.addAll(testRT);
            paths.put("test", files);
        }
        return paths;
    }

    private File ensureDirectory(String name) {
        File dir = project.file(name);
        if (!(dir.exists() || dir.mkdirs())) {
            throw new RuntimeException("failed to create dir: " + dir);
        }

        if (!dir.isDirectory()) {
            throw new RuntimeException("file exists but not a dir: " + dir);
        }

        return dir;
    }

    private void save(File file, String classPath) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(classPath);
        } catch (IOException ex) {
            throw new RuntimeException("cannot write to: " + file, ex);
        }
    }
}
