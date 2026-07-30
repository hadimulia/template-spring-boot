function ListManager(config) {
    var self = this;
    self.apiUrl = config.apiUrl;
    self.rowRenderer = config.rowRenderer;
    self.pageSize = config.pageSize || 10;
    self.keyword = '';
    self.currentPage = 1;

    self.load = function (page) {
        self.currentPage = page || 1;
        var url = self.apiUrl + '?keyword=' + encodeURIComponent(self.keyword) +
                  '&page=' + self.currentPage + '&size=' + self.pageSize;

        self.showLoading();
        setTimeout(function () {
        fetch(url)
            .then(function (r) { return r.json(); })
            .then(function (result) {
                self.renderRows(result.data, self.currentPage, self.pageSize);
                self.renderPagination(result.pagination);
                history.pushState(null, '', '?keyword=' + encodeURIComponent(self.keyword) +
                    '&page=' + self.currentPage + '&size=' + self.pageSize);
            });
        }, 400);
    };

    self.showLoading = function () {
        var tbody = document.getElementById('dataRows');
        var table = document.getElementById('dataTable');
        var colspan = table ? table.querySelector('thead tr').children.length : 4;
        tbody.innerHTML = '<tr><td colspan="' + colspan +
            '" class="text-center py-4"><div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>' + (window.appI18n?.loading || 'Loading...') + '</td></tr>';
    };

    self.renderRows = function (data, page, size) {
        var tbody = document.getElementById('dataRows');
        if (!data || data.length === 0) {
            var table = document.getElementById('dataTable');
            var colspan = table ? table.querySelector('thead tr').children.length : 4;
            tbody.innerHTML = '<tr><td colspan="' + colspan +
                '" class="text-center text-muted py-4">' + (window.appI18n?.noData || 'No data found') + '</td></tr>';
            return;
        }
        var html = '';
        for (var i = 0; i < data.length; i++) {
            html += self.rowRenderer(data[i], (page - 1) * size + i + 1);
        }
        tbody.innerHTML = html;
    };

    self.renderPagination = function (p) {
        var wrapper = document.getElementById('paginationWrapper');
        var ul = document.getElementById('pagination');
        if (!p || p.totalPages <= 1) {
            wrapper.style.display = 'none';
            return;
        }
        wrapper.style.display = '';
        var html = '';
        html += '<li class="page-item' + (p.currentPage === 1 ? ' disabled' : '') + '">' +
                '<a class="page-link page-link-ajax" data-page="1">First</a></li>';
        for (var i = p.startPage; i <= p.endPage; i++) {
            html += '<li class="page-item' + (i === p.currentPage ? ' active' : '') + '">' +
                    '<a class="page-link page-link-ajax" data-page="' + i + '">' + i + '</a></li>';
        }
        html += '<li class="page-item' + (p.currentPage === p.totalPages ? ' disabled' : '') + '">' +
                '<a class="page-link page-link-ajax" data-page="' + p.totalPages + '">Last</a></li>';
        ul.innerHTML = html;
    };

    // Event delegation for pagination clicks
    document.addEventListener('click', function (e) {
        var link = e.target.closest('.page-link-ajax');
        if (link && link.closest('#paginationWrapper')) {
            e.preventDefault();
            var page = parseInt(link.getAttribute('data-page'), 10);
            if (!isNaN(page)) {
                self.load(page);
            }
        }
    });
}
