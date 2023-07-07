package uk.gov.hmcts.reform.pip.publication.services.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = SubscriptionsConstraintValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SubscriptionsConstraint {
    String message() default "Invalid subscriptions map provided. Must contain at least one subscription criteria";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
