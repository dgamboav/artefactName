package com.groupName.artefactName.utiles;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilterTypeInfererTest {

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Root<Object> root;

    @Mock
    private EntityType<Object> entityType;

    @Mock
    private SingularAttribute<Object, String> stringAttribute;

    @Mock
    private SingularAttribute<Object, Long> longAttribute;

    @Mock
    private SingularAttribute<Object, Integer> integerAttribute;

    @Mock
    private SingularAttribute<Object, Double> doubleAttribute;

    @Mock
    private SingularAttribute<Object, Float> floatAttribute;

    @Mock
    private SingularAttribute<Object, Boolean> booleanAttribute;

    @Mock
    private SingularAttribute<Object, LocalDate> localDateAttribute;

    @Mock
    private SingularAttribute<Object, LocalDateTime> localDateTimeAttribute;

    @InjectMocks
    private FilterTypeInferer filterTypeInferer;

    @Test
    void shouldCreateLikePredicateForStringAttribute() {
        when(entityType.getDeclaredSingularAttribute("nombre")).thenReturn((SingularAttribute) stringAttribute);
        when(stringAttribute.getJavaType()).thenReturn(String.class);
        when(root.get("nombre")).thenReturn(Mockito.mock());
        when(criteriaBuilder.like(Mockito.any(), Mockito.anyString())).thenReturn(Mockito.mock(Predicate.class));

        Predicate predicate = filterTypeInferer.inferPredicate(criteriaBuilder, root, "nombre", "test", entityType);
        assertNotNull(predicate);
        Mockito.verify(criteriaBuilder).like(Mockito.any(), Mockito.eq("%test%"));
    }

    @Test
    void shouldCreateEqualPredicateForLongAttribute() {
        when(entityType.getDeclaredSingularAttribute("id")).thenReturn((SingularAttribute) longAttribute);
        when(longAttribute.getJavaType()).thenReturn(Long.class);
        when(root.get("id")).thenReturn(Mockito.mock());
        when(criteriaBuilder.equal(Mockito.any(), Mockito.anyLong())).thenReturn(Mockito.mock(Predicate.class));

        Predicate predicate = filterTypeInferer.inferPredicate(criteriaBuilder, root, "id", "123", entityType);
        assertNotNull(predicate);
        Mockito.verify(criteriaBuilder).equal(Mockito.any(), Mockito.eq(123L));
    }

    @Test
    void shouldCreateEqualPredicateForIntegerAttribute() {
        when(entityType.getDeclaredSingularAttribute("cantidad")).thenReturn((SingularAttribute) integerAttribute);
        when(integerAttribute.getJavaType()).thenReturn(Integer.class);
        when(root.get("cantidad")).thenReturn(Mockito.mock());
        when(criteriaBuilder.equal(Mockito.any(), Mockito.anyInt())).thenReturn(Mockito.mock(Predicate.class));

        Predicate predicate = filterTypeInferer.inferPredicate(criteriaBuilder, root, "cantidad", "5", entityType);
        assertNotNull(predicate);
        Mockito.verify(criteriaBuilder).equal(Mockito.any(), Mockito.eq(5));
    }

    @Test
    void shouldCreateEqualPredicateForDoubleAttribute() {
        when(entityType.getDeclaredSingularAttribute("precio")).thenReturn((SingularAttribute)doubleAttribute);
        when(doubleAttribute.getJavaType()).thenReturn(Double.class);
        when(root.get("precio")).thenReturn(Mockito.mock());
        when(criteriaBuilder.equal(Mockito.any(), Mockito.anyDouble())).thenReturn(Mockito.mock(Predicate.class));

        Predicate predicate = filterTypeInferer.inferPredicate(criteriaBuilder, root, "precio", "99.99", entityType);
        assertNotNull(predicate);
        Mockito.verify(criteriaBuilder).equal(Mockito.any(), Mockito.eq(99.99));
    }

    @Test
    void shouldCreateEqualPredicateForFloatAttribute() {
        when(entityType.getDeclaredSingularAttribute("tasa")).thenReturn((SingularAttribute)floatAttribute);
        when(floatAttribute.getJavaType()).thenReturn(Float.class);
        when(root.get("tasa")).thenReturn(Mockito.mock());
        when(criteriaBuilder.equal(Mockito.any(), Mockito.anyFloat())).thenReturn(Mockito.mock(Predicate.class));

        Predicate predicate = filterTypeInferer.inferPredicate(criteriaBuilder, root, "tasa", "0.5", entityType);
        assertNotNull(predicate);
        Mockito.verify(criteriaBuilder).equal(Mockito.any(), Mockito.eq(0.5f));
    }

    @Test
    void shouldCreateEqualPredicateForBooleanAttribute() {
        when(entityType.getDeclaredSingularAttribute("activo")).thenReturn((SingularAttribute)booleanAttribute);
        when(booleanAttribute.getJavaType()).thenReturn(Boolean.class);
        when(root.get("activo")).thenReturn(Mockito.mock());
        when(criteriaBuilder.equal(Mockito.any(), Mockito.anyBoolean())).thenReturn(Mockito.mock(Predicate.class));

        Predicate predicate = filterTypeInferer.inferPredicate(criteriaBuilder, root, "activo", "true", entityType);
        assertNotNull(predicate);
        Mockito.verify(criteriaBuilder).equal(Mockito.any(), Mockito.eq(true));
    }

    @Test
    void shouldCreateEqualPredicateForLocalDateAttribute() {
        LocalDate now = LocalDate.now();
        when(entityType.getDeclaredSingularAttribute("fecha")).thenReturn((SingularAttribute)localDateAttribute);
        when(localDateAttribute.getJavaType()).thenReturn(LocalDate.class);
        when(root.get("fecha")).thenReturn(Mockito.mock());
        when(criteriaBuilder.equal(Mockito.any(), Mockito.any(LocalDate.class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate predicate = filterTypeInferer.inferPredicate(criteriaBuilder, root, "fecha", now.toString(), entityType);
        assertNotNull(predicate);
        Mockito.verify(criteriaBuilder).equal(Mockito.any(), Mockito.eq(now));
    }

    @Test
    void shouldCreateEqualPredicateForLocalDateTimeAttribute() {
        LocalDateTime now = LocalDateTime.now();
        when(entityType.getDeclaredSingularAttribute("fechaHora")).thenReturn((SingularAttribute)localDateTimeAttribute);
        when(localDateTimeAttribute.getJavaType()).thenReturn(LocalDateTime.class);
        when(root.get("fechaHora")).thenReturn(Mockito.mock());
        when(criteriaBuilder.equal(Mockito.any(), Mockito.any(LocalDateTime.class))).thenReturn(Mockito.mock(Predicate.class));

        Predicate predicate = filterTypeInferer.inferPredicate(criteriaBuilder, root, "fechaHora", now.toString(), entityType);
        assertNotNull(predicate);
        Mockito.verify(criteriaBuilder).equal(Mockito.any(), Mockito.eq(now));
    }

}