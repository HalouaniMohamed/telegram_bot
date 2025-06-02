package fr.ensim.interop.introrest.controller;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/omdb")
public class OmdbRestController {

    @Value("${omdb.api.url}")
    private String omdbApiUrl;

    @Value("${omdb.api.key}")
    private String omdbApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public String getMoviesByGenre(@RequestParam String genre) {
        StringBuilder result = new StringBuilder("🎬 Films de genre \"" + genre + "\" :\n");
        int found = 0;

        try {
            for (int i = 1; i <= 3 && found < 3; i++) {
                String url = omdbApiUrl + "?apikey=" + omdbApiKey + "&s=" + genre + "&page=" + i;
                JSONObject json = new JSONObject(restTemplate.getForObject(url, String.class));

                if (!json.has("Search")) continue;
                JSONArray movies = json.getJSONArray("Search");

                for (int j = 0; j < movies.length() && found < 3; j++) {
                    JSONObject movie = movies.getJSONObject(j);
                    String title = movie.getString("Title");
                    String year = movie.getString("Year");
                    result.append("🎞 ").append(title).append(" (").append(year).append(")\n");
                    found++;
                }
            }
        } catch (Exception e) {
            return "Erreur lors de la récupération des films.";
        }

        return found == 0 ? "Aucun film trouvé pour ce genre." : result.toString();
    }
}
