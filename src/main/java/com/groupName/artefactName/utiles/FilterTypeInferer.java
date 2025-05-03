package com.groupName.artefactName.utiles;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class FilterTypeInferer {

    public Predicate inferPredicate(CriteriaBuilder criteriaBuilder, Root<?> root, String atributoNombre, Object valor, EntityType<?> entityType) {
        SingularAttribute<?, ?> atributo = entityType.getDeclaredSingularAttribute(atributoNombre);
        Class<?> atributoTipo = atributo.getJavaType();

        if (String.class.isAssignableFrom(atributoTipo) && valor instanceof String string) {
            return criteriaBuilder.like(root.get(atributoNombre), "%" + string + "%");
        } else {
            try {
                if (Long.class.isAssignableFrom(atributoTipo)) {
                    return criteriaBuilder.equal(root.get(atributoNombre), Long.parseLong((String) valor));
                } else if (Integer.class.isAssignableFrom(atributoTipo)) {
                    return criteriaBuilder.equal(root.get(atributoNombre), Integer.parseInt((String) valor));
                } else if (Double.class.isAssignableFrom(atributoTipo)) {
                    return criteriaBuilder.equal(root.get(atributoNombre), Double.parseDouble((String) valor));
                } else if (Float.class.isAssignableFrom(atributoTipo)) {
                    return criteriaBuilder.equal(root.get(atributoNombre), Float.parseFloat((String) valor));
                } else if (Boolean.class.isAssignableFrom(atributoTipo)) {
                    return criteriaBuilder.equal(root.get(atributoNombre), Boolean.parseBoolean((String) valor));
                } else if (LocalDate.class.isAssignableFrom(atributoTipo)) {
                    return criteriaBuilder.equal(root.get(atributoNombre), LocalDate.parse((String) valor));
                } else if (LocalDateTime.class.isAssignableFrom(atributoTipo)) {
                    return criteriaBuilder.equal(root.get(atributoNombre), LocalDateTime.parse((String) valor));
                } else {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }
    }
}
