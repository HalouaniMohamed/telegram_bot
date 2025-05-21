package fr.ensim.interop.introrest.controller;

import fr.ensim.interop.introrest.model.joke.JokeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/joke")
public class JokeRestController {

    @Value("${joke.api.url}")
    private String jokeApiUrl;
    private final RestTemplate restTemplate = new RestTemplate();


    @GetMapping
    public String getJokeFromApi() {
        JokeResponse response = restTemplate.getForObject(jokeApiUrl, JokeResponse.class);
        if (response != null) {
            return response.getFormattedJoke();
        } else {
            return "Erreur lors de la récupération de la blague.";
        }
    }
}