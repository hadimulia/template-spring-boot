var Toast = {
    show: function (message, type) {
        type = type || 'success';
        var map = {
            'success': 'toastSuccess',
            'error': 'toastError',
            'danger': 'toastError',
            'warning': 'toastWarning',
            'info': 'toastInfo'
        };
        var id = map[type] || 'toastInfo';
        var el = document.getElementById(id);
        if (!el) return;
        var body = el.querySelector('.toast-body');
        if (body) body.textContent = message;
        var toast = new bootstrap.Toast(el, { delay: 4000 });
        toast.show();
    },
    success: function (msg) { this.show(msg, 'success'); },
    error: function (msg) { this.show(msg, 'error'); },
    warning: function (msg) { this.show(msg, 'warning'); },
    info: function (msg) { this.show(msg, 'info'); }
};

document.addEventListener('DOMContentLoaded', function () {
    var successEl = document.getElementById('flashSuccess');
    if (successEl && successEl.textContent.trim() !== '') Toast.success(successEl.textContent);
    var errorEl = document.getElementById('flashError');
    if (errorEl && errorEl.textContent.trim() !== '') Toast.error(errorEl.textContent);
});
