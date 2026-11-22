package br.edu.ufersa.pw.focustask.features.project;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProjectTagsValidator.class)
public @interface ValidProjectTags {
    String message() default "Use até 10 tags diferentes, com 1 a 30 caracteres cada.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
