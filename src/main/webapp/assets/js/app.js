/**
 * HASHbid Master Client Script — assets/js/app.js
 * "Four founders. One marketplace."
 * Shared Utilities: HASHbid Intro Animation, Demo Timer, Image Preview, In-DOM Filter, Toast Notifications
 */

// 1. HASHbid Landing Page Intro Animation Controller (Scenes 1 - 4)
(function initHashbidIntro() {
    const overlay = document.getElementById("hbIntroOverlay");
    if (!overlay) return;

    // Check prefers-reduced-motion: if reduced, skip immediately
    if (window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        overlay.style.display = "none";
        overlay.setAttribute("aria-hidden", "true");
        return;
    }

    // Pacing: Total cinematic experience ~3.4 seconds, followed by smooth 700ms dissolve
    const fadeOutDelay = 3400;
    const removeDelay = 4100;

    function dismissIntro() {
        if (!overlay || overlay.classList.contains("hb-fade-out")) return;
        overlay.classList.add("hb-fade-out");
        overlay.setAttribute("aria-hidden", "true");
        setTimeout(() => {
            if (overlay && overlay.parentNode) {
                overlay.parentNode.removeChild(overlay);
            }
        }, 700);
        window.removeEventListener("keydown", onKeyDown);
    }

    const timer = setTimeout(dismissIntro, fadeOutDelay);

    // Safeguard: Allow immediate click/tap to dismiss
    overlay.addEventListener("click", () => {
        clearTimeout(timer);
        dismissIntro();
    });

    // Safeguard: Allow keyboard dismissal (Escape, Space, Enter)
    function onKeyDown(e) {
        if (e.key === "Escape" || e.key === " " || e.key === "Enter") {
            clearTimeout(timer);
            dismissIntro();
        }
    }
    window.addEventListener("keydown", onKeyDown);
})();

// 2. Dynamic Homepage Featured Live Auction Controller (Targeting #auctions & #auctionTimer)
(function initFeaturedLiveAuction() {
    const card = document.getElementById("auctions");
    const hero = document.querySelector(".hero, .bs-hero");
    if (!card) return;

    let countdownInterval = null;
    let pollInterval = null;
    let currentEndTime = null;

    async function fetchFeaturedAuction() {
        try {
            const response = await fetch("featured-live-auction", {
                headers: { "Accept": "application/json" },
                cache: "no-store"
            });
            if (!response.ok) {
                hideCard();
                return;
            }
            const data = await response.json();
            if (data && data.hasLiveAuction && data.endTime) {
                renderCard(data);
            } else {
                hideCard();
            }
        } catch (err) {
            // On fetch failure or offline state, cleanly hide the card
            hideCard();
        }
    }

    function hideCard() {
        if (countdownInterval) {
            clearInterval(countdownInterval);
            countdownInterval = null;
        }
        currentEndTime = null;
        card.style.display = "none";
        if (hero) hero.classList.add("hero--no-live-lot");
    }

    function renderCard(lot) {
        if (hero) hero.classList.remove("hero--no-live-lot");
        card.style.display = ""; // restore natural CSS card display

        // Lot badge
        const lotBadge = document.getElementById("heroLotId");
        if (lotBadge) lotBadge.textContent = "#LOT-" + lot.auctionId;

        // Title
        const title = document.getElementById("heroLotTitle");
        if (title) title.textContent = lot.productName || "Live Auction Lot";

        // Subtitle (Category or short description)
        const subtitle = document.getElementById("heroLotSubtitle");
        if (subtitle) {
            subtitle.textContent = lot.category || lot.description || "";
        }

        // Product Image (if available)
        const imgWrapper = document.getElementById("heroLotImageWrapper");
        const img = document.getElementById("heroLotImage");
        if (imgWrapper && img) {
            if (lot.imagePath && lot.imagePath.trim() !== "") {
                img.src = lot.imagePath;
                img.alt = lot.productName || "Live Lot";
                imgWrapper.style.display = "block";
            } else {
                imgWrapper.style.display = "none";
            }
        }

        // Starting Reserve
        const reserve = document.getElementById("heroStartingReserve");
        if (reserve) {
            const startVal = Number(lot.startingPrice);
            reserve.textContent = isNaN(startVal) ? ("₹" + lot.startingPrice) : ("₹" + startVal.toLocaleString("en-IN"));
        }

        // Current Leading Bid
        const currentBid = document.getElementById("heroCurrentBid");
        if (currentBid) {
            const bidVal = Number(lot.currentBid);
            currentBid.textContent = isNaN(bidVal) ? ("₹" + lot.currentBid) : ("₹" + bidVal.toLocaleString("en-IN"));
        }

        // Start dynamic countdown
        currentEndTime = Number(lot.endTime);
        startCountdown();
    }

    function startCountdown() {
        if (countdownInterval) clearInterval(countdownInterval);

        function updateTimer() {
            const timerEl = document.getElementById("auctionTimer");
            if (!timerEl || !currentEndTime) return;

            const now = Date.now();
            const diffSeconds = Math.floor((currentEndTime - now) / 1000);

            if (diffSeconds <= 0) {
                timerEl.textContent = "Auction Ended";
                timerEl.classList.add("urgent");
                clearInterval(countdownInterval);
                countdownInterval = null;
                // Immediately poll the server to fetch the next lot or hide the card
                setTimeout(fetchFeaturedAuction, 1200);
                return;
            }

            timerEl.classList.remove("urgent");
            const days = Math.floor(diffSeconds / 86400);
            const hours = Math.floor((diffSeconds % 86400) / 3600);
            const minutes = Math.floor((diffSeconds % 3600) / 60);
            const seconds = diffSeconds % 60;

            const pad = (n) => String(n).padStart(2, "0");

            if (days > 0) {
                timerEl.textContent = "Ends in " + days + "d " + pad(hours) + "h " + pad(minutes) + "m";
            } else if (hours > 0) {
                timerEl.textContent = "Ends in " + pad(hours) + ":" + pad(minutes) + ":" + pad(seconds);
            } else {
                timerEl.textContent = "Ends in " + pad(minutes) + ":" + pad(seconds);
            }
        }

        updateTimer();
        countdownInterval = setInterval(updateTimer, 1000);
    }

    // Initial fetch on page load
    fetchFeaturedAuction();

    // Periodic refresh every 12 seconds to catch new bids, price changes, or lot transitions
    pollInterval = setInterval(fetchFeaturedAuction, 12000);
})();

// 3. Image Upload Preview (Component 5)
function initHashbidImagePreview(inputId, previewId, helpId) {
    const input = document.getElementById(inputId || 'productImageInput');
    const preview = document.getElementById(previewId || 'productImagePreview');
    const help = document.getElementById(helpId || 'productImageHelp');
    if (!input || !preview) return;

    input.addEventListener('change', () => {
        const file = input.files && input.files[0];
        preview.hidden = true;
        preview.removeAttribute('src');
        if (!file) return;

        const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
        const maxBytes = 5 * 1024 * 1024; // 5 MB per servlet @MultipartConfig

        if (!allowedTypes.includes(file.type)) {
            if (help) help.textContent = 'Please choose a supported image (JPG, PNG, WebP).';
            input.value = '';
            return;
        }

        if (file.size > maxBytes) {
            if (help) help.textContent = 'Selected image exceeds the 5MB file size limit.';
            input.value = '';
            return;
        }

        const reader = new FileReader();
        reader.addEventListener('load', () => {
            preview.src = String(reader.result);
            preview.hidden = false;
            if (help) help.textContent = file.name + ' (' + Math.round(file.size / 1024) + ' KB)';
        });
        reader.addEventListener('error', () => {
            if (help) help.textContent = 'Preview could not be rendered.';
        });
        reader.readAsDataURL(file);
    });
}
// Backward compatibility alias for legacy scripts
const initBidSphereImagePreview = initHashbidImagePreview;

// 4. In-DOM Table & Card Quick Search Filter (Component 9)
function initHashbidFilter(inputId, itemSelector, statusId) {
    const input = document.getElementById(inputId);
    const status = document.getElementById(statusId);
    if (!input) return;

    const items = [...document.querySelectorAll(itemSelector || '[data-bs-filter-item]')];
    input.addEventListener('input', () => {
        const query = input.value.trim().toLowerCase();
        let visible = 0;

        items.forEach((item) => {
            const matches = item.textContent.toLowerCase().includes(query);
            item.style.display = matches ? '' : 'none';
            if (matches) visible++;
        });

        if (status) {
            status.textContent = `${visible} matching item${visible === 1 ? '' : 's'}`;
        }
    });
}
// Backward compatibility alias
const initBidSphereFilter = initHashbidFilter;

// 5. Global Toast Notification Utility (Component 7)
function showHashbidToast(message, type = 'info') {
    let region = document.getElementById('toast');
    if (!region) {
        region = document.createElement('div');
        region.id = 'toast';
        region.className = 'toast-notification';
        document.body.appendChild(region);
    }

    region.textContent = (type === 'success' ? '✓ ' : type === 'error' ? '⚠ ' : 'ℹ ') + String(message);
    region.className = 'toast-notification toast-' + type + ' toast-visible';

    window.setTimeout(() => {
        region.classList.remove('toast-visible');
    }, 4000);
}
// Backward compatibility alias
const showBidSphereToast = showHashbidToast;

// Auto-run when DOM is interactive
document.addEventListener('DOMContentLoaded', () => {
    initHashbidImagePreview('productImageInput', 'productImagePreview', 'productImageHelp');
});
