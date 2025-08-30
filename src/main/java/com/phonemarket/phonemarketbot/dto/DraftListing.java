package com.phonemarket.phonemarketbot.dto;

import com.phonemarket.phonemarketbot.model.Brand;

public class DraftListing {
    private Brand brand;
    private String model;
    private Long price;
    private String description;
    private String photoFileId;

    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPhotoFileId() { return photoFileId; }
    public void setPhotoFileId(String photoFileId) { this.photoFileId = photoFileId; }

    public boolean isComplete() {
        return brand != null && model != null && price != null && description != null && photoFileId != null;
    }
    public void clear() {
        brand = null; model = null; price = null; description = null; photoFileId = null;
    }
}