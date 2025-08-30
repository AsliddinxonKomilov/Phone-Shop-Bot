package com.phonemarket.phonemarketbot.repo;

import com.phonemarket.phonemarketbot.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    Page<Listing> findByStatusAndBrandOrderByCreatedAtDesc(ListingStatus status, Brand brand, Pageable pageable);
    Page<Listing> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
}