/**
 * Customers Page JavaScript controller.
 * Handles customer listing, search, registration, and deletion.
 */

document.addEventListener('DOMContentLoaded', () => {
    loadCustomers();

    document.getElementById('customer-search')?.addEventListener('input', debounce(loadCustomers, 300));
    document.getElementById('customer-form')?.addEventListener('submit', handleAddCustomer);
});

async function loadCustomers() {
    const search = document.getElementById('customer-search')?.value || '';
    const url = `/api/customers?query=${encodeURIComponent(search)}`;

    try {
        const response = await fetch(url);
        const data = await response.json();

        if (data.success) {
            renderCustomersTable(data.customers);
        } else {
            showToast(data.message || 'Error loading customers', 'error');
        }
    } catch (e) {
        console.error('Error loading customers:', e);
    }
}

function renderCustomersTable(customers) {
    const tbody = document.getElementById('customers-table-body');
    if (!tbody) return;

    if (!customers || customers.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; padding: 30px; color: var(--text-muted);">No customers found.</td></tr>`;
        return;
    }

    tbody.innerHTML = customers.map(c => `
        <tr>
            <td><strong>#CUST-${c.id}</strong></td>
            <td>${escapeHtml(c.name)}</td>
            <td>${c.age} yrs</td>
            <td>${escapeHtml(c.phone)}</td>
            <td><code>${escapeHtml(c.licenseNumber)}</code></td>
            <td>
                <button class="btn btn-danger btn-sm" onclick="confirmDeleteCustomer(${c.id}, '${escapeJs(c.name)}')">Delete</button>
            </td>
        </tr>
    `).join('');
}

function openAddCustomerModal() {
    document.getElementById('customer-form').reset();
    openModal('customer-modal');
}

async function handleAddCustomer(e) {
    e.preventDefault();

    const name = document.getElementById('c-name').value.trim();
    const age = document.getElementById('c-age').value.trim();
    const phone = document.getElementById('c-phone').value.trim();
    const licenseNumber = document.getElementById('c-license').value.trim();

    // Frontend validation
    if (!name) {
        showToast('Customer name is required.', 'error');
        return;
    }

    if (!age || parseInt(age) < 18) {
        showToast('Customer must be at least 18 years old.', 'error');
        return;
    }

    const cleanPhone = phone.replace(/[^0-9]/g, '');
    if (cleanPhone.length !== 10) {
        showToast('Phone number must contain exactly 10 digits.', 'error');
        return;
    }

    if (!licenseNumber) {
        showToast('Driving license number is required.', 'error');
        return;
    }

    const payload = {
        name: name,
        age: age,
        phone: cleanPhone,
        licenseNumber: licenseNumber
    };

    try {
        const response = await fetch('/api/customers', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (data.success) {
            showToast(data.message, 'success');
            closeModal('customer-modal');
            loadCustomers();
        } else {
            showToast(data.message || 'Customer registration failed.', 'error');
        }
    } catch (err) {
        showToast('Failed to register customer.', 'error');
    }
}

async function confirmDeleteCustomer(id, name) {
    if (!confirm(`Are you sure you want to delete customer "${name}"?`)) {
        return;
    }

    try {
        const response = await fetch('/api/customers', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ action: 'DELETE', id: id })
        });
        const data = await response.json();

        if (data.success) {
            showToast(data.message, 'success');
            loadCustomers();
        } else {
            showToast(data.message || 'Cannot delete customer.', 'error');
        }
    } catch (err) {
        showToast('Error deleting customer.', 'error');
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
