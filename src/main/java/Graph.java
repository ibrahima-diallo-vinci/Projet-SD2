import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Graph {

    private final Map<Long, Localisation> localisationsById;
    private final Map<Localisation, List<Road>> adjacencyList;

    public Graph(String localisations, String roads) {
        localisationsById = new HashMap<>();
        adjacencyList = new HashMap<>();

        chargerLocalisations(localisations);
        chargerRoutes(roads);
    }

    private void chargerLocalisations(String fichierLocalisations) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fichierLocalisations)) {
            if (is == null) {
                throw new RuntimeException("Fichier introuvable dans resources : " + fichierLocalisations);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String ligne;
                br.readLine();

                while ((ligne = br.readLine()) != null) {
                    String[] parties = ligne.split(",", -1);

                    long id = Long.parseLong(parties[0]);
                    String name = parties[1];
                    double lat = Double.parseDouble(parties[2]);
                    double lon = Double.parseDouble(parties[3]);
                    double alt = Double.parseDouble(parties[4]);

                    Localisation localisation = new Localisation(id, lat, lon, name, alt);
                    localisationsById.put(id, localisation);
                    adjacencyList.put(localisation, new ArrayList<>());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier des localisations : " + fichierLocalisations, e);
        }
    }

    private void chargerRoutes(String fichierRoutes) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fichierRoutes)) {
            if (is == null) {
                throw new RuntimeException("Fichier introuvable dans resources : " + fichierRoutes);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String ligne;
                br.readLine();

                while ((ligne = br.readLine()) != null) {
                    String[] parties = ligne.split(",", -1);

                    long sourceId = Long.parseLong(parties[0]);
                    long targetId = Long.parseLong(parties[1]);
                    double distance = Double.parseDouble(parties[2]);
                    String streetName = parties[3];

                    Localisation source = localisationsById.get(sourceId);
                    Localisation target = localisationsById.get(targetId);

                    if (source == null || target == null) {
                        throw new IllegalArgumentException("Route invalide : noeud source ou cible introuvable.");
                    }

                    Road road = new Road(source, target, distance, streetName);
                    adjacencyList.get(source).add(road);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier des routes : " + fichierRoutes, e);
        }
    }

    public Localisation[] determinerZoneInondee(long[] idsOrigin, double epsilon) {
        if (idsOrigin == null) {
            throw new IllegalArgumentException("idsOrigin ne peut pas être null");
        }

        List<Localisation> ordreInondation = new ArrayList<>();
        Set<Localisation> visites = new HashSet<>();
        Queue<Localisation> file = new ArrayDeque<>();

        // initialisation avec les points de départ
        for (long id : idsOrigin) {
            Localisation origine = localisationsById.get(id);
            if (origine != null && !visites.contains(origine)) {
                visites.add(origine);
                file.add(origine);
                ordreInondation.add(origine);
            }
        }

        while (!file.isEmpty()) {
            Localisation courant = file.poll();
            List<Road> routes = adjacencyList.get(courant);

            if (routes == null) {
                continue;
            }

            for (Road road : routes) {
                Localisation voisin = road.getDestination();

                if (!visites.contains(voisin)
                        && voisin.getAltitude() <= courant.getAltitude() + epsilon) {
                    visites.add(voisin);
                    file.add(voisin);
                    ordreInondation.add(voisin);
                }
            }
        }

        return ordreInondation.toArray(new Localisation[0]);
    }

    public Deque<Localisation> trouverCheminLePlusCourtPourContournerLaZoneInondee(long idOrigin, long idDestination, Localisation[] floodedZone) {
        return null;
    }

    public Map<Localisation,Double> determinerChronologieDeLaCrue(long[] idsOrigin, double vWaterInit,double k) {
        return null;
    }

    public Deque<Localisation> trouverCheminDEvacuationLePlusCourt(long idOrigin, long idEvacuation, double vVehicule, Map<Localisation,Double> tFlood) {
        return null;
    }
}