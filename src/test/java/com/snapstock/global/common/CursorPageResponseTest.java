package com.snapstock.global.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CursorPageResponseTest {

    record TestItem(Long id, String name) {
    }

    @Test
    @DisplayName("데이터가 size보다 적을 때 hasNext는 false이다")
    void of_데이터가_size보다_적을때_hasNext_false() {
        List<TestItem> items = List.of(
                new TestItem(3L, "a"),
                new TestItem(2L, "b")
        );

        CursorPageResponse<TestItem> result = CursorPageResponse.of(items, 5, TestItem::id);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.content()).hasSize(2);
    }

    @Test
    @DisplayName("데이터가 size와 같을 때 hasNext는 false이다")
    void of_데이터가_size와_같을때_hasNext_false() {
        List<TestItem> items = List.of(
                new TestItem(5L, "a"),
                new TestItem(4L, "b"),
                new TestItem(3L, "c")
        );

        CursorPageResponse<TestItem> result = CursorPageResponse.of(items, 3, TestItem::id);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.content()).hasSize(3);
    }

    @Test
    @DisplayName("데이터가 size보다 많을 때 hasNext는 true이고 content는 size개로 잘린다")
    void of_데이터가_size보다_많을때_hasNext_true() {
        List<TestItem> items = List.of(
                new TestItem(5L, "a"),
                new TestItem(4L, "b"),
                new TestItem(3L, "c"),
                new TestItem(2L, "d")
        );

        CursorPageResponse<TestItem> result = CursorPageResponse.of(items, 3, TestItem::id);

        assertThat(result.hasNext()).isTrue();
        assertThat(result.content()).hasSize(3);
        assertThat(result.nextCursor()).isEqualTo(3L);
    }

    @Test
    @DisplayName("빈 리스트일 때 hasNext는 false이다")
    void of_빈_리스트_hasNext_false() {
        CursorPageResponse<TestItem> result = CursorPageResponse.of(
                Collections.emptyList(), 5, TestItem::id);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.content()).isEmpty();
    }
}
