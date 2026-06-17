package com.example.demo.controller;

import com.example.demo.model.Profile;
import com.example.demo.model.Template;
import com.example.demo.repository.ProfileRepository;
import com.example.demo.repository.TemplateRepository;
import com.example.demo.service.ProfileService;
import com.example.demo.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final ProfileService profileService;
    private final ProfileRepository profileRepository;
    private final TemplateRepository templateRepository;
    private final StorageService storageService;

    @GetMapping("/")
    public String home() {
        return "redirect:/profiles";
    }

    @GetMapping("/profiles")
    public String listProfiles(Model model) {
        List<Profile> profiles = profileService.findAll();
        model.addAttribute("profiles", profiles);
        return "profiles";
    }

    @GetMapping("/profiles/new")
    public String newProfileForm(Model model) {
        Profile profile = new Profile();
        // pre-populated default date values
        profile.setIssueDate(java.time.LocalDate.now());
        profile.setExpiryDate(java.time.LocalDate.now().plusYears(2));
        model.addAttribute("profile", profile);
        model.addAttribute("templates", templateRepository.findAll());
        model.addAttribute("isEdit", false);
        return "profile-form";
    }

    @GetMapping("/profiles/{id}/edit")
    public String editProfileForm(@PathVariable Long id, Model model) {
        Profile profile = profileService.findById(id);
        model.addAttribute("profile", profile);
        model.addAttribute("templates", templateRepository.findAll());
        model.addAttribute("isEdit", true);
        return "profile-form";
    }

    @GetMapping("/profiles/{id}/card")
    public String viewProfileCard(@PathVariable Long id, Model model) {
        Profile profile = profileService.findById(id);
        model.addAttribute("profile", profile);
        return "card";
    }

    @GetMapping("/batch")
    public String batchExportPage(Model model) {
        List<Profile> profiles = profileService.findAll();
        model.addAttribute("profiles", profiles);
        return "batch";
    }

    @GetMapping("/verify/{uuid}")
    public String verifyProfile(@PathVariable String uuid, Model model) {
        Profile profile = profileRepository.findByUuid(uuid).orElse(null);
        model.addAttribute("profile", profile);
        model.addAttribute("uuid", uuid);
        return "verify";
    }

    @PostMapping("/profiles")
    public String createProfile(
            @ModelAttribute Profile profile,
            @RequestParam("templateCode") String templateCode,
            @RequestParam("photo") MultipartFile photo) throws IOException {

        Template template = templateRepository.findByCode(templateCode).orElse(null);
        profile.setTemplate(template);

        if (photo != null && !photo.isEmpty()) {
            String filename = storageService.store(photo);
            profile.setPhotoFileName(filename);
            profile.setPhotoContentType(photo.getContentType());
        }

        Profile saved = profileService.create(profile);
        return "redirect:/profiles/" + saved.getId() + "/card";
    }

    @PostMapping("/profiles/{id}")
    public String updateProfile(
            @PathVariable Long id,
            @ModelAttribute Profile profileUpdates,
            @RequestParam("templateCode") String templateCode,
            @RequestParam("photo") MultipartFile photo) throws IOException {

        Template template = templateRepository.findByCode(templateCode).orElse(null);
        profileUpdates.setTemplate(template);

        Profile existing = profileService.findById(id);

        if (photo != null && !photo.isEmpty()) {
            String filename = storageService.store(photo);
            existing.setPhotoFileName(filename);
            existing.setPhotoContentType(photo.getContentType());
        }

        // Update properties
        existing.setFullName(profileUpdates.getFullName());
        existing.setType(profileUpdates.getType());
        existing.setDepartment(profileUpdates.getDepartment());
        existing.setTitle(profileUpdates.getTitle());
        existing.setEmail(profileUpdates.getEmail());
        existing.setPhone(profileUpdates.getPhone());
        existing.setBloodGroup(profileUpdates.getBloodGroup());
        existing.setDateOfBirth(profileUpdates.getDateOfBirth());
        existing.setIssueDate(profileUpdates.getIssueDate());
        existing.setExpiryDate(profileUpdates.getExpiryDate());
        existing.setBarcodeType(profileUpdates.getBarcodeType());
        existing.setTemplate(profileUpdates.getTemplate());

        profileService.save(existing);
        return "redirect:/profiles/" + existing.getId() + "/card";
    }

    @PostMapping("/profiles/{id}/delete")
    public String deleteProfile(@PathVariable Long id) {
        profileService.delete(id);
        return "redirect:/profiles";
    }
}