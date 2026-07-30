document.addEventListener('DOMContentLoaded', function () {
    var lm = new ListManager({
        apiUrl: '/approvals/api/list',
        pageSize: 10,
        rowRenderer: function (row, index) {
            var statusBadge = row.status === 'PENDING' ? 'bg-warning text-dark' :
                row.status === 'APPROVED' ? 'bg-success' :
                row.status === 'REJECTED' ? 'bg-danger' : 'bg-secondary';
            var ts = row.submittedAt ? row.submittedAt.substring(0, 16).replace('T', ' ') : '-';
            return '<tr>' +
                '<td>' + index + '</td>' +
                '<td><span class="badge bg-secondary">' + (row.entityType || '') + '</span></td>' +
                '<td>' + (row.requestType || '') + '</td>' +
                '<td><span class="badge ' + statusBadge + '">' + (row.status || '') + '</span></td>' +
                '<td>' + (row.submittedBy || '') + '</td>' +
                '<td style="white-space:nowrap">' + ts + '</td>' +
                '<td>' + (row.reviewedBy || '-') + '</td>' +
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
