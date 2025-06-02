package fr.ensim.interop.introrest.z;

import fr.ensim.interop.introrest.model.telegram.ApiResponseUpdateTelegram;
import fr.ensim.interop.introrest.model.telegram.Message;
import fr.ensim.interop.introrest.model.telegram.Update;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class ListenerUpdateTelegram {

	@Value("${telegram.api.url}")
	private String telegramApiUrl;

	@Value("${telegram.token}")
	private String botToken;

	private final Map<String, String> chatStates = new HashMap<>();

	private final RestTemplate restTemplate = new RestTemplate();
	private int lastUpdateId = 0;

	@PostConstruct
	public void startPolling() {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				String baseUrl = telegramApiUrl + botToken;
				String url = baseUrl + "/getUpdates?offset=" + (lastUpdateId + 1);

				try {
					ApiResponseUpdateTelegram response = restTemplate.getForObject(url, ApiResponseUpdateTelegram.class);

					if (response != null && response.getResult() != null) {
						for (Update update : response.getResult()) {
							Message message = update.getMessage();
							String text = message.getText();
							String chatId = String.valueOf(message.getChat().getId());

							if (text != null) {

								if (chatStates.containsKey(chatId) && chatStates.get(chatId).equals("awaiting_movie_genre")) {
									String movieResult = restTemplate.getForObject("http://localhost:9090/omdb?genre=" + text, String.class);
									sendMessage(baseUrl, chatId, movieResult);
									chatStates.remove(chatId);
									return;
								}

								if (text.toLowerCase().contains("blague")) {
									String joke = restTemplate.getForObject("http://localhost:9090/joke", String.class);
									sendMessage(baseUrl, chatId, joke);
								} else if (text.toLowerCase().contains("film") || text.toLowerCase().contains("movie")) {
									sendMessage(baseUrl, chatId, "Quel genre de film veux-tu ? (ex : action, comédie, horreur...)");
									chatStates.put(chatId, "awaiting_movie_genre");
								} else if (text.toLowerCase().contains("meteo")) {
									try {
										boolean forecast = text.toLowerCase().contains("semaine")
												|| text.toLowerCase().contains("prevision")
												|| text.toLowerCase().contains("demain");

										String weatherUrl = "http://localhost:9090/weather?city=Le Mans&forecast=" + forecast;
										Map<?, ?> meteo = restTemplate.getForObject(weatherUrl, Map.class);

										if (meteo != null) {
											StringBuilder messageText = new StringBuilder();
											messageText.append("🌤 meteo à ").append(meteo.get("ville")).append(" :\n");
											messageText.append("☁️ ").append(meteo.get("meteo")).append("\n");
											messageText.append("🌡 Température : ").append(meteo.get("temperature")).append(" °C\n");

											if (forecast && meteo.containsKey("prevision")) {
												List<?> previsions = (List<?>) meteo.get("prevision");
												messageText.append("\n📅 prevision :\n");
												for (Object o : previsions) {
													Map<?, ?> jour = (Map<?, ?>) o;
													messageText.append("🗓 ").append(jour.get("date")).append(" - ")
															.append(jour.get("meteo")).append(", ")
															.append(jour.get("temp")).append(" °C\n");
												}
											}

											sendMessage(baseUrl, chatId, messageText.toString());
										} else {
											sendMessage(baseUrl, chatId, "Impossible de récupérer la meteo.");
										}
									} catch (Exception e) {
										sendMessage(baseUrl, chatId, "Erreur lors de la récupération de la meteo.");
									}
								}

							}

							lastUpdateId = update.getUpdateId();
						}
					}
				} catch (Exception e) {
					System.err.println("Erreur pendant le polling Telegram : " + e.getMessage());
				}
			}
		}, 0, 3000);
	}

	private void sendMessage(String baseUrl, String chatId, String text) {
		String url = baseUrl + "/sendMessage?chat_id=" + chatId + "&text=" + text;
		restTemplate.getForObject(url, String.class);
	}
}
