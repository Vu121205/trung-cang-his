package com.trungcang.trung_cang_his.service;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(String username, String password);
}
