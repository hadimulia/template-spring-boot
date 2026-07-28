document.addEventListener('DOMContentLoaded', function () {
    var lm = new ListManager({
        apiUrl: '/users/api/list',
        pageSize: 10,
        rowRenderer: function (row, index) {
            var rolesHtml = '';
            if (row.roles) {
                for (var j = 0; j < row.roles.length; j++) {
                    rolesHtml += '<span class="badge bg-info me-1">' + row.roles[j] + '</span>';
                }
            }
            var statusBadge = row.enabled
                ? '<span class="badge bg-success">Active</span>'
                : '<span class="badge bg-danger">Inactive</span>';
            var deleteForm = '<form action="/users/' + row.id + '/delete" method="post" class="d-inline">' +
                '<button type="button" class="btn btn-sm btn-danger" data-bs-toggle="modal" data-bs-target="#confirmModal" data-modal-title="Delete User" data-modal-body="Are you sure you want to delete this user?" data-modal-btn="Delete"><i class="bi bi-trash"></i></button></form>';
            return '<tr>' +
                '<td>' + index + '</td>' +
                '<td>' + (row.username || '') + '</td>' +
                '<td>' + (row.fullname || '') + '</td>' +
                '<td>' + (row.email || '') + '</td>' +
                '<td>' + rolesHtml + '</td>' +
                '<td>' + statusBadge + '</td>' +
                '<td><a href="/users/' + row.id + '/edit" class="btn btn-sm btn-warning"><i class="bi bi-pencil"></i></a>' + deleteForm + '</td>' +
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
