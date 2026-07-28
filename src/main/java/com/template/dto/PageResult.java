package com.template.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class PageResult<T> {
    private List<T> data;
    private Pagination pagination;

    public static <T> PageResult<T> of(List<T> data, int total, int page, int size) {
        return PageResult.<T>builder()
                .data(data)
                .pagination(Pagination.of(total, page, size))
                .build();
    }

    @Data
    @Builder
    public static class Pagination {
        private int totalRecords;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        private int startPage;
        private int endPage;

        public static Pagination of(int total, int page, int size) {
            int totalPages = (int) Math.ceil((double) total / size);
            int startPage = Math.max(1, page - 5);
            int endPage = Math.min(totalPages, page + 4);

            if (endPage - startPage < 9) {
                if (startPage == 1) {
                    endPage = Math.min(totalPages, startPage + 9);
                } else {
                    startPage = Math.max(1, endPage - 9);
                }
            }

            return Pagination.builder()
                    .totalRecords(total)
                    .totalPages(totalPages)
                    .currentPage(page)
                    .pageSize(size)
                    .startPage(startPage)
                    .endPage(endPage)
                    .build();
        }
    }
}
