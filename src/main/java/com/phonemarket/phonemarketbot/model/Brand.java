package com.phonemarket.phonemarketbot.model;

public enum Brand {
    APPLE, SAMSUNG, XIAOMI, HUAWEI, REALME, ONEPLUS, GOOGLE, NOKIA, SONY, OTHER;

    public static Brand fromString(String s) {
        try { return Brand.valueOf(s); } catch (Exception e) { return OTHER; }
    }
    public String uz() {
        return switch (this) {
            case APPLE -> "Apple";
            case SAMSUNG -> "Samsung";
            case XIAOMI -> "Xiaomi";
            case HUAWEI -> "Huawei";
            case REALME -> "realme";
            case ONEPLUS -> "OnePlus";
            case GOOGLE -> "Google Pixel";
            case NOKIA -> "Nokia";
            case SONY -> "Sony";
            case OTHER -> "Boshqa";
        };
    }
}