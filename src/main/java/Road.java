public class Road {
    private final Localisation destination;
    private final double distance;
    private final String streetName;

    public Road(Localisation destination, double distance, String streetName) {
        this.destination = destination;
        this.distance = distance;
        this.streetName = streetName;
    }

    public Localisation getDestination() { return destination; }
    public double getDistance() { return distance; }
}