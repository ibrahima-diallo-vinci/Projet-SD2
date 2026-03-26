import java.io.*;
import java.util.*;

public class Graph {

    private final Map<Long, Localisation> localisationsParId = new HashMap<>(1000000);
    private final Map<Long, List<Road>> listeAdjacence = new HashMap<>(1000000);

    public Graph(String nodesFile, String edgesFile) {
        chargerLocalisations(nodesFile);
        chargerRoutes(edgesFile);
    }

    private void chargerLocalisations(String fichier) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fichier);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            br.readLine();
            String ligne;
            while ((ligne = br.readLine()) != null) {
                String[] p = ligne.split(",");
                long id = Long.parseLong(p[0]);
                Localisation loc = new Localisation(id, Double.parseDouble(p[2]),
                        Double.parseDouble(p[3]), p[1], Double.parseDouble(p[4]));
                localisationsParId.put(id, loc);
                listeAdjacence.put(id, new ArrayList<>());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void chargerRoutes(String fichier) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fichier);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            br.readLine();
            String ligne;
            while ((ligne = br.readLine()) != null) {
                String[] p = ligne.split(",");
                long src = Long.parseLong(p[0]);
                long dest = Long.parseLong(p[1]);
                if (localisationsParId.containsKey(src) && localisationsParId.containsKey(dest)) {
                    listeAdjacence.get(src).add(new Road(localisationsParId.get(dest), Double.parseDouble(p[2]), p[3]));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public Localisation[] determinerZoneInondee(long[] idsDepart, double epsilon) {
        List<Localisation> resultat = new ArrayList<>();
        Set<Long> visites = new HashSet<>();
        Queue<Localisation> file = new ArrayDeque<>();

        for (long id : idsDepart) {
            Localisation loc = localisationsParId.get(id);
            if (loc != null && visites.add(id)) {
                file.add(loc);
                resultat.add(loc);
            }
        }

        while (!file.isEmpty()) {
            Localisation courant = file.poll();
            for (Road road : listeAdjacence.get(courant.getId())) {
                Localisation voisin = road.getDestination();
                if (!visites.contains(voisin.getId()) && voisin.getAltitude() <= courant.getAltitude() + epsilon) {
                    visites.add(voisin.getId());
                    file.add(voisin);
                    resultat.add(voisin);
                }
            }
        }
        return resultat.toArray(new Localisation[0]);
    }

    public Deque trouverCheminLePlusCourtPourContournerLaZoneInondee(long idDepart, long idArrivee, Localisation[] zoneInondee) {
        Set<Long> inonde = new HashSet<>();
        for (Localisation l : zoneInondee) inonde.add(l.getId());

        if (inonde.contains(idDepart)) return new ArrayDeque();

        Queue<Long> file = new ArrayDeque<>();
        Map<Long, Long> parents = new HashMap<>();
        file.add(idDepart);
        parents.put(idDepart, null);

        while (!file.isEmpty()) {
            long courant = file.poll();
            if (courant == idArrivee) break;

            for (Road road : listeAdjacence.get(courant)) {
                long voisinId = road.getDestination().getId();
                if (!parents.containsKey(voisinId) && !inonde.contains(voisinId)) {
                    parents.put(voisinId, courant);
                    file.add(voisinId);
                }
            }
        }
        return reconstruireChemin(parents, idArrivee);
    }

    public Map determinerChronologieDeLaCrue(long[] idsDepart, double vWaterInit, double k) {
        Map<Long, Double> tempsFlood = new HashMap<>();
        Map<Long, Double> vitesseNode = new HashMap<>();
        PriorityQueue<Long> pq = new PriorityQueue<>(Comparator.comparingDouble(tempsFlood::get));

        Map<Localisation, Double> resultFinal = new LinkedHashMap<>();

        for (long id : idsDepart) {
            tempsFlood.put(id, 0.0);
            vitesseNode.put(id, vWaterInit);
            pq.add(id);
        }

        while (!pq.isEmpty()) {
            long currId = pq.poll();
            double currTime = tempsFlood.get(currId);
            double currV = vitesseNode.get(currId);

            Localisation currLoc = localisationsParId.get(currId);
            if (!resultFinal.containsKey(currLoc)) {
                resultFinal.put(currLoc, currTime);
            }

            for (Road road : listeAdjacence.get(currId)) {
                Localisation dest = road.getDestination();
                double pente = (currLoc.getAltitude() - dest.getAltitude()) / road.getDistance();
                double nextV = currV + (k * pente);

                if (nextV > 0) {
                    double tempsArc = road.getDistance() / nextV;
                    double arrivalTime = currTime + tempsArc;

                    if (!tempsFlood.containsKey(dest.getId()) || arrivalTime < tempsFlood.get(dest.getId())) {
                        tempsFlood.put(dest.getId(), arrivalTime);
                        vitesseNode.put(dest.getId(), nextV);
                        pq.add(dest.getId());
                    }
                }
            }
        }
        return resultFinal;
    }

    public Deque trouverCheminDEvacuationLePlusCourt(long idDepart, long idEvacuation, double vVehicule, Map tFlood) {
        Map<Localisation, Double> floodMap = (Map<Localisation, Double>) tFlood;
        Map<Long, Double> reachTime = new HashMap<>();
        Map<Long, Long> parents = new HashMap<>();
        PriorityQueue<Long> pq = new PriorityQueue<>(Comparator.comparingDouble(reachTime::get));

        reachTime.put(idDepart, 0.0);
        pq.add(idDepart);

        while (!pq.isEmpty()) {
            long curr = pq.poll();
            if (curr == idEvacuation) break;
            double currTime = reachTime.get(curr);

            for (Road road : listeAdjacence.get(curr)) {
                long nextId = road.getDestination().getId();
                double travelTime = road.getDistance() / vVehicule;
                double arrivalTime = currTime + travelTime;

                Double fTime = floodMap.get(localisationsParId.get(nextId));
                if (fTime == null || arrivalTime < fTime) {
                    if (!reachTime.containsKey(nextId) || arrivalTime < reachTime.get(nextId)) {
                        reachTime.put(nextId, arrivalTime);
                        parents.put(nextId, curr);
                        pq.add(nextId);
                    }
                }
            }
        }
        return reconstruireChemin(parents, idEvacuation);
    }

    private Deque<Localisation> reconstruireChemin(Map<Long, Long> parents, long targetId) {
        Deque<Localisation> path = new ArrayDeque<>();
        if (!parents.containsKey(targetId)) {
            return path;
        }
        Long curr = targetId;
        while (curr != null) {
            path.addFirst(localisationsParId.get(curr));
            curr = parents.get(curr);
        }
        return path;
    }
}
