// Small shared UI helpers for the DepLens pages (no libraries).

(function () {
    let stack = null;

    function getStack() {
        if (stack && document.body.contains(stack)) return stack;
        stack = document.createElement('div');
        stack.className = 'toast-stack';
        stack.setAttribute('role', 'status');
        stack.setAttribute('aria-live', 'polite');
        document.body.appendChild(stack);
        return stack;
    }

    const ICONS = {
        success: '<path d="M20 6 9 17l-5-5"/>',
        error: '<path d="M12 8v5M12 16.5h.01"/><circle cx="12" cy="12" r="9"/>',
        info: '<path d="M12 11v5M12 7.5h.01"/><circle cx="12" cy="12" r="9"/>'
    };

    /**
     * Shows a short message in the bottom-right corner.
     * type: 'success' | 'error' | 'info'
     */
    window.showToast = function (message, type, timeout) {
        type = ICONS[type] ? type : 'info';
        const toast = document.createElement('div');
        toast.className = 'toast toast-' + type;

        const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        icon.setAttribute('viewBox', '0 0 24 24');
        icon.setAttribute('width', '18');
        icon.setAttribute('height', '18');
        icon.setAttribute('fill', 'none');
        icon.setAttribute('stroke', 'currentColor');
        icon.setAttribute('stroke-width', '2.2');
        icon.setAttribute('stroke-linecap', 'round');
        icon.setAttribute('aria-hidden', 'true');
        icon.innerHTML = ICONS[type];

        const text = document.createElement('span');
        text.className = 'toast-text';
        text.textContent = message;

        const close = document.createElement('button');
        close.className = 'toast-close';
        close.setAttribute('aria-label', 'Dismiss');
        close.innerHTML = '&times;';

        toast.append(icon, text, close);
        getStack().appendChild(toast);

        let timer = null;
        const dismiss = () => {
            clearTimeout(timer);
            toast.classList.add('is-leaving');
            setTimeout(() => toast.remove(), 200);
        };
        close.addEventListener('click', dismiss);
        timer = setTimeout(dismiss, timeout || (type === 'error' ? 6000 : 3500));
        toast.addEventListener('mouseenter', () => clearTimeout(timer));
        toast.addEventListener('mouseleave', () => { timer = setTimeout(dismiss, 2000); });
        return dismiss;
    };
})();
