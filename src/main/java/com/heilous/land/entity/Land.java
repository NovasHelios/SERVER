package com.heilous.land.entity;

import com.heilous.apply.entity.LandApply;
import com.heilous.chat.entity.ChatRoom;
import com.heilous.common.entity.BaseEntity;
import com.heilous.user.entity.User;
import com.heilous.wish.entity.Wish;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lands", indexes = {
        @Index(name = "idx_lands_status_id", columnList = "status, id"),
        @Index(name = "idx_lands_owner_id", columnList = "owner_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Land extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String address;

    private Double area;
    private String lcCode;
    private String lcCodeNm;
    private String lastUpdtDt;
    private String regstrSeCodeNm;
    private String cnrsPsnCo;
    private String pnu;
    private String ldCodeNm;
    private String regionSido;
    private String regionSigungu;
    private String regionEupmyeondong;
    private Long desiredPrice;
    private Double desiredArea;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private LandStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    private String documentPath;

    @Column(nullable = false)
    private Double x;

    @Column(nullable = false)
    private Double y;

    // 용도지역
    private String prposAreaCode;
    private String prposAreaName;
    @Column(length = 1)
    private String prposAreaCnflcAt;
    private String prposAreaCnflcAtNm;

    // 도로접면코드
    private String roadSideCode;

    @OneToMany(mappedBy = "land", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LandZone> landZones = new ArrayList<>();

    @OneToMany(mappedBy = "land", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LandEtc> landEtcs = new ArrayList<>();

    @OneToMany(mappedBy = "land", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LandPrice> landPrices = new ArrayList<>();

    @OneToMany(mappedBy = "land", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<LandImage> landImages = new ArrayList<>();

    @OneToMany(mappedBy = "land", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Wish> wishes = new ArrayList<>();

    @OneToMany(mappedBy = "land", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<LandApply> landApplies = new ArrayList<>();

    @OneToMany(mappedBy = "land", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ChatRoom> chatRooms = new ArrayList<>();

    public enum LandStatus { PENDING, APPROVED, REJECTED }
    public enum TransactionType { SALE, LEASE, BUSINESS_HOPE }

    public void updateLand(String address, Double area, String lcCode, String lcCodeNm,
                           String lastUpdtDt, String regstrSeCodeNm, String cnrsPsnCo, String pnu,
                           String ldCodeNm, String regionSido, String regionSigungu, String regionEupmyeondong,
                           Long desiredPrice, Double desiredArea, String description, TransactionType transactionType,
                           Double x, Double y) {
        this.address = address; this.area = area; this.lcCode = lcCode; this.lcCodeNm = lcCodeNm;
        this.lastUpdtDt = lastUpdtDt; this.regstrSeCodeNm = regstrSeCodeNm; this.cnrsPsnCo = cnrsPsnCo;
        this.pnu = pnu; this.ldCodeNm = ldCodeNm; this.regionSido = regionSido;
        this.regionSigungu = regionSigungu; this.regionEupmyeondong = regionEupmyeondong;
        this.desiredPrice = desiredPrice; this.desiredArea = desiredArea; this.description = description;
        this.x = x; this.y = y;
        if (transactionType != null) this.transactionType = transactionType;
    }

    public void updateLandUse(String prposAreaCode, String prposAreaName,
                              String prposAreaCnflcAt, String prposAreaCnflcAtNm) {
        this.prposAreaCode = prposAreaCode; this.prposAreaName = prposAreaName;
        this.prposAreaCnflcAt = prposAreaCnflcAt; this.prposAreaCnflcAtNm = prposAreaCnflcAtNm;
    }

    public void updateRoadSideCode(String roadSideCode) { this.roadSideCode = roadSideCode; }
    public void changeStatus(LandStatus status) { this.status = status; }
    public void addImage(LandImage image) { this.landImages.add(image); }
    public void removeImage(LandImage image) { this.landImages.remove(image); }
    public void updateDocumentPath(String documentPath) { this.documentPath = documentPath; }
}
