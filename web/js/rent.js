/**
 * Rent Vehicle JavaScript Controller.
 * Manages customer/vehicle selection, live total price calculation, validation, and receipt generation.
 */

let customersList = [];
let availableVehicles = [];
let selectedVehicle = null;

document.addEventListener('DOMContentLoaded', async () => {
    await loadFormData();

    // Check URL parameters for pre-selected vehicle ID
    const urlParams = new URLSearchParams(window.location.search);
    const preSelectedVehId = urlParams.get('vehicleId');

    if (preSelectedVehId) {
        const select = document.getElementById('rental-vehicle-select');
        if (select) {
            select.value = preSelectedVehId;
            handleVehicleSelectionChange();
        }
    }

    // Event Listeners for dynamic updates
    document.getElementById('rental-vehicle-select')?.addEventListener('change', handleVehicleSelectionChange);
    document.getElementById('rental-days')?.addEventListener('input', updatePriceCalculation);
    document.getElementById('rental-days')?.addEventListener('change', updatePriceCalculation);
    document.getElementById('rental-form')?.addEventListener('submit', handleProcessRental);
});

async function loadFormData() {
    try {
        // Fetch Customers
        const custRes = await fetch('/api/customers');
        const custData = await custRes.json();
        if (custData.success) {
            customersList = custData.customers;
            populateCustomerDropdown(customersList);
        }

        // Fetch Available Vehicles
        const vehRes = await fetch('/api/vehicles?availability=AVAILABLE');
        const vehData = await vehRes.json();
        if (vehData.success) {
            availableVehicles = vehData.vehicles;
            populateVehicleDropdown(availableVehicles);
        }

    } catch (err) {
        console.error('Error loading rental form data:', err);
        showToast('Error loading customers/vehicles data', 'error');
    }
}

function populateCustomerDropdown(customers) {
    const select = document.getElementById('rental-customer-select');
    if (!select) return;

    if (!customers || customers.length === 0) {
        select.innerHTML = `<option value="">-- No Customers Registered --</option>`;
        return;
    }

    select.innerHTML = `<option value="">-- Select Customer --</option>` +
        customers.map(c => `<option value="${c.id}">${escapeHtml(c.name)} (License: ${escapeHtml(c.licenseNumber)}, Age: ${c.age})</option>`).join('');
}

function populateVehicleDropdown(vehicles) {
    const select = document.getElementById('rental-vehicle-select');
    if (!select) return;

    if (!vehicles || vehicles.length === 0) {
        select.innerHTML = `<option value="">-- No Available Vehicles --</option>`;
        return;
    }

    select.innerHTML = `<option value="">-- Select Available Vehicle --</option>` +
        vehicles.map(v => `<option value="${v.id}">${escapeHtml(v.name)} [${escapeHtml(v.code)}] - ${formatCurrency(v.rentPerDay)}/day (${v.type})</option>`).join('');
}

function handleVehicleSelectionChange() {
    const vehId = document.getElementById('rental-vehicle-select').value;
    const previewBox = document.getElementById('vehicle-preview-card');

    if (!vehId) {
        selectedVehicle = null;
        if (previewBox) previewBox.style.display = 'none';
        updatePriceCalculation();
        return;
    }

    selectedVehicle = availableVehicles.find(v => v.id == vehId);
    if (selectedVehicle) {
        document.getElementById('prev-name').innerText = selectedVehicle.name;
        document.getElementById('prev-code').innerText = selectedVehicle.code;
        document.getElementById('prev-number').innerText = selectedVehicle.vehicleNumber;
        document.getElementById('prev-type').innerText = selectedVehicle.type;
        document.getElementById('prev-rent').innerText = formatCurrency(selectedVehicle.rentPerDay);

        if (previewBox) previewBox.style.display = 'block';
        updatePriceCalculation();
    }
}

function updatePriceCalculation() {
    const daysInput = document.getElementById('rental-days');
    const calcBox = document.getElementById('price-calc-container');

    if (!selectedVehicle || !daysInput) {
        if (calcBox) calcBox.style.display = 'none';
        return;
    }

    const days = parseInt(daysInput.value) || 0;
    const rentPerDay = selectedVehicle.rentPerDay;
    const totalAmount = rentPerDay * Math.max(0, days);

    document.getElementById('calc-rent-rate').innerText = formatCurrency(rentPerDay);
    document.getElementById('calc-days-count').innerText = days > 0 ? `${days} Day(s)` : '0 Days';
    document.getElementById('calc-formula').innerText = `${formatCurrency(rentPerDay)} × ${days}`;
    document.getElementById('calc-total-amount').innerText = formatCurrency(totalAmount);

    if (calcBox) calcBox.style.display = 'block';
}

async function handleProcessRental(e) {
    e.preventDefault();

    const customerId = document.getElementById('rental-customer-select').value;
    const vehicleId = document.getElementById('rental-vehicle-select').value;
    const days = document.getElementById('rental-days').value.trim();

    // Client-side validation feedback
    if (!customerId) {
        showToast('Please select a customer.', 'error');
        return;
    }

    if (!vehicleId) {
        showToast('Please select a vehicle.', 'error');
        return;
    }

    const daysInt = parseInt(days);
    if (isNaN(daysInt) || daysInt <= 0) {
        showToast('Number of days must be greater than zero.', 'error');
        return;
    }

    const payload = {
        customerId: customerId,
        vehicleId: vehicleId,
        days: daysInt
    };

    try {
        const response = await fetch('/api/rentals', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (data.success) {
            showToast('Booking Successful! Generating receipt...', 'success');
            renderReceiptModal(data.rental);
        } else {
            showToast(data.message || 'Rental booking failed.', 'error');
        }
    } catch (err) {
        console.error('Error booking rental:', err);
        showToast('Server error while booking rental.', 'error');
    }
}

function renderReceiptModal(rental) {
    const container = document.getElementById('receipt-container');
    if (!container) return;

    container.innerHTML = `
        <div class="receipt-card">
            <div class="receipt-header">
                <h2>VEHICLE RENTAL RECEIPT</h2>
                <div style="margin-top:8px;">
                    <span class="status-badge badge-available">Booking Successful</span>
                </div>
                <p style="margin-top:12px; font-size:13px; color:var(--text-secondary);">Rental ID: <strong>${escapeHtml(rental.rentalCode)}</strong></p>
                <p style="font-size:12px; color:var(--text-muted);">Rental Date: ${formatDate(rental.rentalDate)}</p>
            </div>

            <div class="receipt-grid">
                <div>
                    <div class="receipt-section-title">Customer Details</div>
                    <div style="font-size:14px; font-weight:700;">${escapeHtml(rental.customerName)}</div>
                    <div style="font-size:13px; color:var(--text-secondary);">Age: ${rental.customerAge} yrs</div>
                    <div style="font-size:13px; color:var(--text-secondary);">Phone: ${escapeHtml(rental.customerPhone)}</div>
                    <div style="font-size:13px; color:var(--text-secondary);">License: <code>${escapeHtml(rental.customerLicense)}</code></div>
                </div>

                <div>
                    <div class="receipt-section-title">Vehicle Details</div>
                    <div style="font-size:14px; font-weight:700;">${escapeHtml(rental.vehicleName)} (${escapeHtml(rental.vehicleType)})</div>
                    <div style="font-size:13px; color:var(--text-secondary);">Code: <strong>${escapeHtml(rental.vehicleCode)}</strong></div>
                    <div style="font-size:13px; color:var(--text-secondary);">Registration: ${escapeHtml(rental.vehicleNumber)}</div>
                    <div style="font-size:13px; color:var(--text-secondary);">Rent Rate: ${formatCurrency(rental.rentPerDay)}/day</div>
                </div>
            </div>

            <div class="calc-box">
                <div class="calc-row">
                    <span>Duration:</span>
                    <span><strong>${rental.days} Day(s)</strong></span>
                </div>
                <div class="calc-row">
                    <span>Rate Calculation:</span>
                    <span>${formatCurrency(rental.rentPerDay)} × ${rental.days}</span>
                </div>
                <div class="calc-row calc-total">
                    <span>Total Amount Paid:</span>
                    <span>${formatCurrency(rental.totalAmount)}</span>
                </div>
            </div>

            <div style="display:flex; justify-content:space-between; margin-top:24px;" class="no-print">
                <button class="btn btn-secondary" onclick="window.print()">🖨️ Print Receipt</button>
                <a href="index.html" class="btn btn-primary">Back to Dashboard</a>
            </div>
        </div>
    `;

    openModal('receipt-modal');
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
