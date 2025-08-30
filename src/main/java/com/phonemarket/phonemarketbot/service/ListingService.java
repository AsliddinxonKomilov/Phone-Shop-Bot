package com.phonemarket.phonemarketbot.service;

import com.phonemarket.phonemarketbot.model.Brand;
import com.phonemarket.phonemarketbot.model.Listing;
import com.phonemarket.phonemarketbot.model.ListingStatus;
import com.phonemarket.phonemarketbot.repo.ListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ListingService {

    private final ListingRepository listingRepository;

    public ListingService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    /** E'lonni saqlash (yangi yoki yangilash) */
    public Listing save(Listing l) {
        return listingRepository.save(l);
    }

    /** Brend bo'yicha ACTIVElarni sahifalab olish (katalog ko'rinishida) */
    public Page<Listing> findActiveByBrand(Brand brand, int page, int size) {
        return listingRepository.findByStatusAndBrandOrderByCreatedAtDesc(
                ListingStatus.ACTIVE, brand, PageRequest.of(page, size));
    }

    /** ID bo'yicha topish */
    public Optional<Listing> findById(Long id) {
        return listingRepository.findById(id);
    }

    /** Foydalanuvchining o'z e'lonlari (statusidan qat'i nazar), eng yangidan */
    public Page<Listing> findByOwner(long ownerId, int page, int size) {
        return listingRepository.findByOwnerIdOrderByCreatedAtDesc(
                ownerId, PageRequest.of(page, size));
    }

    /** Egasi o'zi bo'lsa — e'lonni SOLD ga o'tkazish */
    @Transactional
    public boolean markSold(Long listingId, long ownerId) {
        Optional<Listing> opt = listingRepository.findById(listingId);
        if (opt.isEmpty()) return false;
        Listing l = opt.get();
        if (l.getOwner() == null || l.getOwner().getId() == null || l.getOwner().getId() != ownerId) return false;
        l.setStatus(ListingStatus.SOLD);
        // save() shart emas, lekin aniq bo‘lishi uchun qoldiramiz:
        listingRepository.save(l);
        return true;
    }

    /** Egasi o'zi bo'lsa — soft delete (DELETED holatiga o'tkazish) */
    @Transactional
    public boolean softDelete(Long listingId, long ownerId) {
        Optional<Listing> opt = listingRepository.findById(listingId);
        if (opt.isEmpty()) return false;
        Listing l = opt.get();
        if (l.getOwner() == null || l.getOwner().getId() == null || l.getOwner().getId() != ownerId) return false;
        l.setStatus(ListingStatus.DELETED);
        listingRepository.save(l);
        return true;
    }
}