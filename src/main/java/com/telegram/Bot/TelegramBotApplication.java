package com.telegram.Bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TelegramBotApplication {


	/*iwr "https://api.telegram.org/bot8006329268:AAE06Jw-NAEsotbhTfjywZDXobbWlK_XC2Q/setWebhook" `
  -Method POST `
  -Body @{ url = "https://bitfetch.onrender.com/webhook/very_secret_path" }
*/

	public static void main(String[] args) {
		SpringApplication.run(TelegramBotApplication.class, args);
	}

}
