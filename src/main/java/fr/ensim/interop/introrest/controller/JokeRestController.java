package fr.ensim.interop.introrest.controller;

import fr.ensim.interop.introrest.model.joke.Joke;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/joke")
public class JokeRestController {

    private final Map<Integer, Joke> jokes = new HashMap<>();
    private int currentId = 1;

    public JokeRestController() {
        jokes.put(currentId, new Joke(currentId, "Dev", "Pourquoi les programmeurs n'aiment pas la nature ? Trop de bugs.", 8));
        currentId++;
        jokes.put(currentId, new Joke(currentId, "Math", "Un byte entre dans un bar et commande une bière. Le barman répond : désolé, on ne sert pas ton type ici.", 6));
        currentId++;
        jokes.put(currentId, new Joke(currentId, "Prof", "Pourquoi les profs de physique aiment les blagues sèches ? Parce qu’elles ont plus de potentiel.", 3));
        currentId++;
        jokes.put(currentId, new Joke(currentId, "ChatGPT", "Pourquoi ChatGPT ne joue jamais à cache-cache ? Parce qu'il ne peut pas se cacher, il est toujours en ligne.", 7));
        currentId++;
        jokes.put(currentId, new Joke(currentId, "Informatique", "Combien de programmeurs faut-il pour changer une ampoule ? Aucun, c’est un problème hardware.", 2));
        currentId++;
    }


    @GetMapping
    public Joke getRandomJoke(@RequestParam(required = false) String quality) {
        List<Joke> filtered = new ArrayList<>(jokes.values());
        if ("nulle".equalsIgnoreCase(quality)) {
            filtered.removeIf(j -> j.getNote() > 4);
        } else if ("bonne".equalsIgnoreCase(quality)) {
            filtered.removeIf(j -> j.getNote() < 7);
        }
        if (filtered.isEmpty()) return new Joke(0, "Aucune", "Pas de blague trouvée pour cette note.", 0);
        return filtered.get(new Random().nextInt(filtered.size()));
    }

    @PostMapping
    public Joke addJoke(@RequestBody Joke joke) {
        joke.setId(currentId++);
        jokes.put(joke.getId(), joke);
        return joke;
    }

    @PutMapping("/{id}")
    public Joke updateJoke(@PathVariable int id, @RequestBody Joke newJoke) {
        Joke old = jokes.get(id);
        if (old == null) return null;
        old.setTitre(newJoke.getTitre());
        old.setTexte(newJoke.getTexte());
        old.setNote(newJoke.getNote());
        return old;
    }

    @DeleteMapping("/{id}")
    public String deleteJoke(@PathVariable int id) {
        if (jokes.remove(id) != null)
            return "Supprimée avec succès.";
        return "Blague non trouvée.";
    }
}
