package com.trungcang.trung_cang_his.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumber.Validator.class)
public @interface PhoneNumber {
    String message() default "cần 8–15 chữ số, có thể có dấu + ở đầu, khoảng trắng, dấu chấm, gạch ngang hoặc ngoặc";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<PhoneNumber, String> {
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isEmpty()) return true;
            String digits = value.replaceAll("[^0-9]", "");
            return value.length() <= 20 && value.matches("\\+?[0-9() .-]+")
                    && digits.length() >= 8 && digits.length() <= 15;
        }
    }
}
