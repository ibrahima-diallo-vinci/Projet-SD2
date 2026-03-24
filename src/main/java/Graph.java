import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Graph {

    private final Map<Long, Localisation> localisationsParId;
    private final Map<Localisation, List<Road>> listeAdjacence;

    public Graph(String localisations, String roads) {
        localisationsParId = new HashMap<>();
        listeAdjacence = new HashMap<>();

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
                    localisationsParId.put(id, localisation);
                    listeAdjacence.put(localisation, new ArrayList<>());
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

                    Localisation source = localisationsParId.get(sourceId);
                    Localisation target = localisationsParId.get(targetId);

                    if (source == null || target == null) {
                        throw new IllegalArgumentException("Route invalide : noeud source ou cible introuvable.");
                    }

                    Road road = new Road(source, target, distance, streetName);
                    listeAdjacence.get(source).add(road);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier des routes : " + fichierRoutes, e);
        }
    }

    public Localisation[] determinerZoneInondee(long[] idsOrigin, double epsilon) {

        if (idsOrigin == null) {
            throw new IllegalArgumentException("idsOrigin null");
        }

        List<Localisation> resultat = new ArrayList<>();
        Set<Localisation> visites = new HashSet<>();
        Queue<Localisation> file = new ArrayDeque<>();

        for (long id : idsOrigin) {
            Localisation loc = localisationsParId.get(id);

            if (loc == null) {
                throw new IllegalArgumentException("ID inconnu : " + id);
            }

            if (!visites.contains(loc)) {
                visites.add(loc);
                file.add(loc);
                resultat.add(loc);
            }
        }

        while (!file.isEmpty()) {

            Localisation courant = file.poll();

            for (Road road : listeAdjacence.get(courant)) {

                Localisation voisin = road.getDestination();

                if (!visites.contains(voisin)
                        && voisin.getAltitude() <= courant.getAltitude() + epsilon) {

                    visites.add(voisin);
                    file.add(voisin);
                    resultat.add(voisin);
                }
            }
        }

        return resultat.toArray(new Localisation[0]);
    }

    public Deque<Localisation> trouverCheminLePlusCourtPourContournerLaZoneInondee(long idOrigin, long idDestination, Localisation[] floodedZone) {
        Localisation depart = localisationsParId.get(idOrigin);
        Localisation arrivee = localisationsParId.get(idDestination);

        if (depart == null || arrivee == null) {
            throw new IllegalArgumentException("ID invalide");
        }

        Set<Localisation> inonde = new HashSet<>(Arrays.asList(floodedZone));

        Queue<Localisation> file = new ArrayDeque<>();
        Map<Localisation, Localisation> parent = new HashMap<>();
        Set<Localisation> visites = new HashSet<>();

        file.add(depart);
        visites.add(depart);

        while (!file.isEmpty()) {

            Localisation courant = file.poll();

            if (courant.equals(arrivee)) {
                break;
            }

            for (Road road : listeAdjacence.get(courant)) {

                Localisation voisin = road.getDestination();

                if (!visites.contains(voisin) && !inonde.contains(voisin)) {

                    visites.add(voisin);
                    parent.put(voisin, courant);
                    file.add(voisin);
                }
            }
        }

        if (!parent.containsKey(arrivee) && !depart.equals(arrivee)) {
            throw new RuntimeException("Aucun chemin possible");
        }

        Deque<Localisation> chemin = new ArrayDeque<>();
        Localisation courant = arrivee;

        while (courant != null) {
            chemin.addFirst(courant);
            courant = parent.get(courant);
        }

        return chemin;
    }

    public Map<Localisation,Double> determinerChronologieDeLaCrue(long[] idsOrigin, double vWaterInit,double k) {
        Map<Localisation, Double> tFlood = new HashMap<>();
        Map<Localisation, Double> vitesse = new HashMap<>();

        PriorityQueue<Localisation> pq = new PriorityQueue<>(
                Comparator.comparingDouble(tFlood::get)
        );

        for (long id : idsOrigin) {
            Localisation loc = localisationsParId.get(id);

            if (loc == null) {
                throw new IllegalArgumentException("ID inconnu : " + id);
            }

            tFlood.put(loc, 0.0);
            vitesse.put(loc, vWaterInit);
            pq.add(loc);
        }

        while (!pq.isEmpty()) {

            Localisation courant = pq.poll();

            double tempsCourant = tFlood.get(courant);
            double vitesseCourante = vitesse.get(courant);

            for (Road road : listeAdjacence.get(courant)) {

                Localisation voisin = road.getDestination();
                double distance = road.getDistance();

                double pente = (courant.getAltitude() - voisin.getAltitude()) / distance;

                double nouvelleVitesse = vitesseCourante + (k * pente);

                if (nouvelleVitesse <= 0) {
                    continue;
                }

                double tempsArc = distance / nouvelleVitesse;
                double nouveauTemps = tempsCourant + tempsArc;

                if (!tFlood.containsKey(voisin) || nouveauTemps < tFlood.get(voisin)) {

                    tFlood.put(voisin, nouveauTemps);
                    vitesse.put(voisin, nouvelleVitesse);
                    pq.add(voisin);
                }
            }
        }

        return tFlood;
    }

    public Deque<Localisation> trouverCheminDEvacuationLePlusCourt(long idOrigin, long idEvacuation, double vVehicule, Map<Localisation,Double> tFlood) {
        Localisation depart = localisationsParId.get(idOrigin);
        Localisation arrivee = localisationsParId.get(idEvacuation);

        if (depart == null || arrivee == null) {
            throw new IllegalArgumentException("ID invalide");
        }

        Map<Localisation, Double> temps = new HashMap<>();
        Map<Localisation, Localisation> parent = new HashMap<>();

        PriorityQueue<Localisation> pq = new PriorityQueue<>(
                Comparator.comparingDouble(temps::get)
        );

        temps.put(depart, 0.0);
        pq.add(depart);

        while (!pq.isEmpty()) {

            Localisation courant = pq.poll();
            double tempsCourant = temps.get(courant);

            if (courant.equals(arrivee)) {
                break;
            }

            for (Road road : listeAdjacence.get(courant)) {

                Localisation voisin = road.getDestination();

                double tempsArc = road.getDistance() / vVehicule;
                double nouveauTemps = tempsCourant + tempsArc;

                Double tFloodVoisin = tFlood.get(voisin);

                if (tFloodVoisin != null && nouveauTemps >= tFloodVoisin) {
                    continue;
                }

                if (!temps.containsKey(voisin) || nouveauTemps < temps.get(voisin)) {

                    temps.put(voisin, nouveauTemps);
                    parent.put(voisin, courant);
                    pq.add(voisin);
                }
            }
        }

        if (!temps.containsKey(arrivee)) {
            throw new RuntimeException("Aucun chemin possible");
        }

        Deque<Localisation> chemin = new ArrayDeque<>();
        Localisation courant = arrivee;

        while (courant != null) {
            chemin.addFirst(courant);
            courant = parent.get(courant);
        }

        return chemin;
    }
}