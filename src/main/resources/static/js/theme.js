document.addEventListener('DOMContentLoaded', function () {
    var theme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-bs-theme', theme);
    updateThemeIcon(theme);

    document.getElementById('themeToggle').addEventListener('click', function () {
        var current = document.documentElement.getAttribute('data-bs-theme');
        var next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-bs-theme', next);
        localStorage.setItem('theme', next);
        updateThemeIcon(next);
    });

    function updateThemeIcon(t) {
        var icon = document.querySelector('#themeToggle i');
        if (icon) {
            icon.className = t === 'dark' ? 'bi bi-sun-fill' : 'bi bi-moon-fill';
        }
    }
});
