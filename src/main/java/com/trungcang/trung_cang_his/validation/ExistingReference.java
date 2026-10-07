package com.trungcang.trung_cang_his.validation;

import jakarta.persistence.EntityManager;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExistingReference.Validator.class)
public @interface ExistingReference {
    String message() default "phải tham chiếu bản ghi đang tồn tại bằng ID hợp lệ";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ExistingReference, Object> {
        private final EntityManager entities;
        public Validator(EntityManager entities) { this.entities = entities; }
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) return true;
            Object id = entities.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(value);
            return id instanceof Long number && number > 0 && entities.find(value.getClass(), id) != null;
        }
    }
}
