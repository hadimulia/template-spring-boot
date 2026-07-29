document.addEventListener('DOMContentLoaded', function () {
    var lm = new ListManager({
        apiUrl: '/audit-logs/api/list',
        pageSize: 10,
        rowRenderer: function (row, index) {
            var actionBadge = row.action === 'CREATE' ? 'bg-success' :
                row.action === 'UPDATE' ? 'bg-primary' :
                row.action === 'DELETE' ? 'bg-danger' : 'bg-info';
            var ts = row.performedAt ? row.performedAt.substring(0, 16).replace('T', ' ') : '-';
            return '<tr>' +
                '<td>' + index + '</td>' +
                '<td style="white-space:nowrap">' + ts + '</td>' +
                '<td>' + (row.performedBy || '') + '</td>' +
                '<td><span class="badge bg-secondary">' + (row.entityType || '') + '</span></td>' +
                '<td><span class="badge ' + actionBadge + '">' + (row.action || '') + '</span></td>' +
                '<td style="max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">' + (row.description || '') + '</td>' +
                '<td><code>' + (row.ipAddress || '') + '</code></td>' +
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
