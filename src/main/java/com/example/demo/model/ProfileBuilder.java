package com.example.demo.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A helper/builder class used to construct Profile objects, especially for
 * building default profiles.
 */
public class ProfileBuilder {

    private final Profile profile;

    public ProfileBuilder() {
        this.profile = new Profile();
        // Set sensible default values
        this.profile.setFullName("John Doe");
        this.profile.setType(ProfileType.STUDENT);
        this.profile.setDepartment("Computer Science");
        this.profile.setTitle("Undergraduate Student");
        this.profile.setEmail("john.doe@example.com");
        this.profile.setPhone("+1234567890");
        this.profile.setBloodGroup("O+");
        this.profile.setDateOfBirth(LocalDate.of(2004, 9, 1));
        this.profile.setIssueDate(LocalDate.now());
        this.profile.setExpiryDate(LocalDate.now().plusYears(4));
        this.profile.setBarcodeType(BarcodeType.CODE_128);
    }

    /**
     * Creates a profile with default values.
     *
     * @return a default Profile instance
     */
    public static Profile buildDefault() {
        return new ProfileBuilder().build();
    }

    public ProfileBuilder withName(String fullName) {
        this.profile.setFullName(fullName);
        return this;
    }

    public ProfileBuilder withType(ProfileType type) {
        this.profile.setType(type);
        if (type == ProfileType.EMPLOYEE) {
            this.profile.setTitle("Staff Member");
            this.profile.setExpiryDate(LocalDate.now().plusYears(5));
        } else if (type == ProfileType.USER) {
            this.profile.setTitle("General User");
            this.profile.setExpiryDate(LocalDate.now().plusYears(1));
        } else {
            this.profile.setTitle("Undergraduate Student");
            this.profile.setExpiryDate(LocalDate.now().plusYears(4));
        }
        return this;
    }

    public ProfileBuilder withDepartment(String department) {
        this.profile.setDepartment(department);
        return this;
    }

    public ProfileBuilder withTitle(String title) {
        this.profile.setTitle(title);
        return this;
    }

    public ProfileBuilder withEmail(String email) {
        this.profile.setEmail(email);
        return this;
    }

    public ProfileBuilder withPhone(String phone) {
        this.profile.setPhone(phone);
        return this;
    }

    public ProfileBuilder withBloodGroup(String bloodGroup) {
        this.profile.setBloodGroup(bloodGroup);
        return this;
    }

    public ProfileBuilder withDateOfBirth(LocalDate dob) {
        this.profile.setDateOfBirth(dob);
        return this;
    }

    public ProfileBuilder withTemplate(Template template) {
        this.profile.setTemplate(template);
        return this;
    }

    public ProfileBuilder withBarcodeType(BarcodeType barcodeType) {
        this.profile.setBarcodeType(barcodeType);
        return this;
    }

    public Profile build() {
        if (this.profile.getUuid() == null) {
            this.profile.setUuid(UUID.randomUUID().toString());
        }
        return this.profile;
    }
}
