package com.zoopzoop.zoopzoop.domain.policy.repository;

import com.zoopzoop.zoopzoop.domain.policy.dto.PolicySearchCriteria;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyConditions;
import com.zoopzoop.zoopzoop.domain.policy.entity.PolicyList;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class PolicyListRepositoryImpl implements PolicyListRepositoryCustom {

    private static final Set<String> SPECIAL_FIELDS = Set.of(
            "ja0201", "ja0202", "ja0203", "ja0204", "ja0205",
            "ja0301", "ja0302", "ja0303",
            "ja0313", "ja0314", "ja0315", "ja0316",
            "ja0317", "ja0318", "ja0319", "ja0320",
            "ja0326", "ja0327",
            "ja0401", "ja0402", "ja0403", "ja0404", "ja0411", "ja0412", "ja0413", "ja0414",
            "ja0328", "ja0329", "ja0330"
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<PolicyList> searchPolicies(PolicySearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<PolicyList> query = cb.createQuery(PolicyList.class);
        Root<PolicyList> root = query.from(PolicyList.class);
        List<Predicate> predicates = buildPredicates(criteria, cb, query, root);

        query.select(root)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(
                        cb.desc(root.get("viewCount")),
                        cb.asc(root.get("serviceName"))
                );

        TypedQuery<PolicyList> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<PolicyList> countRoot = countQuery.from(PolicyList.class);
        List<Predicate> countPredicates = buildPredicates(criteria, cb, countQuery, countRoot);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(Predicate[]::new));

        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(typedQuery.getResultList(), pageable, total);
    }

    @Override
    public List<String> findServiceTypes(PolicySearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<PolicyList> root = query.from(PolicyList.class);
        List<Predicate> predicates = buildPredicates(criteria, cb, query, root);

        query.select(root.get("serviceType"))
                .where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(query).getResultList();
    }

    private List<Predicate> buildPredicates(
            PolicySearchCriteria criteria,
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<PolicyList> root
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (hasText(criteria.query())) {
            String likeValue = "%" + criteria.query() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("serviceName")), likeValue),
                    cb.like(cb.lower(root.get("orgName")), likeValue)
            ));
        }

        if (hasText(criteria.type())) {
            predicates.add(cb.like(cb.lower(root.get("serviceType")), "%" + criteria.type() + "%"));
        }

        if (hasText(criteria.region())) {
            String likeValue = "%" + criteria.region() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("orgName")), likeValue),
                    cb.like(cb.lower(root.get("receivingOrg")), likeValue),
                    cb.like(cb.lower(root.get("departmentName")), likeValue)
            ));
        }

        if (criteria.age() != null || !criteria.specialCodes().isEmpty()) {
            Subquery<String> conditionsQuery = query.subquery(String.class);
            Root<PolicyConditions> conditionsRoot = conditionsQuery.from(PolicyConditions.class);
            List<Predicate> conditionPredicates = new ArrayList<>();

            conditionPredicates.add(cb.equal(conditionsRoot.get("serviceId"), root.get("serviceId")));

            if (criteria.age() != null) {
                conditionPredicates.add(cb.and(
                        cb.or(
                                cb.isNull(conditionsRoot.get("ja0110")),
                                cb.lessThanOrEqualTo(conditionsRoot.get("ja0110"), criteria.age())
                        ),
                        cb.or(
                                cb.isNull(conditionsRoot.get("ja0111")),
                                cb.greaterThanOrEqualTo(conditionsRoot.get("ja0111"), criteria.age())
                        )
                ));
            }

            if (!criteria.specialCodes().isEmpty()) {
                List<Predicate> specialPredicates = criteria.specialCodes().stream()
                        .filter(SPECIAL_FIELDS::contains)
                        .map(code -> isEnabled(cb, conditionsRoot.get(code)))
                        .toList();

                if (!specialPredicates.isEmpty()) {
                    conditionPredicates.add(cb.or(specialPredicates.toArray(Predicate[]::new)));
                }
            }

            conditionsQuery.select(conditionsRoot.get("serviceId"))
                    .where(conditionPredicates.toArray(Predicate[]::new));

            predicates.add(cb.exists(conditionsQuery));
        }

        return predicates;
    }

    private Predicate isEnabled(CriteriaBuilder cb, Path<String> path) {
        Expression<String> normalized = cb.upper(cb.trim(cb.coalesce(path, "")));
        return cb.and(
                cb.notEqual(normalized, ""),
                cb.notEqual(normalized, "N"),
                cb.notEqual(normalized, "0"),
                cb.notEqual(normalized, "FALSE")
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
