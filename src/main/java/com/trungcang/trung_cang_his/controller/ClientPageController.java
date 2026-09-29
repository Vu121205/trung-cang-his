package com.trungcang.trung_cang_his.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class ClientPageController {
    @ModelAttribute
    void navigation(Authentication authentication, Model model) {
        boolean admin = hasRole(authentication, "ADMIN");
        model.addAttribute("canManageStaff", admin);
        model.addAttribute("canReceive", admin || hasRole(authentication, "RECEPTION"));
        model.addAttribute("canExamine", admin || hasRole(authentication, "DOCTOR"));
        model.addAttribute("canDispense", admin || hasRole(authentication, "PHARMACIST"));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "client/login";
    }


    @GetMapping("/rooms")
    public String roomsPage() {
        return "client/rooms";
    }

    @GetMapping("/employees")
    public String employeesPage() {
        return "client/employees";
    }


    @GetMapping("/reception")
    public String receptionPage() {
        return "client/reception";
    }


    @GetMapping({"/examination", "/medical-examination"})
    public String examinationPage() {
        return "client/medical-examination";
    }


    @GetMapping("/pharmacy")
    public String pharmacyPage() {
        return "client/pharmacy";
    }


    @GetMapping("/payment")
    public String paymentPage() {
        return "client/payment";
    }

    @GetMapping("/examination-history")
    public String historyPage() {
        return "client/examination-history";
    }
}
