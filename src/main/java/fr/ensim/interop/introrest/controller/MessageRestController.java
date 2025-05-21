package fr.ensim.interop.introrest.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageRestController {

	@Value("${telegram.api.url}")
	private String telegramApiUrl;

	@Value("${telegram.token}")
	private String botToken;

	private final RestTemplate restTemplate = new RestTemplate();

	@PostMapping
	public ResponseEntity<?> sendMessage(@RequestBody MessageRequest request) {
		String url = telegramApiUrl + botToken + "/sendMessage";

		Map<String, String> body = new HashMap<>();
		body.put("chat_id", request.getChatId());
		body.put("text", request.getText());

		try {
			ResponseEntity<String> response = restTemplate.postForEntity(url, body, String.class);
			return ResponseEntity.ok(response.getBody());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur : " + e.getMessage());
		}
	}

	public static class MessageRequest {
		private String chatId;
		private String text;

		public String getChatId() {
			return chatId;
		}

		public void setChatId(String chatId) {
			this.chatId = chatId;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}

