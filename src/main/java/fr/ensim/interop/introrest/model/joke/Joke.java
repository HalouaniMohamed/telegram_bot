package fr.ensim.interop.introrest.model.joke;

public class Joke {
    private int id;
    private String titre;
    private String texte;
    private int note;

    public Joke() {}

    public Joke(String titre, String texte, int note) {
        this.titre = titre;
        this.texte = texte;
        this.note = note;
    }

    public Joke(int id, String titre, String texte, int note) {
        this.id = id;
        this.titre = titre;
        this.texte = texte;
        this.note = note;
    }

    public String getFormatted() {
        return "[" + titre + "] " + texte + " (note: " + note + "/10)";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }
    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }
}
