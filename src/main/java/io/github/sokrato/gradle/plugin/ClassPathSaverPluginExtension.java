package io.github.sokrato.gradle.plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * to customize manifest key:
 * <p>
 * import io.github.sokrato.gradle.plugin.ClassPathSaverPluginExtension
 * the&lt;ClassPathSaverPluginExtension&gt;().revisionKey = "GitRevision"
 */
public class ClassPathSaverPluginExtension {
    public List<Object> dependsOn = Collections.singletonList("test");
    /**
     * Two kinds of choices:
     * - sourceSet name, like main, test
     * - configuration name, like runtimeClasspath, testRuntimeClasspath
     */
    public List<String> configs = Arrays.asList("main", "test");
    public String dir = ".cp";
}
