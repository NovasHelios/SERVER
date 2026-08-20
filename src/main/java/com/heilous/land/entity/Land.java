package com.heilous.land.entity;

import com.heilous.common.entity.BaseEntity;
import com.heilous.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

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

    private Long desiredPrice;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private LandStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    private String landImagePath; // 토지 이미지 파일명

    private String documentPath;  // 증명서 파일명

    public enum LandStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum TransactionType {
        SALE,
        LEASE
    }

    public void updateLand(String address, Double area, String lcCode, String lcCodeNm,
                           String lastUpdtDt, String regstrSeCodeNm, String cnrsPsnCo, String pnu,
                           Long desiredPrice, String description, TransactionType transactionType) {
        this.address = address;
        this.area = area;
        this.lcCode = lcCode;
        this.lcCodeNm = lcCodeNm;
        this.lastUpdtDt = lastUpdtDt;
        this.regstrSeCodeNm = regstrSeCodeNm;
        this.cnrsPsnCo = cnrsPsnCo;
        this.pnu = pnu;
        this.desiredPrice = desiredPrice;
        this.description = description;
        if (transactionType != null) {
            this.transactionType = transactionType;
        }
    }

    public void changeStatus(LandStatus status) {
        this.status = status;
    }

    public void updateImagePath(String landImagePath) {
        this.landImagePath = landImagePath;
    }

    public void updateDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

}
