package com.trungcang.trung_cang_his.validation;

/** Request constraints are separate from JPA lifecycle validation and ID-only references. */
public interface ApiInput {
    interface Create extends ApiInput {}
    interface Update extends ApiInput {}
}
