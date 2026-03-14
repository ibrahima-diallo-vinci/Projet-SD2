public class Road {
    private Localisation origine;
    private Localisation destination;
    private double distance;
    private String nomRue;

    public Road(Localisation origine, Localisation destination, double distance, String nomRue) {
        this.origine = origine;
        this.destination = destination;
        this.distance = distance;
        this.nomRue = nomRue;
    }

    public Localisation getOrigine() {
        return origine;
    }

    public Localisation getDestination() {
        return destination;
    }

    public double getDistance() {
        return distance;
    }

    public String getNomRue() {
        return nomRue;
    }
}