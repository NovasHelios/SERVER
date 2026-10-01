package com.heilous.business.entity;

import com.heilous.common.entity.BaseEntity;
import com.heilous.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "business_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BusinessProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String companyName;

    @Column(unique = true)
    private String businessNumber;

    private String department;

    private String jobTitle;

    @ElementCollection
    @CollectionTable(name = "business_profile_fields", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "field")
    private List<String> mainFields;

    @Column(length = 1000)
    private String mainFieldDescription;

    @Column(length = 1000)
    private String businessDescription;

    @ElementCollection
    @CollectionTable(name = "business_profile_regions", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "region")
    private List<String> activeRegions;

    @Column(nullable = false)
    private boolean nationwideActive;

    @Column(nullable = false)
    private boolean consultationAvailable;

    private LocalTime consultationStartTime;
    private LocalTime consultationEndTime;

    private String businessLicensePath;
    private boolean businessVerified;

    public void update(String companyName, String businessNumber, String department,
                       String jobTitle, List<String> mainFields, String mainFieldDescription,
                       String businessDescription, List<String> activeRegions,
                       boolean nationwideActive, boolean consultationAvailable,
                       LocalTime consultationStartTime, LocalTime consultationEndTime) {
        this.companyName = companyName;
        this.businessNumber = businessNumber;
        this.department = department;
        this.jobTitle = jobTitle;
        this.mainFields = mainFields;
        this.mainFieldDescription = mainFieldDescription;
        this.businessDescription = businessDescription;
        this.activeRegions = activeRegions;
        this.nationwideActive = nationwideActive;
        this.consultationAvailable = consultationAvailable;
        this.consultationStartTime = consultationStartTime;
        this.consultationEndTime = consultationEndTime;
    }

    public void updateLicensePath(String path) {
        this.businessLicensePath = path;
    }

    public void verify() {
        this.businessVerified = true;
    }
}
