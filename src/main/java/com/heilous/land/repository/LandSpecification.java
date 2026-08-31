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

            // 상태 필터 (nullable → null이면 전체)
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            // 면적 범위
            if (filter.getMinArea() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), filter.getMinArea()));
            }
            if (filter.getMaxArea() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), filter.getMaxArea()));
            }

            // 지역 계층 필터
            if (filter.getSido() != null && !filter.getSido().isBlank()) {
                predicates.add(cb.equal(root.get("regionSido"), filter.getSido()));
                if (filter.getSigungu() != null && !filter.getSigungu().isBlank()) {
                    predicates.add(cb.equal(root.get("regionSigungu"), filter.getSigungu()));
                    if (filter.getEupmyeondong() != null && !filter.getEupmyeondong().isBlank()) {
                        predicates.add(cb.equal(root.get("regionEupmyeondong"), filter.getEupmyeondong()));
                    }
                }
            }

            // 가격 필터
            Predicate pricePredicate = buildPricePredicate(filter, cb, root);
            if (pricePredicate != null) {
                predicates.add(pricePredicate);
            }

            // N+1 방지: owner fetch join
            if (query.getResultType().equals(Land.class)) {
                root.fetch("owner");
            }

            query.orderBy(cb.desc(root.get("id")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 가격 조건 처리
     * - transactionType 명시: 해당 타입 가격 범위만 적용
     * - transactionType null + sale 조건만: (SALE AND salePriceCondition)
     * - transactionType null + lease 조건만: (LEASE AND leasePriceCondition)
     * - transactionType null + 양쪽 조건: (SALE AND salePriceCondition) OR (LEASE AND leasePriceCondition)
     * - 가격 조건 없음: null 반환 (조건 미적용)
     */
    private static Predicate buildPricePredicate(
            LandFilterRequest filter,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Root<Land> root
    ) {
        boolean hasSalePrice = filter.getSaleMinPrice() != null || filter.getSaleMaxPrice() != null;
        boolean hasLeasePrice = filter.getLeaseMinPrice() != null || filter.getLeaseMaxPrice() != null;

        if (filter.getTransactionType() != null) {
            // transactionType 명시된 경우
            Predicate typePredicate = cb.equal(root.get("transactionType"), filter.getTransactionType());
            Predicate rangePredicate = filter.getTransactionType() == Land.TransactionType.SALE
                    ? buildRangePredicate(cb, root, filter.getSaleMinPrice(), filter.getSaleMaxPrice())
                    : buildRangePredicate(cb, root, filter.getLeaseMinPrice(), filter.getLeaseMaxPrice());

            return rangePredicate != null ? cb.and(typePredicate, rangePredicate) : typePredicate;
        }

        // transactionType null
        if (hasSalePrice && hasLeasePrice) {
            // (SALE AND saleRange) OR (LEASE AND leaseRange)
            Predicate saleBlock = buildTypeWithRange(cb, root, Land.TransactionType.SALE,
                    filter.getSaleMinPrice(), filter.getSaleMaxPrice());
            Predicate leaseBlock = buildTypeWithRange(cb, root, Land.TransactionType.LEASE,
                    filter.getLeaseMinPrice(), filter.getLeaseMaxPrice());
            return cb.or(saleBlock, leaseBlock);
        }

        if (hasSalePrice) {
            // SALE은 가격 필터 적용, LEASE는 필터 없이 전부
            Predicate saleBlock = buildTypeWithRange(cb, root, Land.TransactionType.SALE,
                    filter.getSaleMinPrice(), filter.getSaleMaxPrice());
            Predicate leaseAll = cb.equal(root.get("transactionType"), Land.TransactionType.LEASE);
            return cb.or(saleBlock, leaseAll);
        }

        if (hasLeasePrice) {
            // LEASE는 가격 필터 적용, SALE은 필터 없이 전부
            Predicate leaseBlock = buildTypeWithRange(cb, root, Land.TransactionType.LEASE,
                    filter.getLeaseMinPrice(), filter.getLeaseMaxPrice());
            Predicate saleAll = cb.equal(root.get("transactionType"), Land.TransactionType.SALE);
            return cb.or(leaseBlock, saleAll);
        }

        return null; // 가격 조건 없음
    }

    private static Predicate buildTypeWithRange(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Root<Land> root,
            Land.TransactionType type, Long min, Long max
    ) {
        Predicate typePredicate = cb.equal(root.get("transactionType"), type);
        Predicate rangePredicate = buildRangePredicate(cb, root, min, max);
        return rangePredicate != null ? cb.and(typePredicate, rangePredicate) : typePredicate;
    }

    private static Predicate buildRangePredicate(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Root<Land> root,
            Long min, Long max
    ) {
        if (min == null && max == null) return null;
        if (min != null && max != null) {
            return cb.between(root.get("desiredPrice"), min, max);
        }
        if (min != null) {
            return cb.greaterThanOrEqualTo(root.get("desiredPrice"), min);
        }

        return cb.lessThanOrEqualTo(root.get("desiredPrice"), max);
    }
}
