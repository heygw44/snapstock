package com.snapstock.global.common;

import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    @DisplayName("BaseEntity에 @MappedSuperclass 어노테이션이 존재한다")
    void BaseEntity_MappedSuperclass_어노테이션_존재() {
        assertThat(BaseEntity.class.isAnnotationPresent(MappedSuperclass.class))
                .isTrue();
    }

    @Test
    @DisplayName("BaseEntity에 createdAt, updatedAt 필드가 존재한다")
    void BaseEntity_createdAt_updatedAt_필드_존재() throws NoSuchFieldException {
        Field createdAt = BaseEntity.class.getDeclaredField("createdAt");
        Field updatedAt = BaseEntity.class.getDeclaredField("updatedAt");

        assertThat(createdAt).isNotNull();
        assertThat(updatedAt).isNotNull();
    }
}
