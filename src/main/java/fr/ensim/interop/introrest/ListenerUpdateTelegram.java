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

	private final List<String> villesConnues = Arrays.asList(
			"paris", "lyon", "marseille", "toulouse", "nantes",
			"lille", "bordeaux", "strasbourg", "rennes", "le mans"
	);

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
								String lower = text.toLowerCase();

								if (chatStates.containsKey(chatId) && chatStates.get(chatId).equals("awaiting_movie_genre")) {
									String movieResult = restTemplate.getForObject("http://localhost:9090/omdb?genre=" + text, String.class);
									sendMessage(baseUrl, chatId, movieResult);
									chatStates.remove(chatId);
									lastUpdateId = update.getUpdateId();
									return;
								}

								if (lower.contains("blague")) {
									String joke = restTemplate.getForObject("http://localhost:9090/joke", String.class);
									sendMessage(baseUrl, chatId, joke);
									lastUpdateId = update.getUpdateId();
									return;
								}

								if (lower.contains("film") || lower.contains("movie")) {
									sendMessage(baseUrl, chatId, "Quel genre de film veux-tu ? (ex : action, comédie, horreur...)");
									chatStates.put(chatId, "awaiting_movie_genre");
									lastUpdateId = update.getUpdateId();
									return;
								}

								if (lower.contains("meteo")) {
									try {
										boolean forecast = lower.contains("semaine") || lower.contains("prevision") || lower.contains("demain");

										String ville = null;
										for (String v : villesConnues) {
											if (lower.contains(v)) {
												ville = capitalize(v);
												break;
											}
										}

										if (ville == null) {
											sendMessage(baseUrl, chatId, "Je ne connais pas cette ville. Essaie par exemple : Paris, Lyon, Nantes...");
											lastUpdateId = update.getUpdateId();
											return;
										}

										String weatherUrl = "http://localhost:9090/weather?city=" + ville + "&forecast=" + forecast;
										Map<?, ?> meteo = restTemplate.getForObject(weatherUrl, Map.class);

										if (meteo != null) {
											StringBuilder messageText = new StringBuilder();
											messageText.append("🌤 meteo à ").append(meteo.get("ville")).append(" :\n");
											messageText.append("☁️ ").append(meteo.get("meteo")).append("\n");
											messageText.append("🌡 Température : ").append(meteo.get("temperature")).append(" °C\n");

											if (forecast && meteo.containsKey("prevision")) {
												List<?> previsions = (List<?>) meteo.get("prevision");
												messageText.append("\n📅 prévisions :\n");
												for (Object o : previsions) {
													Map<?, ?> jour = (Map<?, ?>) o;
													messageText.append("🗓 ").append(jour.get("date")).append(" - ")
															.append(jour.get("meteo")).append(", ")
															.append(jour.get("temp")).append(" °C\n");
												}
											}

											sendMessage(baseUrl, chatId, messageText.toString());
											lastUpdateId = update.getUpdateId();
											return;
										} else {
											sendMessage(baseUrl, chatId, "Impossible de récupérer la météo.");
											lastUpdateId = update.getUpdateId();
											return;
										}
									} catch (Exception e) {
										sendMessage(baseUrl, chatId, "Erreur lors de la récupération de la météo.");
										lastUpdateId = update.getUpdateId();
										return;
									}
								}
							}

							// par défaut : marquer le message comme traité
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

	private String capitalize(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
}
