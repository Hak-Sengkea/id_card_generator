package com.example.demo.service;

import com.example.demo.model.Profile;
import com.example.demo.model.ProfileType;
import com.example.demo.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    public List<Profile> findAll() {
        return profileRepository.findAll();
    }

    public Profile findById(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
    }

    public Profile create(Profile profile) {

        if (profile.getUuid() == null || profile.getUuid().isBlank()) {
            profile.setUuid(UUID.randomUUID().toString());
        }

        if (profile.getRegistrationNumber() == null ||
                profile.getRegistrationNumber().isBlank()) {

            long count = profileRepository.countByType(profile.getType()) + 1;
            String prefix = getDeptPrefix(profile);

            profile.setRegistrationNumber(
                    Year.now().getValue()
                            + "-"
                            + prefix
                            + "-"
                            + String.format("%03d", count));
        }

        return profileRepository.save(profile);
    }

    public Profile update(Long id, Profile updated) {

        Profile profile = findById(id);

        profile.setFullName(updated.getFullName());
        profile.setType(updated.getType());
        profile.setDepartment(updated.getDepartment());
        profile.setTitle(updated.getTitle());
        profile.setEmail(updated.getEmail());
        profile.setPhone(updated.getPhone());
        profile.setBloodGroup(updated.getBloodGroup());
        profile.setDateOfBirth(updated.getDateOfBirth());
        profile.setExpiryDate(updated.getExpiryDate());
        profile.setBarcodeType(updated.getBarcodeType());
        profile.setTemplate(updated.getTemplate());

        return profileRepository.save(profile);
    }

    public void delete(Long id) {
        profileRepository.deleteById(id);
    }

    public Profile save(Profile profile) {
        return profileRepository.save(profile);
    }

    private String getDeptPrefix(Profile profile) {
        if (profile.getDepartment() != null && !profile.getDepartment().isBlank()) {
            String dept = profile.getDepartment().trim().replaceAll("[^a-zA-Z]", "").toUpperCase();
            if (dept.length() >= 3) {
                return dept.substring(0, 3);
            } else if (!dept.isEmpty()) {
                return dept;
            }
        }
        return profile.getType() == ProfileType.STUDENT ? "STU" : "EMP";
    }
}