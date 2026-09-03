package com.heilous.land.repository;

import com.heilous.land.dto.LandFilterRequest;
import com.heilous.land.entity.Land;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class LandSpecification {

    public static Specification<Land> buildFilter(LandFilterRequest filter) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 상태 필터
            if (filter.getStatus() != null) {
                predicates.add(
                        cb.equal(root.get("status"), filter.getStatus())
                );
            }

            // 면적 범위
            if (filter.getMinArea() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("area"),
                                filter.getMinArea()
                        )
                );
            }

            if (filter.getMaxArea() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("area"),
                                filter.getMaxArea()
                        )
                );
            }

            // 지역 계층 필터
            if (filter.getSido() != null && !filter.getSido().isBlank()) {

                predicates.add(
                        cb.equal(root.get("regionSido"), filter.getSido())
                );

                if (filter.getSigungu() != null &&
                        !filter.getSigungu().isBlank()) {

                    predicates.add(
                            cb.equal(
                                    root.get("regionSigungu"),
                                    filter.getSigungu()
                            )
                    );

                    if (filter.getEupmyeondong() != null &&
                            !filter.getEupmyeondong().isBlank()) {

                        predicates.add(
                                cb.equal(
                                        root.get("regionEupmyeondong"),
                                        filter.getEupmyeondong()
                                )
                        );
                    }
                }
            }

            // 가격 필터
            Predicate pricePredicate =
                    buildPricePredicate(filter, cb, root);

            if (pricePredicate != null) {
                predicates.add(pricePredicate);
            }

            // =========================
            // 지도 영역(BBOX) 필터
            // EPSG:4326
            //
            // X = 경도
            // Y = 위도
            // =========================
            if (filter.getTopLeftX() != null &&
                    filter.getTopLeftY() != null &&
                    filter.getBottomRightX() != null &&
                    filter.getBottomRightY() != null) {

                // X: 왼쪽 → 오른쪽
                predicates.add(
                        cb.between(
                                root.get("x"),
                                filter.getTopLeftX(),
                                filter.getBottomRightX()
                        )
                );

                // Y: 아래 → 위
                predicates.add(
                        cb.between(
                                root.get("y"),
                                filter.getBottomRightY(),
                                filter.getTopLeftY()
                        )
                );
            }

            // N+1 방지
            if (query.getResultType().equals(Land.class)) {
                root.fetch("owner");
            }

            // 최신 등록순
            query.orderBy(
                    cb.desc(root.get("id"))
            );

            return cb.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }

    /**
     * 가격 조건 처리
     */
    private static Predicate buildPricePredicate(
            LandFilterRequest filter,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Root<Land> root
    ) {

        boolean hasSalePrice =
                filter.getSaleMinPrice() != null ||
                        filter.getSaleMaxPrice() != null;

        boolean hasLeasePrice =
                filter.getLeaseMinPrice() != null ||
                        filter.getLeaseMaxPrice() != null;

        // transactionType 지정
        if (filter.getTransactionType() != null) {

            Predicate typePredicate =
                    cb.equal(
                            root.get("transactionType"),
                            filter.getTransactionType()
                    );

            // BUSINESS는 가격 조건 없음
            if (filter.getTransactionType() == Land.TransactionType.BUSINESS) {
                return typePredicate;
            }

            Predicate rangePredicate;

            if (filter.getTransactionType() ==
                    Land.TransactionType.SALE) {

                rangePredicate = buildRangePredicate(
                        cb,
                        root,
                        filter.getSaleMinPrice(),
                        filter.getSaleMaxPrice()
                );

            } else {

                rangePredicate = buildRangePredicate(
                        cb,
                        root,
                        filter.getLeaseMinPrice(),
                        filter.getLeaseMaxPrice()
                );
            }

            return rangePredicate != null
                    ? cb.and(typePredicate, rangePredicate)
                    : typePredicate;
        }

        // SALE + LEASE 가격 조건 둘 다 있음
        if (hasSalePrice && hasLeasePrice) {

            Predicate saleBlock =
                    buildTypeWithRange(
                            cb,
                            root,
                            Land.TransactionType.SALE,
                            filter.getSaleMinPrice(),
                            filter.getSaleMaxPrice()
                    );

            Predicate leaseBlock =
                    buildTypeWithRange(
                            cb,
                            root,
                            Land.TransactionType.LEASE,
                            filter.getLeaseMinPrice(),
                            filter.getLeaseMaxPrice()
                    );

            return cb.or(
                    saleBlock,
                    leaseBlock
            );
        }

        // SALE 가격 조건만 있음 → SALE은 범위 적용, LEASE/BUSINESS는 필터 없이 전부
        if (hasSalePrice) {

            Predicate saleBlock =
                    buildTypeWithRange(
                            cb,
                            root,
                            Land.TransactionType.SALE,
                            filter.getSaleMinPrice(),
                            filter.getSaleMaxPrice()
                    );

            Predicate leaseAll = cb.equal(root.get("transactionType"), Land.TransactionType.LEASE);
            Predicate businessAll = cb.equal(root.get("transactionType"), Land.TransactionType.BUSINESS);

            return cb.or(saleBlock, leaseAll, businessAll);
        }

        // LEASE 가격 조건만 있음 → LEASE는 범위 적용, SALE/BUSINESS는 필터 없이 전부
        if (hasLeasePrice) {

            Predicate leaseBlock =
                    buildTypeWithRange(
                            cb,
                            root,
                            Land.TransactionType.LEASE,
                            filter.getLeaseMinPrice(),
                            filter.getLeaseMaxPrice()
                    );

            Predicate saleAll = cb.equal(root.get("transactionType"), Land.TransactionType.SALE);
            Predicate businessAll = cb.equal(root.get("transactionType"), Land.TransactionType.BUSINESS);

            return cb.or(leaseBlock, saleAll, businessAll);
        }

        return null;
    }

    /**
     * 거래 유형 + 가격 범위
     */
    private static Predicate buildTypeWithRange(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Root<Land> root,
            Land.TransactionType type,
            Long min,
            Long max
    ) {

        Predicate typePredicate =
                cb.equal(
                        root.get("transactionType"),
                        type
                );

        Predicate rangePredicate =
                buildRangePredicate(
                        cb,
                        root,
                        min,
                        max
                );

        return rangePredicate != null
                ? cb.and(typePredicate, rangePredicate)
                : typePredicate;
    }

    /**
     * 가격 범위
     */
    private static Predicate buildRangePredicate(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Root<Land> root,
            Long min,
            Long max
    ) {

        if (min == null && max == null) {
            return null;
        }

        // 최소 + 최대
        if (min != null && max != null) {
            return cb.between(
                    root.get("desiredPrice"),
                    min,
                    max
            );
        }

        // 최소만
        if (min != null) {
            return cb.greaterThanOrEqualTo(
                    root.get("desiredPrice"),
                    min
            );
        }

        // 최대만
        return cb.lessThanOrEqualTo(
                root.get("desiredPrice"),
                max
        );
    }
}