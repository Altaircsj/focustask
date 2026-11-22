package br.edu.ufersa.pw.focustask.features.project;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class ProjectTagsValidator implements ConstraintValidator<ValidProjectTags, List<String>> {
    @Override
    public boolean isValid(List<String> tags, ConstraintValidatorContext context) {
        if (tags == null) return true;
        if (tags.size() > 10) return false;
        var seen = new HashSet<String>();
        for (String raw : tags) {
            if (raw == null) return false;
            String tag = ProjectAppearance.trimTag(raw);
            if (tag.isEmpty() || tag.codePointCount(0, tag.length()) > 30 || !seen.add(tag.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }
}
