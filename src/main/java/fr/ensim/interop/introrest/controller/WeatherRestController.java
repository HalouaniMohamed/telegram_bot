package fr.ensim.interop.introrest.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@RestController
@RequestMapping("/weather")
public class WeatherRestController {

    @Value("${open.weather.api.token}")
    private String apiKey;

    // URL pour la mÃ©tÃ©o du jour (codÃ©e en dur ici)
    private final String currentUrl = "https://api.openweathermap.org/data/2.5/weather";

    @Value("${open.weather.api.url}")
    private String forecastUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public ResponseEntity<?> getWeather(
            @RequestParam String city,
            @RequestParam(defaultValue = "false") boolean forecast
    ) {
        try {
            // Appel mÃ©tÃ©o du jour
            String currentWeatherUrl = currentUrl + "?q=" + city + "&appid=" + apiKey + "&units=metric&lang=fr";
            Map<String, Object> result = new HashMap<>();
            Map<?, ?> current = restTemplate.getForObject(currentWeatherUrl, Map.class);
            System.out.println("RESPONSE CURRENT : " + current); // ⬅️ Ajoute ça


            result.put("ville", city);

            List<?> weatherList = (List<?>) current.get("weather");
            Map<?, ?> weather = (Map<?, ?>) (weatherList != null && !weatherList.isEmpty() ? weatherList.get(0) : new HashMap<>());
            Map<?, ?> main = (Map<?, ?>) current.get("main");

            result.put("meteo", weather.get("description"));
            result.put("temperature", main != null ? main.get("temp") : null);

            if (forecast) {
                String forecastApiUrl = forecastUrl + "?q=" + city + "&appid=" + apiKey + "&units=metric&lang=fr";
                Map<?, ?> forecastResponse = restTemplate.getForObject(forecastApiUrl, Map.class);
                List<Map<String, Object>> previsions = new ArrayList<>();

                List<?> list = (List<?>) forecastResponse.get("list");
                for (Object o : list) {
                    Map<?, ?> item = (Map<?, ?>) o;
                    String datetime = (String) item.get("dt_txt");
                    if (datetime.contains("12:00:00")) { // on prend le midi uniquement
                        Map<String, Object> bloc = new HashMap<>();
                        bloc.put("date", datetime);
                        bloc.put("temp", ((Map<?, ?>) item.get("main")).get("temp"));
                        bloc.put("meteo", ((Map<?, ?>)((List<?>) item.get("weather")).get(0)).get("description"));
                        previsions.add(bloc);
                        if (previsions.size() == 2) break; // on prend 2 jours
                    }
                }

                result.put("prevision", previsions);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur meteo : " + e.getMessage());
        }
    }
}


