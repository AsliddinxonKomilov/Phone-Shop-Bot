package com.phoneshop.phoneshop_bot;

import com.phoneshop.phoneshop_bot.bot.TelegramBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class PhoneshopBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhoneshopBotApplication.class, args);
	}
@Bean
	public TelegramBotsApi
	telegramBotsApi(TelegramBot telegramBot)
	throws TelegramApiException{
		TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
		botsApi.registerBot(telegramBot);
		return botsApi;
    }
}