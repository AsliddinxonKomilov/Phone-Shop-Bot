package com.phonemarket.phonemarketbot.repo;

import com.phonemarket.phonemarketbot.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {}