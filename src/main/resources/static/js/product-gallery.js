(() => {
    const mainImage = document.getElementById("product-main-image");
    const thumbnails = document.querySelectorAll(".detail-thumbnail");

    if (!mainImage || thumbnails.length < 2) return;

    thumbnails.forEach((thumbnail) => {
        thumbnail.addEventListener("click", () => {
            const imageUrl = thumbnail.dataset.image;
            if (!imageUrl) return;
            mainImage.src = imageUrl;
            thumbnails.forEach((item) => {
                const active = item === thumbnail;
                item.classList.toggle("is-active", active);
                item.setAttribute("aria-pressed", String(active));
            });
        });
    });
})();
