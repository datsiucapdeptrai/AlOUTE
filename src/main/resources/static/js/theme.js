document.addEventListener("DOMContentLoaded", function () {
    const themeToggleBtn = document.getElementById('themeToggle');
    if (!themeToggleBtn) return;

    const themeIcon = themeToggleBtn.querySelector('i');
    const body = document.body;

    // Kiểm tra xem trước đó người dùng có bật Dark Mode không
    const currentTheme = localStorage.getItem('theme');
    if (currentTheme === 'dark') {
        body.classList.add('dark-mode');
        themeIcon.classList.replace('bi-moon-fill', 'bi-sun-fill');
    }

    // Bắt sự kiện click
    themeToggleBtn.addEventListener('click', function () {
        body.classList.toggle('dark-mode');
        
        if (body.classList.contains('dark-mode')) {
            localStorage.setItem('theme', 'dark');
            themeIcon.classList.replace('bi-moon-fill', 'bi-sun-fill');
        } else {
            localStorage.setItem('theme', 'light');
            themeIcon.classList.replace('bi-sun-fill', 'bi-moon-fill');
        }
    });
});