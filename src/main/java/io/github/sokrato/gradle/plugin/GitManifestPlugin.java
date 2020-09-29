package io.github.sokrato.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.tasks.bundling.Jar;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class GitManifestPlugin implements Plugin<Project> {

    private final Map<String, GitInfo> cache = new HashMap<>();

    private GitManifestPluginExtension ext;

    @Override
    public void apply(Project project) {
        ext = project.getExtensions()
                .create(GitManifestPluginExtension.class,
                        "gitManifest",
                        GitManifestPluginExtension.class);

        project.afterEvaluate(p -> {
            p.getTasks()
                    .withType(Jar.class)
                    .forEach(jar -> updateManifest(p, jar));
        });
    }

    private void updateManifest(Project project, Jar jar) {
        jar.doFirst(task -> {
            GitInfo gitInfo = gitInfo(project);
            ((Jar) task).manifest(mf -> {
                Attributes attrs = mf.getAttributes();
                attrs.put(ext.revisionKey, gitInfo.getRevision());
                attrs.put(ext.remoteKey, gitInfo.getRemoteURL());
                attrs.put(ext.branchKey, gitInfo.getBranch());
            });
        });
    }

    private GitInfo gitInfo(Project project) {
        final String rootDir = project.getRootDir().toString();

        GitInfo info = cache.get(rootDir);
        if (info == null || info.expired()) {
            info = new GitInfo();
            Logger logger = project.getLogger();
            if (hasUnCommitted(logger, rootDir))
                logger.warn("You have uncommitted content! {} is not accurate!", ext.revisionKey);

            info.setRevision(gitRevision(logger, rootDir));
            info.setRemoteURL(gitRemote(logger, rootDir));
            info.setBranch(gitBranch(logger, rootDir));

            cache.put(rootDir, info);
        }
        return info;
    }

    private boolean isCwd(String dir) {
        String cwd = System.getProperty("user.dir");
        return cwd.equals(dir);
    }

    private Stream<String> gitCmd(Logger logger, String rootDir, String... args) {
        List<String> l = new ArrayList<>();
        l.add("git");
        if (!isCwd(rootDir)) {
            l.add("-C");
            l.add(rootDir);
        }

        Collections.addAll(l, args);
        String cmd = String.join(" ", l);
        logger.info("{}", cmd);

        try {
            return ShellUtil.runAndGetOutput(l);
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("err running: " + cmd, ex);
        }
    }

    private static final Pattern uncommittedPattern =
            Pattern.compile(".*Changes (not staged for commit|to be committed).*", Pattern.DOTALL);

    private boolean hasUnCommitted(Logger logger, String dir) {
        return gitCmd(logger, dir, "status")
                .anyMatch(line -> uncommittedPattern.matcher(line).matches());
    }

    private static final Pattern revisionPattern = Pattern.compile("[a-z0-9]{7,}");

    private String gitRevision(Logger logger, String dir) {
        Optional<String> firstLine = gitCmd(logger, dir, "rev-parse", "--short", "HEAD").findFirst();

        if (!firstLine.isPresent()) {
            throw new RuntimeException("failed to get current revision");
        }
        String line = firstLine.get().trim();
        if (revisionPattern.matcher(line).matches())
            return line;
        throw new RuntimeException("failed to get current revision: " + line);
    }

    private String gitRemote(Logger logger, String dir) {
        final Map<String, String> remotes = new HashMap<>();
        gitCmd(logger, dir, "remote", "-v")
                .filter(line -> line.startsWith(ext.useRemote))
                .forEach(line -> {
                    String[] parts = line.split("\\s+");
                    remotes.put(parts[2], parts[1]);
                });

        if (remotes.containsKey("(fetch)"))
            return remotes.get("(fetch)");

        return remotes.values().stream().findFirst().orElse("");
    }

    private String gitBranch(Logger logger, String dir) {
        Optional<String> line = gitCmd(logger, dir, "branch", "--show-current").findFirst();
        if (line.isPresent())
            return line.get().trim();
        throw new RuntimeException("failed to get current branch");
    }

    private static class GitInfo {
        private String revision;
        private String remoteURL;
        private String branch;
        private final long checkedAt = System.currentTimeMillis();

        public String getRevision() {
            return revision;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }

        public String getRemoteURL() {
            return remoteURL;
        }

        public void setRemoteURL(String remoteURL) {
            this.remoteURL = remoteURL;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public long getCheckedAt() {
            return checkedAt;
        }

        public boolean expired() {
            long elapsed = System.currentTimeMillis() - checkedAt;
            return elapsed < 0 || elapsed > 5_000;
        }
    }
}
