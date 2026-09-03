package com.heilous.land.entity;

import com.heilous.apply.entity.LandApply;
import com.heilous.chat.entity.ChatRoom;
import com.heilous.common.entity.BaseEntity;
import com.heilous.user.entity.User;
import com.heilous.wish.entity.Wish;import jakarta.persistence.*;
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

    private String lcCode;    // 지목코드

    private String lcCodeNm;  // 지목명

    private String lastUpdtDt;    // 데이터기준일자

    private String regstrSeCodeNm; // 대장구분명

    private String cnrsPsnCo;     // 소유(공유)인수(명)

    private String pnu;           // 고유번호

    private String ldCodeNm;      // 법정동명 (전체)

    private String regionSido;    // 시/도

    private String regionSigungu; // 시/군/구

    private String regionEupmyeondong; // 읍/면/동

    private Long desiredPrice;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private LandStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    private String documentPath;  // 증명서 파일명

    @Column(nullable = false)
    private Double x;

    @Column(nullable = false)
    private Double y;

    @OneToMany(mappedBy = "land", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<LandImage> landImages = new ArrayList<>();

    @OneToMany(mappedBy = "land", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Wish> wishes = new ArrayList<>();

    @OneToMany(mappedBy = "land", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<LandApply> landApplies = new ArrayList<>();

    @OneToMany(mappedBy = "land", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ChatRoom> chatRooms = new ArrayList<>();

    public enum LandStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum TransactionType {
        SALE,
        LEASE,
        BUSINESS
    }

    public void updateLand(String address, Double area, String lcCode, String lcCodeNm,
                           String lastUpdtDt, String regstrSeCodeNm, String cnrsPsnCo, String pnu,
                           String ldCodeNm, String regionSido, String regionSigungu, String regionEupmyeondong,
                           Long desiredPrice, String description, TransactionType transactionType,
                           Double x, Double y) {
        this.address = address;
        this.area = area;
        this.lcCode = lcCode;
        this.lcCodeNm = lcCodeNm;
        this.lastUpdtDt = lastUpdtDt;
        this.regstrSeCodeNm = regstrSeCodeNm;
        this.cnrsPsnCo = cnrsPsnCo;
        this.pnu = pnu;
        this.ldCodeNm = ldCodeNm;
        this.regionSido = regionSido;
        this.regionSigungu = regionSigungu;
        this.regionEupmyeondong = regionEupmyeondong;
        this.desiredPrice = desiredPrice;
        this.description = description;
        this.x = x;
        this.y = y;
        if (transactionType != null) {
            this.transactionType = transactionType;
        }
    }

    public void changeStatus(LandStatus status) {
        this.status = status;
    }

    public void addImage(LandImage image) {
        this.landImages.add(image);
    }

    public void removeImage(LandImage image) {
        this.landImages.remove(image);
    }

    public void updateDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

}
