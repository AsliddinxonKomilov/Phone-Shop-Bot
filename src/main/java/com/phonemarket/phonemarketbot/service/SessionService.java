package com.phonemarket.phonemarketbot.service;

import com.phonemarket.phonemarketbot.dto.DraftListing;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    public enum State {
        IDLE,
        NEW_WAIT_BRAND,
        NEW_WAIT_MODEL,
        NEW_WAIT_PRICE,
        NEW_WAIT_DESCRIPTION,
        NEW_WAIT_PHOTO,
        BROWSE_WAIT_BRAND
    }

    private static class Session {
        State state = State.IDLE;
        DraftListing draft = new DraftListing();
        int page = 0;
        String browsingBrand = null;
    }

    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    public State getState(long userId) {
        return sessions.computeIfAbsent(userId, id -> new Session()).state;
    }
    public void setState(long userId, State state) {
        sessions.computeIfAbsent(userId, id -> new Session()).state = state;
    }
    public DraftListing draft(long userId) {
        return sessions.computeIfAbsent(userId, id -> new Session()).draft;
    }
    public void reset(long userId) {
        sessions.computeIfAbsent(userId, id -> new Session()).draft.clear();
        setState(userId, State.IDLE);
    }

    public void setBrowsingBrand(long userId, String brand) {
        sessions.computeIfAbsent(userId, id -> new Session()).browsingBrand = brand;
    }
    public String getBrowsingBrand(long userId) {
        return sessions.computeIfAbsent(userId, id -> new Session()).browsingBrand;
    }

    public int getPage(long userId) {
        return sessions.computeIfAbsent(userId, id -> new Session()).page;
    }
    public void setPage(long userId, int page) {
        sessions.computeIfAbsent(userId, id -> new Session()).page = page;
    }
}