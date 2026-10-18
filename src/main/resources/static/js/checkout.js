(() => {
    const provinceSelect = document.getElementById("province");
    const wardSelect = document.getElementById("ward");
    const status = document.getElementById("location-status");
    const retryButton = document.getElementById("location-retry");

    if (!provinceSelect || !wardSelect || !status || !retryButton) return;

    class ProvinceApiAdapter {
        constructor(baseUrl) {
            this.baseUrl = baseUrl;
        }

        async request(path) {
            const response = await fetch(`${this.baseUrl}${path}`);
            if (!response.ok) {
                throw new Error(`API địa giới trả về HTTP ${response.status}.`);
            }
            return response.json();
        }

        getProvinces() {
            return this.request("/p/");
        }

        async getWards(provinceCode) {
            const province = await this.request(`/p/${encodeURIComponent(provinceCode)}?depth=2`);
            if (!Array.isArray(province.wards)) {
                throw new Error("Dữ liệu phường, xã không đúng định dạng.");
            }
            return province.wards;
        }
    }

    class LocationSelectionStore {
        constructor() {
            this.state = { province: null, ward: null };
            this.observers = new Set();
        }

        subscribe(observer) {
            this.observers.add(observer);
            return () => this.observers.delete(observer);
        }

        selectProvince(province) {
            this.state = { province, ward: null };
            this.notify("province-changed");
        }

        selectWard(ward) {
            this.state = { ...this.state, ward };
            this.notify("ward-changed");
        }

        notify(type) {
            this.observers.forEach((observer) => observer({ type, ...this.state }));
        }
    }

    const api = new ProvinceApiAdapter("https://provinces.open-api.vn/api/v2");
    const selection = new LocationSelectionStore();
    const selectedProvinceName = provinceSelect.dataset.selected;
    let selectedWardName = wardSelect.dataset.selected;
    let provincesLoaded = false;
    let wardRequestVersion = 0;

    function replaceOptions(select, placeholder, items) {
        select.replaceChildren(new Option(placeholder, ""));
        items.forEach((item) => {
            const option = new Option(item.name, item.name);
            option.dataset.code = item.code;
            select.add(option);
        });
    }

    async function loadWards(province) {
        const requestVersion = ++wardRequestVersion;
        wardSelect.disabled = true;
        replaceOptions(wardSelect, "Đang tải phường / xã...", []);
        status.textContent = `Đang tải phường / xã thuộc ${province.name}...`;
        retryButton.hidden = true;

        try {
            const wards = await api.getWards(province.code);
            if (requestVersion !== wardRequestVersion) return;
            replaceOptions(wardSelect, "Chọn phường / xã", wards);
            wardSelect.disabled = wards.length === 0;
            if (selectedWardName) {
                wardSelect.value = selectedWardName;
                selectedWardName = "";
            }
            status.textContent = wards.length
                ? "Chọn phường / xã và nhập số nhà, tên đường."
                : "API chưa có dữ liệu phường / xã cho địa phương này.";
            if (wards.length) selection.selectWard(wardSelect.value || null);
        } catch (error) {
            if (requestVersion !== wardRequestVersion) return;
            wardSelect.disabled = true;
            replaceOptions(wardSelect, "Không tải được phường / xã", []);
            status.textContent = `Không tải được danh sách phường / xã: ${error.message}`;
            retryButton.hidden = false;
        }
    }

    selection.subscribe((event) => {
        if (event.type === "province-changed" && event.province) {
            loadWards(event.province);
        }
    });

    async function loadProvinces() {
        provinceSelect.disabled = true;
        replaceOptions(provinceSelect, "Đang tải danh sách...", []);
        status.textContent = "Đang tải dữ liệu Tỉnh / Thành phố...";
        retryButton.hidden = true;

        try {
            const provinces = await api.getProvinces();
            if (!Array.isArray(provinces)) {
                throw new Error("Dữ liệu Tỉnh / Thành phố không đúng định dạng.");
            }
            replaceOptions(provinceSelect, "Chọn Tỉnh / Thành phố", provinces);
            provinceSelect.disabled = false;
            provincesLoaded = true;

            if (selectedProvinceName) {
                provinceSelect.value = selectedProvinceName;
            }
            const selectedOption = provinceSelect.selectedOptions[0];
            if (selectedOption?.dataset.code) {
                selection.selectProvince({ name: selectedOption.value, code: selectedOption.dataset.code });
            } else {
                status.textContent = "Chọn Tỉnh / Thành phố để tải danh sách Phường / Xã.";
            }
        } catch (error) {
            status.textContent = `Không tải được danh sách Tỉnh / Thành phố: ${error.message}`;
            retryButton.hidden = false;
        }
    }

    provinceSelect.addEventListener("change", () => {
        selectedWardName = "";
        const selectedOption = provinceSelect.selectedOptions[0];
        wardRequestVersion++;
        wardSelect.disabled = true;
        replaceOptions(wardSelect, "Chọn tỉnh / thành phố trước", []);
        if (selectedOption?.dataset.code) {
            selection.selectProvince({ name: selectedOption.value, code: selectedOption.dataset.code });
        } else {
            selection.state = { province: null, ward: null };
            status.textContent = "Chọn Tỉnh / Thành phố để tải danh sách Phường / Xã.";
        }
    });

    wardSelect.addEventListener("change", () => {
        selection.selectWard(wardSelect.value || null);
    });

    retryButton.addEventListener("click", () => {
        if (!provincesLoaded) {
            loadProvinces();
            return;
        }
        if (selection.state.province) {
            loadWards(selection.state.province);
        }
    });

    loadProvinces();
})();
