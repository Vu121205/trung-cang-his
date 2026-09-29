package com.trungcang.trung_cang_his.config;

import com.trungcang.trung_cang_his.domain.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

public class StaffPrincipal extends org.springframework.security.core.userdetails.User {
    private final Long staffId;
    private final long securityVersion;

    public StaffPrincipal(User user) {
        super(user.getUsername(), user.getPassword(),
                !user.isDeleted() && user.getStatus() == User.Status.ACTIVE, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getCode().replaceFirst("^ROLE_", ""))));
        staffId = user.getId();
        securityVersion = user.getSecurityVersion();
    }

    public Long staffId() { return staffId; }
    public long securityVersion() { return securityVersion; }
}
