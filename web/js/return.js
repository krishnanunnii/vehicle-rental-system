/**
 * Return Vehicle JavaScript Controller.
 * Lists active rentals and handles vehicle return submission.
 */

document.addEventListener('DOMContentLoaded', () => {
    loadActiveRentals();
});

async function loadActiveRentals() {
    try {
        const response = await fetch('/api/returns');
        const data = await response.json();

        if (data.success) {
            renderActiveRentalsTable(data.activeRentals);
        } else {
            showToast(data.message || 'Error loading active rentals', 'error');
        }
    } catch (err) {
        console.error('Error fetching active rentals:', err);
    }
}

function renderActiveRentalsTable(rentals) {
    const tbody = document.getElementById('active-rentals-body');
    if (!tbody) return;

    if (!rentals || rentals.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; padding: 40px; color: var(--text-muted);">No active vehicle rentals at this moment. All vehicles are in garage.</td></tr>`;
        return;
    }

    tbody.innerHTML = rentals.map(r => `
        <tr>
            <td><strong>${escapeHtml(r.rentalCode)}</strong></td>
            <td>${escapeHtml(r.customerName)}<br><small style="color:var(--text-muted);">${escapeHtml(r.customerPhone)}</small></td>
            <td>${escapeHtml(r.vehicleName)} <span class="badge-type" style="position:static;">${r.vehicleType}</span></td>
            <td>${escapeHtml(r.vehicleNumber)}</td>
            <td>${formatDate(r.rentalDate)}</td>
            <td>${r.days} Day(s)</td>
            <td><strong>${formatCurrency(r.totalAmount)}</strong></td>
            <td>
                <button class="btn btn-success btn-sm" onclick="processReturnVehicle(${r.id}, '${escapeJs(r.vehicleName)}', '${escapeJs(r.customerName)}')">Return Vehicle</button>
            </td>
        </tr>
    `).join('');
}

async function processReturnVehicle(rentalId, vehicleName, customerName) {
    if (!confirm(`Confirm return of vehicle "${vehicleName}" from customer "${customerName}"?`)) {
        return;
    }

    try {
        const response = await fetch('/api/returns', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ rentalId: rentalId })
        });

        const data = await response.json();

        if (data.success) {
            showToast(data.message, 'success');
            loadActiveRentals();
        } else {
            showToast(data.message || 'Failed to process return.', 'error');
        }
    } catch (err) {
        showToast('Error processing vehicle return.', 'error');
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function escapeJs(str) {
    if (!str) return '';
    return str.replace(/'/g, "\\'").replace(/"/g, '\\"');
}
