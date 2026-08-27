/**
 * Rental History JavaScript Controller.
 * Loads complete audit log of rentals with search and status filters.
 */

document.addEventListener('DOMContentLoaded', () => {
    loadRentalHistory();

    document.getElementById('history-search')?.addEventListener('input', debounce(loadRentalHistory, 300));
    document.getElementById('filter-history-status')?.addEventListener('change', loadRentalHistory);
});

async function loadRentalHistory() {
    const search = document.getElementById('history-search')?.value || '';
    const status = document.getElementById('filter-history-status')?.value || '';

    const url = `/api/rentals?query=${encodeURIComponent(search)}&status=${encodeURIComponent(status)}`;

    try {
        const response = await fetch(url);
        const data = await response.json();

        if (data.success) {
            renderHistoryTable(data.rentals);
        } else {
            showToast(data.message || 'Error loading rental history', 'error');
        }
    } catch (err) {
        console.error('Error fetching history:', err);
    }
}

function renderHistoryTable(rentals) {
    const tbody = document.getElementById('history-table-body');
    if (!tbody) return;

    if (!rentals || rentals.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; padding: 40px; color: var(--text-muted);">No rental history records found.</td></tr>`;
        return;
    }

    tbody.innerHTML = rentals.map(r => {
        const isReturned = r.status === 'RETURNED';
        const statusBadge = isReturned
            ? `<span class="status-badge badge-available" style="position:static;">Returned</span>`
            : `<span class="status-badge badge-rented" style="position:static;">Active</span>`;

        return `
            <tr>
                <td><strong>${escapeHtml(r.rentalCode)}</strong></td>
                <td>${escapeHtml(r.customerName)}</td>
                <td>${escapeHtml(r.vehicleName)} <span class="badge-type" style="position:static;">${r.vehicleType}</span></td>
                <td>${escapeHtml(r.vehicleNumber)}</td>
                <td>${formatDate(r.rentalDate)}</td>
                <td>${formatDate(r.returnDate)}</td>
                <td>${r.days} Day(s)</td>
                <td><strong>${formatCurrency(r.totalAmount)}</strong></td>
                <td>${statusBadge}</td>
            </tr>
        `;
    }).join('');
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
