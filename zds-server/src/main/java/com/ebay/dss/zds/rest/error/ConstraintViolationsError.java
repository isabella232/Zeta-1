package com.ebay.dss.zds.rest.error;

import javax.validation.ConstraintViolation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConstraintViolationsError extends BaseErrorDetail {

    private Set<ConstraintViolation<?>> innerViolations;
    private Set<Map<String, String>> violations;

    public ConstraintViolationsError() {
    }

    public ConstraintViolationsError(Set<ConstraintViolation<?>> violations) {
        this.innerViolations = violations;
        this.violations = new HashSet<>();
        for (ConstraintViolation<?> vio : this.innerViolations) {
            Map<String, String> details = new HashMap<>();
            details.put("propertyPath", vio.getPropertyPath().toString());
            details.put("invalidValue", vio.getInvalidValue().toString());
            details.put("message", vio.getMessage());
            this.violations.add(details);
        }
    }

    public static ConstraintViolationsError from(Set<ConstraintViolation<?>> violations) {
        return new ConstraintViolationsError(violations);
    }

    public Set<Map<String, String>> getViolations() {
        return violations;
    }

    public ConstraintViolationsError setViolations(Set<Map<String, String>> violations) {
        this.violations = violations;
        return this;
    }
}
