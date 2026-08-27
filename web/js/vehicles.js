/**
 * Vehicles Page JavaScript controller.
 * Handles Vehicle list rendering, searching, filtering, adding, editing, and deleting.
 */

document.addEventListener('DOMContentLoaded', () => {
    loadVehicles();

    // Event listeners for search and filters
    document.getElementById('vehicle-search')?.addEventListener('input', debounce(loadVehicles, 300));
    document.getElementById('filter-type')?.addEventListener('change', loadVehicles);
    document.getElementById('filter-availability')?.addEventListener('change', loadVehicles);

    // Form submit listener
    document.getElementById('vehicle-form')?.addEventListener('submit', handleSaveVehicle);
});

let currentEditingId = null;

async function loadVehicles() {
    const search = document.getElementById('vehicle-search')?.value || '';
    const type = document.getElementById('filter-type')?.value || '';
    const availability = document.getElementById('filter-availability')?.value || '';

    const url = `/api/vehicles?query=${encodeURIComponent(search)}&type=${encodeURIComponent(type)}&availability=${encodeURIComponent(availability)}`;

    try {
        const response = await fetch(url);
        const data = await response.json();

        if (data.success) {
            renderVehiclesTable(data.vehicles);
        } else {
            showToast(data.message || 'Error loading vehicles', 'error');
        }
    } catch (e) {
        console.error('Error loading vehicles:', e);
    }
}

function renderVehiclesTable(vehicles) {
    const tbody = document.getElementById('vehicles-table-body');
    if (!tbody) return;

    if (!vehicles || vehicles.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 30px; color: var(--text-muted);">No vehicles found matching your criteria.</td></tr>`;
        return;
    }

    tbody.innerHTML = vehicles.map(v => {
        const isAvail = v.available;
        const statusBadge = isAvail 
            ? `<span class="status-badge badge-available">Available</span>`
            : `<span class="status-badge badge-rented">Rented</span>`;

        const rentBtn = isAvail
            ? `<a href="rent.html?vehicleId=${v.id}" class="btn btn-primary btn-sm">Rent</a>`
            : `<button class="btn btn-secondary btn-sm" disabled style="opacity:0.5; cursor:not-allowed;">Rented</button>`;

        return `
            <tr>
                <td><strong>${escapeHtml(v.code)}</strong></td>
                <td>${escapeHtml(v.name)}</td>
                <td><span class="badge-type" style="position:static; background:#334155;">${escapeHtml(v.type)}</span></td>
                <td>${escapeHtml(v.vehicleNumber)}</td>
                <td><strong>${formatCurrency(v.rentPerDay)}</strong></td>
                <td>${statusBadge}</td>
                <td>
                    <div style="display:flex; gap:6px;">
                        ${rentBtn}
                        <button class="btn btn-secondary btn-sm" onclick="openEditModal(${v.id}, '${escapeJs(v.code)}', '${escapeJs(v.name)}', '${escapeJs(v.vehicleNumber)}', '${escapeJs(v.type)}', ${v.rentPerDay})">Edit</button>
                        <button class="btn btn-danger btn-sm" onclick="confirmDeleteVehicle(${v.id}, '${escapeJs(v.name)}')">Delete</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function openAddModal() {
    currentEditingId = null;
    document.getElementById('modal-title').innerText = 'Add New Vehicle';
    document.getElementById('vehicle-form').reset();
    document.getElementById('vehicle-id').value = '';
    document.getElementById('v-code').disabled = false;
    openModal('vehicle-modal');
}

function openEditModal(id, code, name, vehicleNumber, type, rentPerDay) {
    currentEditingId = id;
    document.getElementById('modal-title').innerText = 'Edit Vehicle Details';
    document.getElementById('vehicle-id').value = id;
    document.getElementById('v-code').value = code;
    document.getElementById('v-code').disabled = true; // Keep code unique & immutable in edit
    document.getElementById('v-name').value = name;
    document.getElementById('v-number').value = vehicleNumber;
    document.getElementById('v-type').value = type;
    document.getElementById('v-rent').value = rentPerDay;
    openModal('vehicle-modal');
}

async function handleSaveVehicle(e) {
    e.preventDefault();

    const id = document.getElementById('vehicle-id').value;
    const code = document.getElementById('v-code').value.trim();
    const name = document.getElementById('v-name').value.trim();
    const vehicleNumber = document.getElementById('v-number').value.trim();
    const type = document.getElementById('v-type').value;
    const rentPerDay = document.getElementById('v-rent').value.trim();

    // Frontend validation feedback
    if (!code || !name || !vehicleNumber || !type || !rentPerDay) {
        showToast('Please fill in all vehicle fields.', 'error');
        return;
    }

    if (parseFloat(rentPerDay) <= 0) {
        showToast('Rent per day must be a positive number.', 'error');
        return;
    }

    const payload = {
        action: id ? 'UPDATE' : 'ADD',
        id: id,
        code: code,
        name: name,
        vehicleNumber: vehicleNumber,
        type: type,
        rentPerDay: rentPerDay
    };

    try {
        const response = await fetch('/api/vehicles', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (data.success) {
            showToast(data.message, 'success');
            closeModal('vehicle-modal');
            loadVehicles();
        } else {
            showToast(data.message || 'Validation error', 'error');
        }
    } catch (err) {
        console.error('Error saving vehicle:', err);
        showToast('Failed to save vehicle.', 'error');
    }
}

async function confirmDeleteVehicle(id, name) {
    if (!confirm(`Are you sure you want to delete vehicle "${name}"?`)) {
        return;
    }

    try {
        const response = await fetch('/api/vehicles', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'DELETE', id: id })
        });
        const data = await response.json();

        if (data.success) {
            showToast(data.message, 'success');
            loadVehicles();
        } else {
            showToast(data.message || 'Cannot delete vehicle.', 'error');
        }
    } catch (err) {
        showToast('Error deleting vehicle', 'error');
    }
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

function escapeJs(str) {
    if (!str) return '';
    return str.replace(/'/g, "\\'").replace(/"/g, '\\"');
}
