package com.ebay.dss.zds.model.ace;

import org.apache.commons.lang3.StringUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Documented
@Constraint(validatedBy = AceTagName.AceTagNameValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AceTagName {

    String message() default "Invalid tag name, only [a-z] [0-9] + - # . is allowed, and start with alphabet, length min 3 and max 35";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class AceTagNameValidator implements ConstraintValidator<AceTagName, String> {

        private static final Pattern START_ALPHA_CONTAIN_ALPHA_NUMERIC_DOT_SHAP_PLUS_DASH = Pattern.compile("^[a-z]+([a-z0-9+#\\-.]+)$");

        @Override
        public boolean isValid(String s, ConstraintValidatorContext context) {
            int len = StringUtils.length(s);
            boolean lenOk = len >= 3 && len <= 35;
            if (!lenOk) return false;

            Matcher matcher = START_ALPHA_CONTAIN_ALPHA_NUMERIC_DOT_SHAP_PLUS_DASH.matcher(s);
            return matcher.find();
        }
    }
}

