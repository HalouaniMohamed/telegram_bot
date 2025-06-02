package fr.ensim.interop.introrest.controller;

import fr.ensim.interop.introrest.model.joke.Joke;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/joke")
public class JokeRestController {
    private final Map<Integer, Joke> jokes = new ConcurrentHashMap<>();
    private int currentId = 1;

    public JokeRestController() {
        jokes.put(currentId, new Joke(currentId++, "Dev", "Pourquoi les programmeurs n'aiment pas la nature ? Trop de bugs.", 8));
        jokes.put(currentId, new Joke(currentId++, "Math", "Un byte entre dans un bar et commande une bière. Le barman répond : désolé, on ne sert pas ton type ici.", 6));
        jokes.put(currentId, new Joke(currentId++, "Prof", "Pourquoi les profs de physique aiment les blagues sèches ? Parce qu’elles ont plus de potentiel.", 3));
        jokes.put(currentId, new Joke(currentId++, "Reseau", "Combien de routeurs faut-il pour visser une ampoule ? Aucun, c’est un problème de couche physique.", 7));
        jokes.put(currentId, new Joke(currentId++, "IA", "Pourquoi l’IA ne fait jamais de pause ? Parce qu’elle a trop de processus en cours.", 5));
    }

    @GetMapping
    public String getJoke(@RequestParam(required = false) Integer id,
                          @RequestParam(required = false) String titre,
                          @RequestParam(required = false, name = "rate") String rateFilter) {
        List<Joke> list = new ArrayList<>(jokes.values());
        if (id != null) return jokes.getOrDefault(id, new Joke(0, "", "Pas de blague trouvée.", 0)).getFormatted();
        if (titre != null) return list.stream().filter(j -> j.getTitre().equalsIgnoreCase(titre)).findFirst().map(Joke::getFormatted).orElse("Aucune blague avec ce titre.");
        if (rateFilter != null) {
            try {
                char op = rateFilter.charAt(0);
                int val = Integer.parseInt(rateFilter.substring(1));
                return list.stream()
                        .filter(j -> (op == '<' && j.getNote() < val) || (op == '>' && j.getNote() > val))
                        .findAny().map(Joke::getFormatted).orElse("Aucune blague ne correspond.");
            } catch (Exception ignored) {}
        }
        return list.get(new Random().nextInt(list.size())).getFormatted();
    }

    @PostMapping
    public String addJoke(@RequestBody Joke joke) {
        joke.setId(currentId);
        jokes.put(currentId++, joke);
        return "Blague ajoutée.";
    }

    @PutMapping("/{id}")
    public String editJoke(@PathVariable int id, @RequestBody Joke joke) {
        if (!jokes.containsKey(id)) return "Blague non trouvée.";
        jokes.put(id, new Joke(id, joke.getTitre(), joke.getTexte(), joke.getNote()));
        return "Blague mise à jour.";
    }

    @DeleteMapping("/{id}")
    public String deleteJoke(@PathVariable int id) {
        return jokes.remove(id) != null ? "Blague supprimée." : "Blague introuvable.";
    }
    @GetMapping(produces = "application/json")
    public Joke getJokeAsObject(@RequestParam(required = false) Integer id,
                                @RequestParam(required = false) String titre,
                                @RequestParam(required = false, name = "rate") String rateFilter) {
        List<Joke> list = new ArrayList<>(jokes.values());

        if (id != null) return jokes.getOrDefault(id, new Joke(0, "Erreur", "Pas de blague trouvée.", 0));
        if (titre != null) return list.stream()
                .filter(j -> j.getTitre().equalsIgnoreCase(titre))
                .findFirst().orElse(new Joke(0, "Erreur", "Pas de blague avec ce titre.", 0));
        if (rateFilter != null) {
            try {
                char op = rateFilter.charAt(0);
                int val = Integer.parseInt(rateFilter.substring(1));
                return list.stream()
                        .filter(j -> (op == '<' && j.getNote() < val) || (op == '>' && j.getNote() > val))
                        .findAny().orElse(new Joke(0, "Erreur", "Aucune blague ne correspond.", 0));
            } catch (Exception ignored) {}
        }

        return list.get(new Random().nextInt(list.size()));
    }

}