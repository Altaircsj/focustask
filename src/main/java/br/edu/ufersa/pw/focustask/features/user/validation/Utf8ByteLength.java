package br.edu.ufersa.pw.focustask.features.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = Utf8ByteLengthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Utf8ByteLength {
    String message() default "must contain at most {max} UTF-8 bytes";
    int max();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
