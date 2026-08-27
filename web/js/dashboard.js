/**
 * Dashboard JavaScript controller.
 * Fetches metric summaries and renders vehicle cards dynamically.
 */

document.addEventListener('DOMContentLoaded', () => {
    loadDashboardData();
});

async function loadDashboardData() {
    try {
        const response = await fetch('/api/dashboard');
        const data = await response.json();

        if (data.success) {
            // Update Metrics
            document.getElementById('metric-total-vehicles').innerText = data.metrics.totalVehicles;
            document.getElementById('metric-available-vehicles').innerText = data.metrics.availableVehicles;
            document.getElementById('metric-rented-vehicles').innerText = data.metrics.rentedVehicles;
            document.getElementById('metric-total-customers').innerText = data.metrics.totalCustomers;
            document.getElementById('metric-active-rentals').innerText = data.metrics.activeRentals;
            document.getElementById('metric-total-revenue').innerText = formatCurrency(data.metrics.totalRevenue);

            // Render Vehicle Cards
            renderVehicleCards(data.vehicles);
        } else {
            showToast('Failed to load dashboard data', 'error');
        }
    } catch (error) {
        console.error('Error fetching dashboard metrics:', error);
        showToast('Database connection error. Ensure server is running.', 'error');
    }
}

function renderVehicleCards(vehicles) {
    const grid = document.getElementById('vehicle-cards-grid');
    if (!grid) return;

    if (!vehicles || vehicles.length === 0) {
        grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-muted);">No vehicles available in database.</div>`;
        return;
    }

    grid.innerHTML = vehicles.map(v => {
        const isAvailable = v.available;
        const statusBadge = isAvailable 
            ? `<span class="status-badge badge-available">Available</span>`
            : `<span class="status-badge badge-rented">Rented</span>`;
        
        let typeIcon = '🚗';
        if (v.type === 'Bike') typeIcon = '🏍️';
        if (v.type === 'Truck') typeIcon = '🚚';

        const rentButton = isAvailable
            ? `<a href="rent.html?vehicleId=${v.id}" class="btn btn-primary btn-sm">Rent Now</a>`
            : `<button class="btn btn-secondary btn-sm" disabled style="opacity:0.6; cursor:not-allowed;">Currently Rented</button>`;

        return `
            <div class="vehicle-card">
                <div class="card-banner">
                    <span class="card-banner-icon">${typeIcon}</span>
                    ${statusBadge}
                    <span class="badge-type">${v.type}</span>
                </div>
                <div class="card-body">
                    <div class="card-title">${escapeHtml(v.name)}</div>
                    <div class="card-subtitle">
                        <span>Code: <strong>${escapeHtml(v.code)}</strong></span>
                    </div>
                    <div class="card-details">
                        <div class="detail-item">
                            <span class="detail-label">Registration No:</span>
                            <span class="detail-value">${escapeHtml(v.vehicleNumber)}</span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">Rent Per Day:</span>
                            <span class="rent-price">${formatCurrency(v.rentPerDay)}</span>
                        </div>
                    </div>
                    <div class="card-footer">
                        ${rentButton}
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
