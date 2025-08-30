package com.phonemarket.phonemarketbot.bot;

import com.phonemarket.phonemarketbot.config.BotConfig;
import com.phonemarket.phonemarketbot.dto.DraftListing;
import com.phonemarket.phonemarketbot.model.*;
import com.phonemarket.phonemarketbot.repo.AppUserRepository;
import com.phonemarket.phonemarketbot.service.KeyboardFactory;
import com.phonemarket.phonemarketbot.service.ListingService;
import com.phonemarket.phonemarketbot.service.SessionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class PhoneMarketBot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(PhoneMarketBot.class);

    private final BotConfig cfg;
    private final KeyboardFactory kb;
    private final ListingService listingService;
    private final AppUserRepository userRepo;
    private final SessionService sessions;

    public PhoneMarketBot(BotConfig cfg, KeyboardFactory kb, ListingService listingService, AppUserRepository userRepo, SessionService sessions) {
        super(cfg.getToken());
        this.cfg = cfg;
        this.kb = kb;
        this.listingService = listingService;
        this.userRepo = userRepo;
        this.sessions = sessions;
    }

    @PostConstruct
    public void register() throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(this);
        log.info("Bot registered as @{}", getBotUsername());
    }

    @Override public String getBotUsername() { return cfg.getUsername(); }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) handleMessage(update.getMessage());
            else if (update.hasCallbackQuery()) handleCallback(update.getCallbackQuery());
        } catch (Exception e) {
            log.error("Error handling update", e);
        }
    }

    private void handleMessage(Message msg) throws TelegramApiException {
        long chatId = msg.getChatId();
        long userId = msg.getFrom().getId();
        ensureUser(msg.getFrom());

        if (msg.hasText()) {
            String text = msg.getText().trim();
            switch (text) {
                case "/start" -> sendMainMenu(chatId, "Assalomu alaykum! Phone Market botga xush kelibsiz.");
                case "Yangi e'lon" -> {
                    sessions.setState(userId, SessionService.State.NEW_WAIT_BRAND);
                    execute(SendMessage.builder().chatId(chatId)
                            .text("Brendni tanlang:")
                            .replyMarkup(kb.brandChooser("NEW_BRAND"))
                            .build());
                }
                case "Sotib olaman" -> {
                    sessions.setState(userId, SessionService.State.BROWSE_WAIT_BRAND);
                    execute(SendMessage.builder().chatId(chatId)
                            .text("Qaysi brendni qidiramiz?")
                            .replyMarkup(kb.brandChooser("BROWSE_BRAND"))
                            .build());
                }
                case "Mening e'lonlarim" -> { // ✅ YANGI: shaxsiy e’lonlar
                    sendMyListingsPage(chatId, userId, 0);
                }
                default -> handleTextAsState(chatId, userId, text);
            }
        } else if (msg.hasPhoto()) {
            handlePhoto(chatId, userId, msg.getPhoto());
        } else {
            execute(SendMessage.builder().chatId(chatId).text("Noto'g'ri format. Matn yoki rasm yuboring.").build());
        }
    }

    private void handleTextAsState(long chatId, long userId, String text) throws TelegramApiException {
        SessionService.State st = sessions.getState(userId);
        DraftListing d = sessions.draft(userId);

        switch (st) {
            case NEW_WAIT_MODEL -> {
                d.setModel(text);
                sessions.setState(userId, SessionService.State.NEW_WAIT_PRICE);
                execute(SendMessage.builder().chatId(chatId).text("Narxni kiriting (so'm, faqat raqam).").build());
            }
            case NEW_WAIT_PRICE -> {
                try {
                    String digits = text.replaceAll("[^0-9]", "");
                    d.setPrice(Long.parseLong(digits));
                    sessions.setState(userId, SessionService.State.NEW_WAIT_DESCRIPTION);
                    execute(SendMessage.builder().chatId(chatId).text("Qisqacha tavsif yuboring.").build());
                } catch (Exception e) {
                    execute(SendMessage.builder().chatId(chatId).text("Iltimos, faqat raqam kiriting. Masalan: 3500000").build());
                }
            }
            case NEW_WAIT_DESCRIPTION -> {
                d.setDescription(text);
                sessions.setState(userId, SessionService.State.NEW_WAIT_PHOTO);
                execute(SendMessage.builder().chatId(chatId).text("Endi telefon rasmini yuboring.").build());
            }
            default -> sendMainMenu(chatId, "Menyu:");
        }
    }

    private void handlePhoto(long chatId, long userId, List<PhotoSize> photos) throws TelegramApiException {
        SessionService.State st = sessions.getState(userId);
        if (st != SessionService.State.NEW_WAIT_PHOTO) {
            execute(SendMessage.builder().chatId(chatId).text("Rasm hozir kerak emas 🙂").build());
            return;
        }
        PhotoSize best = photos.stream().max(Comparator.comparing(PhotoSize::getFileSize)).orElse(photos.get(0));
        String fileId = best.getFileId();
        DraftListing d = sessions.draft(userId);
        d.setPhotoFileId(fileId);

        if (d.isComplete()) {
            Listing l = new Listing();
            l.setBrand(d.getBrand());
            l.setModel(d.getModel());
            l.setPrice(d.getPrice());
            l.setDescription(d.getDescription());
            l.setPhotoFileId(d.getPhotoFileId());
            l.setStatus(ListingStatus.ACTIVE);
            AppUser owner = new AppUser();
            owner.setId(userId);
            l.setOwner(owner);
            listingService.save(l);
            sessions.reset(userId);

            execute(SendMessage.builder().chatId(chatId).text("✅ E'lon saqlandi! Rahmat.").replyMarkup(kb.mainMenu()).build());
        } else {
            execute(SendMessage.builder().chatId(chatId).text("Nimadir yetishmayapti, qayta urinib ko'ring.").build());
        }
    }

    private void handleCallback(CallbackQuery cb) throws TelegramApiException {
        String data = cb.getData();
        long chatId = cb.getMessage().getChatId();
        long userId = cb.getFrom().getId();

        if (data.startsWith("NEW_BRAND:")) {
            String brandStr = data.substring("NEW_BRAND:".length());
            Brand b = Brand.fromString(brandStr);
            DraftListing d = sessions.draft(userId);
            d.setBrand(b);
            sessions.setState(userId, SessionService.State.NEW_WAIT_MODEL);
            execute(SendMessage.builder().chatId(chatId).text("Modelni kiriting (masalan, iPhone 13, Galaxy S21, va hokazo).").build());
            return;
        }
        if (data.startsWith("BROWSE_BRAND:")) {
            String brandStr = data.substring("BROWSE_BRAND:".length());
            sessions.setBrowsingBrand(userId, brandStr);
            sessions.setPage(userId, 0);
            sendBrowsePage(chatId, brandStr, 0);
            return;
        }
        if (data.startsWith("PAGE:")) {
            String[] parts = data.split(":");
            String brandStr = parts[1];
            int page = Integer.parseInt(parts[2]);
            sessions.setPage(userId, page);
            sendBrowsePage(chatId, brandStr, page);
            return;
        }
        if (data.startsWith("DETAIL:")) {
            Long id = Long.parseLong(data.substring("DETAIL:".length()));
            Optional<Listing> opt = listingService.findById(id);
            if (opt.isEmpty()) {
                execute(SendMessage.builder().chatId(chatId).text("E'lon topilmadi.").build());
                return;
            }
            Listing l = opt.get();
            String caption =
                    "📱 " + l.getBrand().uz() + " " + l.getModel() + "\n" +
                            "💰 " + l.getPrice() + " so'm\n" +
                            "📝 " + l.getDescription();

            SendPhoto sp = new SendPhoto();
            sp.setChatId(chatId);
            sp.setPhoto(new InputFile(l.getPhotoFileId()));
            sp.setCaption(caption);
            execute(sp);
            return;
        }

        // =======================
        // ✅ YANGI: "Mening e'lonlarim" callback’lari
        // =======================
        if (data.startsWith("MY_PAGE:")) {
            int page = Integer.parseInt(data.substring("MY_PAGE:".length()));
            sendMyListingsPage(chatId, userId, page);
            return;
        }
        if (data.startsWith("MY_DETAIL:")) {
            Long id = Long.parseLong(data.substring("MY_DETAIL:".length()));
            Optional<Listing> opt = listingService.findById(id);
            if (opt.isEmpty()) {
                execute(SendMessage.builder().chatId(chatId).text("E'lon topilmadi.").build());
                return;
            }
            Listing l = opt.get();
            String caption = "📱 " + l.getBrand().uz() + " " + l.getModel() + "\n" +
                    "💰 " + l.getPrice() + " so'm\n" +
                    "📝 " + l.getDescription() + "\n" +
                    "📌 Status: " + l.getStatus();
            SendPhoto sp = new SendPhoto();
            sp.setChatId(chatId);
            sp.setPhoto(new InputFile(l.getPhotoFileId()));
            sp.setCaption(caption);
            execute(sp);
            return;
        }
        if (data.startsWith("MY_SOLD:")) {
            Long id = Long.parseLong(data.substring("MY_SOLD:".length()));
            boolean ok = listingService.markSold(id, userId);
            execute(SendMessage.builder().chatId(chatId)
                    .text(ok ? "✅ E'lon #"+id+" sotildi deb belgilandi." : "❌ Amal bajarilmadi.")
                    .build());
            sendMyListingsPage(chatId, userId, 0);
            return;
        }
        if (data.startsWith("MY_DELETE:")) {
            Long id = Long.parseLong(data.substring("MY_DELETE:".length()));
            boolean ok = listingService.softDelete(id, userId);
            execute(SendMessage.builder().chatId(chatId)
                    .text(ok ? "🗑 E'lon #"+id+" o'chirildi." : "❌ Amal bajarilmadi.")
                    .build());
            sendMyListingsPage(chatId, userId, 0);
        }
    }
    private void sendBrowsePage(long chatId, String brandStr, int page) throws TelegramApiException {
        Brand brand = Brand.fromString(brandStr);
        var p = listingService.findActiveByBrand(brand, page, 5);
        if (p.getTotalElements() == 0) {
            execute(SendMessage.builder().chatId(chatId).text("Bu brend bo'yicha e'lonlar topilmadi.").build());
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("🔎 ").append(brand.uz()).append(" bo'yicha e'lonlar (")
                .append(page + 1).append("/").append(p.getTotalPages()).append(")\n\n");
        for (Listing l : p.getContent()) {
            sb.append("• #").append(l.getId()).append(" — ")
                    .append(l.getModel()).append(", ").append(l.getPrice()).append(" so'm\n");
        }
        InlineKeyboardMarkup pager = kb.pager("PAGE", brand.name(), page, page > 0, page + 1 < p.getTotalPages());
        execute(SendMessage.builder().chatId(chatId).text(sb.toString()).replyMarkup(pager).build());

        for (Listing l : p.getContent()) {
            InlineKeyboardButton btn = new InlineKeyboardButton("🔎 #" + l.getId() + " tafsilot");
            btn.setCallbackData("DETAIL:" + l.getId());
            InlineKeyboardMarkup mk = new InlineKeyboardMarkup(java.util.List.of(java.util.List.of(btn)));
            execute(SendMessage.builder().chatId(chatId).text("E'lon #" + l.getId()).replyMarkup(mk).build());
        }
    }

    // ✅ YANGI: “Mening e’lonlarim” sahifasi + tugmalar
    private void sendMyListingsPage(long chatId, long userId, int page) throws TelegramApiException {
        var p = listingService.findByOwner(userId, page, 5);
        if (p.getTotalElements() == 0) {
            execute(SendMessage.builder().chatId(chatId)
                    .text("Sizda hali e'lon yo'q. \"Yangi e'lon\" orqali qo'shing.")
                    .replyMarkup(kb.mainMenu())
                    .build());
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("👤 Mening e'lonlarim (")
                .append(page + 1).append("/").append(p.getTotalPages()).append(")\n\n");
        p.getContent().forEach(l -> sb.append("• #").append(l.getId())
                .append(" — ").append(l.getBrand().uz()).append(" ").append(l.getModel())
                .append(" — ").append(l.getPrice()).append(" so'm")
                .append(" [").append(l.getStatus()).append("]\n"));

        // pager xabari
        execute(SendMessage.builder().chatId(chatId)
                .text(sb.toString())
                .replyMarkup(kb.myPager(page, page > 0, page + 1 < p.getTotalPages()))
                .build());

        // har bir e’lon uchun amallar
        for (var l : p.getContent()) {
            execute(SendMessage.builder().chatId(chatId)
                    .text("E'lon #" + l.getId())
                    .replyMarkup(kb.myListingActions(l.getId(), l.getStatus()))
                    .build());
        }
    }

    private void sendMainMenu(long chatId, String text) throws TelegramApiException {
        ReplyKeyboardMarkup menu = kb.mainMenu();
        execute(SendMessage.builder().chatId(chatId).text(text).replyMarkup(menu).build());
    }

    private void ensureUser(User u) {
        AppUser entity = userRepo.findById(u.getId()).orElseGet(() -> new AppUser(u.getId()));
        entity.setUsername(u.getUserName());
        entity.setFirstName(u.getFirstName());
        entity.setLastName(u.getLastName());
        userRepo.save(entity);
    }
}