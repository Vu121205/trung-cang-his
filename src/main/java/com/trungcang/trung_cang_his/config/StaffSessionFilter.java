package com.trungcang.trung_cang_his.config;

import com.trungcang.trung_cang_his.domain.User;
import com.trungcang.trung_cang_his.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Check the account revision on each authenticated request to revoke stale sessions. */
public class StaffSessionFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public StaffSessionFilter(UserRepository users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof StaffPrincipal principal) {
            boolean valid = users.findById(principal.staffId())
                    .filter(u -> !u.isDeleted() && u.getStatus() == User.Status.ACTIVE
                            && u.getSecurityVersion() == principal.securityVersion()).isPresent();
            if (!valid) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    response.sendRedirect(request.getContextPath() + "/login");
                }
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
