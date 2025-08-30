package com.phonemarket.phonemarketbot.service;

import com.phonemarket.phonemarketbot.model.Brand;
import com.phonemarket.phonemarketbot.model.ListingStatus;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class KeyboardFactory {

    /** Asosiy menyu */
    public ReplyKeyboardMarkup mainMenu() {
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Yangi e'lon"));
        row1.add(new KeyboardButton("Sotib olaman"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Mening e'lonlarim"));

        rows.add(row1);
        rows.add(row2);

        kb.setKeyboard(rows);
        return kb;
    }

    /** Brend tanlash inline tugmalari (prefix: NEW_BRAND yoki BROWSE_BRAND) */
    public InlineKeyboardMarkup brandChooser(String prefix) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Brand b : Brand.values()) {
            InlineKeyboardButton btn = new InlineKeyboardButton(b.uz());
            btn.setCallbackData(prefix + ":" + b.name());
            rows.add(Arrays.asList(btn));
        }
        InlineKeyboardMarkup mk = new InlineKeyboardMarkup();
        mk.setKeyboard(rows);
        return mk;
    }

    /** Katalog ko‘rish uchun pager (brend bo‘yicha qidirishda) */
    public InlineKeyboardMarkup pager(String actionPrefix, String brand, int page, boolean hasPrev, boolean hasNext) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        if (hasPrev) {
            InlineKeyboardButton prev = new InlineKeyboardButton("⬅️ Oldingi");
            prev.setCallbackData(actionPrefix + ":" + brand + ":" + (page - 1));
            row.add(prev);
        }
        if (hasNext) {
            InlineKeyboardButton next = new InlineKeyboardButton("Keyingi ➡️");
            next.setCallbackData(actionPrefix + ":" + brand + ":" + (page + 1));
            row.add(next);
        }
        if (!row.isEmpty()) rows.add(row);

        InlineKeyboardMarkup mk = new InlineKeyboardMarkup();
        mk.setKeyboard(rows);
        return mk;
    }

    /** Mening e’lonlarim — pager (sahifalash) */
    public InlineKeyboardMarkup myPager(int page, boolean hasPrev, boolean hasNext) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        if (hasPrev) {
            InlineKeyboardButton prev = new InlineKeyboardButton("⬅️ Oldingi");
            prev.setCallbackData("MY_PAGE:" + (page - 1));
            row.add(prev);
        }
        if (hasNext) {
            InlineKeyboardButton next = new InlineKeyboardButton("Keyingi ➡️");
            next.setCallbackData("MY_PAGE:" + (page + 1));
            row.add(next);
        }
        if (!row.isEmpty()) rows.add(row);

        InlineKeyboardMarkup mk = new InlineKeyboardMarkup();
        mk.setKeyboard(rows);
        return mk;
    }

    /** Mening e’lonlarim — har bir e’lon uchun amallar */
    public InlineKeyboardMarkup myListingActions(long id, ListingStatus status) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton detail = new InlineKeyboardButton("🔎 Tafsilot");
        detail.setCallbackData("MY_DETAIL:" + id);
        rows.add(Arrays.asList(detail));
        if (status == ListingStatus.ACTIVE) {
            InlineKeyboardButton sold = new InlineKeyboardButton("✅ Sotildi");
            sold.setCallbackData("MY_SOLD:" + id);
            rows.add(Arrays.asList(sold));
        }

        InlineKeyboardButton del = new InlineKeyboardButton("🗑 O'chirish");
        del.setCallbackData("MY_DELETE:" + id);
        rows.add(Arrays.asList(del));

        InlineKeyboardMarkup mk = new InlineKeyboardMarkup();
        mk.setKeyboard(rows);
        return mk;
    }
}