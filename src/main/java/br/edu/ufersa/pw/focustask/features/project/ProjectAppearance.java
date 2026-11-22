package br.edu.ufersa.pw.focustask.features.project;

import java.util.regex.Pattern;

public final class ProjectAppearance {
    public static final String DEFAULT_COLOR = "#4D49FC";
    public static final String COLOR_PATTERN = "(?i)(#4D49FC|#F59E0B|#10B981|#EF4444|#A855F7|#06B6D4|#E2E8F0)";
    private static final Pattern EDGE_SPACES = Pattern.compile("(?U)^\\s+|\\s+$");

    private ProjectAppearance() {}

    static String trimTag(String tag) {
        return EDGE_SPACES.matcher(tag).replaceAll("");
    }
}
