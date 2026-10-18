(() => {
    const input = document.getElementById("product-images");
    const preview = document.getElementById("selected-images-preview");
    const list = document.getElementById("selected-images-list");

    if (!input || !preview || !list) return;

    let previewUrls = [];

    input.addEventListener("change", () => {
        previewUrls.forEach((url) => URL.revokeObjectURL(url));
        previewUrls = [];
        list.replaceChildren();

        const files = Array.from(input.files || []);
        preview.hidden = files.length === 0;
        if (files.length > 10) {
            input.setCustomValidity("Chỉ được chọn tối đa 10 ảnh mỗi lần.");
            input.reportValidity();
        } else {
            input.setCustomValidity("");
        }

        files.forEach((file) => {
            const image = document.createElement("img");
            image.alt = file.name;
            if (!["image/jpeg", "image/png", "image/webp"].includes(file.type) || file.size > 4_000_000) {
                image.classList.add("is-invalid");
                image.title = "Chỉ chấp nhận JPG, PNG, WEBP nhỏ hơn 4 MB.";
            } else {
                const url = URL.createObjectURL(file);
                previewUrls.push(url);
                image.src = url;
                image.title = file.name;
            }
            list.append(image);
        });
    });

    window.addEventListener("pagehide", () => {
        previewUrls.forEach((url) => URL.revokeObjectURL(url));
    });
})();
