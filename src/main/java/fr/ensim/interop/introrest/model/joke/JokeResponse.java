package fr.ensim.interop.introrest.model.joke;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JokeResponse {
    private String type;
    private String joke;
    private String setup;
    private String delivery;

    public String getType() {
        return type;
    }

    public String getJoke() {
        return joke;
    }

    public String getSetup() {
        return setup;
    }

    public String getDelivery() {
        return delivery;
    }

    // Méthode utilitaire pour obtenir la blague formatée
    public String getFormattedJoke() {
        if ("single".equals(type)) {
            return joke;
        } else if ("twopart".equals(type)) {
            return setup + "\n" + delivery;
        } else {
            return "Blague non disponible.";
        }
    }
}