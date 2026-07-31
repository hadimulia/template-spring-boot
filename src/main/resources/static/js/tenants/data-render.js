document.addEventListener('DOMContentLoaded', function () {
    var lm = new ListManager({
        apiUrl: '/tenants/api/list',
        pageSize: 10,
        rowRenderer: function (row, index) {
            var statusBadge = row.status === 'ACTIVE'
                ? '<span class="badge bg-success">' + appI18n.tenantStatusActive + '</span>'
                : '<span class="badge bg-secondary">' + appI18n.tenantStatusInactive + '</span>';
            var deleteForm = '<form action="/tenants/' + row.id + '/delete" method="post" class="d-inline">' +
                '<button type="button" class="btn btn-sm btn-danger" data-bs-toggle="modal" data-bs-target="#confirmModal" data-modal-title="' + appI18n.confirmDeleteTenantTitle + '" data-modal-body="' + appI18n.confirmDeleteTenantBody + '" data-modal-btn="' + appI18n.modalConfirmDelete + '"><i class="bi bi-trash"></i></button></form>';
            return '<tr>' +
                '<td>' + index + '</td>' +
                '<td>' + (row.code || '') + '</td>' +
                '<td>' + (row.name || '') + '</td>' +
                '<td>' + (row.description || '') + '</td>' +
                '<td>' + statusBadge + '</td>' +
                '<td><a href="/tenants/' + row.id + '/edit" class="btn btn-sm btn-warning"><i class="bi bi-pencil"></i></a>' + deleteForm + '</td>' +
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
