package fr.ensim.interop.introrest;

import fr.ensim.interop.introrest.model.joke.Joke;
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

								if (lower.startsWith("ajoute blague")) {
									String[] parts = text.split("\\|");
									if (parts.length != 4) {
										sendMessage(baseUrl, chatId, "Format attendu : ajoute blague | titre | texte | note");
										lastUpdateId = update.getUpdateId();
										return;
									}
									String titre = parts[1].trim();
									String texte = parts[2].trim();
									int note;
									try {
										note = Integer.parseInt(parts[3].trim());
									} catch (NumberFormatException e) {
										sendMessage(baseUrl, chatId, "Note invalide. Elle doit être un entier.");
										lastUpdateId = update.getUpdateId();
										return;
									}

									Map<String, Object> newJoke = new HashMap<>();
									newJoke.put("titre", titre);
									newJoke.put("texte", texte);
									newJoke.put("note", note);

									try {
										restTemplate.postForObject("http://localhost:9090/joke", newJoke, String.class);
										sendMessage(baseUrl, chatId, "Blague ajoutée !");
									} catch (Exception e) {
										sendMessage(baseUrl, chatId, "Erreur lors de l'ajout de la blague.");
									}
									lastUpdateId = update.getUpdateId();
									return;
								}


								if (lower.startsWith("modifie blague")) {
									String[] parts = text.split("\\|");
									if (parts.length != 5) {
										sendMessage(baseUrl, chatId, "Format : modifie blague | id | titre | texte | note");
										lastUpdateId = update.getUpdateId();
										return;
									}
									int id;
									try {
										id = Integer.parseInt(parts[1].trim());
									} catch (NumberFormatException e) {
										sendMessage(baseUrl, chatId, "ID invalide.");
										lastUpdateId = update.getUpdateId();
										return;
									}
									String titre = parts[2].trim();
									String texte = parts[3].trim();
									int note;
									try {
										note = Integer.parseInt(parts[4].trim());
									} catch (NumberFormatException e) {
										sendMessage(baseUrl, chatId, "Note invalide.");
										lastUpdateId = update.getUpdateId();
										return;
									}

									Map<String, Object> updatedJoke = new HashMap<>();
									updatedJoke.put("titre", titre);
									updatedJoke.put("texte", texte);
									updatedJoke.put("note", note);

									try {
										restTemplate.put("http://localhost:9090/joke/" + id, updatedJoke);
										sendMessage(baseUrl, chatId, "Blague modifiée !");
									} catch (Exception e) {
										sendMessage(baseUrl, chatId, "Erreur lors de la modification.");
									}
									lastUpdateId = update.getUpdateId();
									return;
								}


								if (lower.startsWith("supprime blague")) {
									String[] parts = text.split("\\s+");
									if (parts.length != 3) {
										sendMessage(baseUrl, chatId, "Format : supprime blague [id]");
										lastUpdateId = update.getUpdateId();
										return;
									}
									int id;
									try {
										id = Integer.parseInt(parts[2].trim());
									} catch (NumberFormatException e) {
										sendMessage(baseUrl, chatId, "ID invalide.");
										lastUpdateId = update.getUpdateId();
										return;
									}

									try {
										restTemplate.delete("http://localhost:9090/joke/" + id);
										sendMessage(baseUrl, chatId, "Blague supprimée !");
									} catch (Exception e) {
										sendMessage(baseUrl, chatId, "Erreur lors de la suppression.");
									}
									lastUpdateId = update.getUpdateId();
									return;
								}

								if (lower.contains("blague")) {
									String quality = null;
									if (lower.contains("nulle")) quality = "nulle";
									else if (lower.contains("bonne") || lower.contains("top")) quality = "bonne";

									String jokeUrl = "http://localhost:9090/joke";
									if (quality != null) jokeUrl += "?quality=" + quality;

									Joke joke = restTemplate.getForObject(jokeUrl, Joke.class);
									if (joke != null) {
										sendMessage(baseUrl, chatId, "🤣 " + joke.getTexte() + " (Note : " + joke.getNote() + "/10)");
									} else {
										sendMessage(baseUrl, chatId, "Pas de blague disponible.");
									}

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
										boolean forecast = lower.contains("semaine") || lower.contains("prévision") || lower.contains("demain");

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
