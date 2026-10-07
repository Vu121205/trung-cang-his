package com.trungcang.trung_cang_his.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.*;
import java.time.LocalDate;
import java.time.ZoneId;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BirthDate.Validator.class)
public @interface BirthDate {
    String message() default "phải từ năm 1900 đến ngày hiện tại";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<BirthDate, LocalDate> {
        public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
            return value == null || (!value.isBefore(LocalDate.of(1900, 1, 1))
                    && !value.isAfter(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))));
        }
    }
}
