package com.snapstock.global.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("size가 0 이하이면 IllegalArgumentException이 발생한다")
    void of_size_0이하_예외발생() {
        List<TestItem> items = List.of(new TestItem(1L, "a"));

        assertThatThrownBy(() -> CursorPageResponse.of(items, 0, TestItem::id))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CursorPageResponse.of(items, -1, TestItem::id))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("content가 null이면 NullPointerException이 발생한다")
    void of_content_null_예외발생() {
        assertThatThrownBy(() -> CursorPageResponse.of(null, 5, TestItem::id))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("반환된 content는 원본 리스트 변경에 영향받지 않는다")
    void of_반환_content_불변성() {
        ArrayList<TestItem> items = new ArrayList<>(List.of(
                new TestItem(3L, "a"),
                new TestItem(2L, "b")
        ));

        CursorPageResponse<TestItem> result = CursorPageResponse.of(items, 5, TestItem::id);
        items.add(new TestItem(1L, "c"));

        assertThat(result.content()).hasSize(2);
    }

    @Test
    @DisplayName("반환된 content는 수정 불가능하다")
    void of_반환_content_수정불가() {
        List<TestItem> items = List.of(new TestItem(1L, "a"));

        CursorPageResponse<TestItem> result = CursorPageResponse.of(items, 5, TestItem::id);

        assertThatThrownBy(() -> result.content().add(new TestItem(2L, "b")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
