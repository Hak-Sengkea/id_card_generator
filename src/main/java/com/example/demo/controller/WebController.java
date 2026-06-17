package com.example.demo.controller;

import com.example.demo.model.Profile;
import com.example.demo.model.ProfileType;
import com.example.demo.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final ProfileService profileService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/profiles/create")
    public String create(
            @RequestParam String fullName,
            @RequestParam String department,
            @RequestParam String email,
            @RequestParam ProfileType type,
            Model model) {

        Profile profile = new Profile();

        profile.setFullName(fullName);
        profile.setDepartment(department);
        profile.setEmail(email);
        profile.setType(type);

        Profile saved =
                profileService.create(profile);

        return "redirect:/preview/" + saved.getId();
    }
}