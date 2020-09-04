package io.github.sokrato.gradle.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Stream;

public class ShellUtil {
    public static Stream<String> runAndGetOutput(List<String> cmd)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(cmd).start();
        process.waitFor();
        return Stream.concat(
                readAll(process.getInputStream()),
                readAll(process.getErrorStream())
        );

    }

    private static Stream<String> readAll(InputStream inputStream) {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        return br.lines();
    }
}
