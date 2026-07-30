document.addEventListener('DOMContentLoaded', function () {
    var lm = new ListManager({
        apiUrl: '/menus/api/list',
        pageSize: 10,
        rowRenderer: function (row, index) {
            var visibleBadge = row.visible
                ? '<span class="badge bg-success">Yes</span>'
                : '<span class="badge bg-danger">No</span>';
            var deleteForm = '<form action="/menus/' + row.id + '/delete" method="post" class="d-inline">' +
                '<button type="button" class="btn btn-sm btn-danger" data-bs-toggle="modal" data-bs-target="#confirmModal" data-modal-title="' + appI18n.confirmDeleteMenuTitle + '" data-modal-body="' + appI18n.confirmDeleteMenuBody + '" data-modal-btn="' + appI18n.modalConfirmDelete + '"><i class="bi bi-trash"></i></button></form>';
            return '<tr>' +
                '<td>' + index + '</td>' +
                '<td>' + t(row.i18nKey, row.name || '') + '</td>' +
                '<td>' + t(row.parentI18nKey, row.parentName || '') + '</td>' +
                '<td><code>' + (row.url || '') + '</code></td>' +
                '<td><i class="' + (row.icon || '') + '"></i> ' + (row.icon || '') + '</td>' +
                '<td>' + (row.sortOrder || '') + '</td>' +
                '<td>' + visibleBadge + '</td>' +
                '<td><a href="/menus/' + row.id + '/edit" class="btn btn-sm btn-warning"><i class="bi bi-pencil"></i></a>' + deleteForm + '</td>' +
                '</tr>';
        }
    });

    document.getElementById('searchBtn').addEventListener('click', function () {
        lm.keyword = document.getElementById('searchKeyword').value;
        lm.load(1);
    });
    document.getElementById('searchKeyword').addEventListener('keyup', function (e) {
        if (e.key === 'Enter') {
            lm.keyword = this.value;
            lm.load(1);
        }
    });
    document.getElementById('refreshBtn').addEventListener('click', function (e) {
        e.preventDefault();
        document.getElementById('searchKeyword').value = '';
        lm.keyword = '';
        lm.load(1);
    });
});
