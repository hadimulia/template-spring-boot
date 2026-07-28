// Auto-hide flash messages after 5 seconds
document.addEventListener('DOMContentLoaded', function() {
    var alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            var bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});

// Confirm modal
var confirmModalEl = document.getElementById('confirmModal');
if (confirmModalEl) {
    var confirmModal = new bootstrap.Modal(confirmModalEl);
    var confirmBtn = document.getElementById('confirmModalBtn');
    var pendingForm = null;

    confirmModalEl.addEventListener('show.bs.modal', function (event) {
        var trigger = event.relatedTarget;
        pendingForm = trigger.closest('form');
        document.getElementById('confirmModalTitle').textContent =
            trigger.getAttribute('data-modal-title') || 'Confirm';
        document.getElementById('confirmModalBody').textContent =
            trigger.getAttribute('data-modal-body') || 'Are you sure?';
        confirmBtn.textContent =
            trigger.getAttribute('data-modal-btn') || 'Delete';
        confirmBtn.className =
            'btn ' + (trigger.getAttribute('data-modal-btn-class') || 'btn-danger');
    });

    confirmBtn.addEventListener('click', function () {
        if (pendingForm) {
            pendingForm.submit();
        }
        confirmModal.hide();
    });
}
