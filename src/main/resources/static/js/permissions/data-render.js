document.addEventListener('DOMContentLoaded', function () {
    var lm = new ListManager({
        apiUrl: '/permissions/api/list',
        pageSize: 10,
        rowRenderer: function (row, index) {
            var deleteForm = '<form action="/permissions/' + row.id + '/delete" method="post" class="d-inline">' +
                '<button type="button" class="btn btn-sm btn-danger" data-bs-toggle="modal" data-bs-target="#confirmModal" data-modal-title="Delete Permission" data-modal-body="Are you sure you want to delete this permission?" data-modal-btn="Delete"><i class="bi bi-trash"></i></button></form>';
            return '<tr>' +
                '<td>' + index + '</td>' +
                '<td><code>' + (row.code || '') + '</code></td>' +
                '<td>' + (row.description || '') + '</td>' +
                '<td><a href="/permissions/' + row.id + '/edit" class="btn btn-sm btn-warning"><i class="bi bi-pencil"></i></a>' + deleteForm + '</td>' +
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
