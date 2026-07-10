package Data;

import GeneticAlgorithm.CaregiverPair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InstancesClass {
    private String name;
    private double[] area;
    private Patient[] patients;
    private Service[] services;
    private Caregiver[] caregivers;
    private Offices[] departing_points;
    private double[][] distances;
    private static final Map<String, Set<Integer>> SERVICE_CAREGIVER_CACHE = new ConcurrentHashMap<>();

    public String getName() {
        return name;
    }

    public double[] getArea() {return area;}

    public double[][] getDistances() {
        return distances;
    }

    public Patient[] getPatients() {
        return patients;
    }

    public Caregiver[] getCaregivers() {
        return caregivers;
    }

    //The method should be called by the jackson package after deserialization
    public void setCaregivers(Caregiver[] caregivers) {
        this.caregivers = caregivers;
        initializeCaregiverCache();
        initializePatientServiceCaregiver();
        initializeAllPossibleCaregiverCombinations();
    }

    private void initializePatientServiceCaregiver() {
        for( Patient p : patients){
            Set<Integer> caregiver1 = new HashSet<>(getQualifiedCaregiver(p.getRequired_caregivers()[0].getService()));
            String[] incompatibleCaregivers = p.getIncompatible_caregivers();
            Set<Integer> incompatible = new HashSet<>();
            if(incompatibleCaregivers!=null){
                for(int i = 0; i<incompatibleCaregivers.length; i++){
                    incompatible.add(Integer.parseInt(incompatibleCaregivers[0].substring(1)));
                }
                caregiver1.removeAll(incompatible);
            }
            p.setPossibleFirstCaregiver(caregiver1);
            if(p.getRequired_caregivers().length>1){
                Set<Integer> caregiver2 = new HashSet<>(getQualifiedCaregiver(p.getRequired_caregivers()[1].getService()));
                if(incompatibleCaregivers!=null){
                    caregiver2.removeAll(incompatible);
                }
                p.setPossibleSecondCaregiver(caregiver2);
            }
        }
    }

    private void initializeAllPossibleCaregiverCombinations() {
        for( Patient p : patients){
            if(p.getRequired_caregivers().length>1){
                List<CaregiverPair> caregiverPairs = getListOfCaregiverPairs(p);
                p.setAllPossibleCaregiverCombinations(caregiverPairs);
                p.setAllPossibleCaregiverCombinationsCrossover(new LinkedHashSet<>(caregiverPairs));
            }else {
                List<CaregiverPair> caregiverPairs = new ArrayList<>();
                CaregiverPair caregiverPair;
                Set<Integer>firstCaregivers = p.getPossibleFirstCaregiver();
                for (int i : firstCaregivers) {
                    caregiverPair = new CaregiverPair(i, -1);
                    caregiverPairs.add(caregiverPair);
                }
                p.setAllPossibleCaregiverCombinations(caregiverPairs);
                p.setAllPossibleCaregiverCombinationsCrossover(new LinkedHashSet<>(caregiverPairs));
            }
        }
    }

    private static List<CaregiverPair> getListOfCaregiverPairs(Patient p) {
        Set<Integer>firstCaregivers = p.getPossibleFirstCaregiver();
        Set<Integer>secondCaregivers = p.getPossibleSecondCaregiver();

        List<CaregiverPair> caregiverPairs = new ArrayList<>( firstCaregivers.size() + secondCaregivers.size());
        Set<Long> seen = new HashSet<>();

        for (int i : firstCaregivers) {
            for(int j : secondCaregivers){
                if(i==j) continue;
                long key = (((long) i) << 32) | j;
                if(seen.add(key)){
                    caregiverPairs.add(new CaregiverPair(i, j));
                }
            }
        }

        Set<Integer> allCaregivers = new HashSet<>(firstCaregivers);
        allCaregivers.addAll(secondCaregivers);
        p.setAllCaregiversForDoubleService(allCaregivers);
        return caregiverPairs;
    }

    // Initialize the cache at startup or when dataset changes
    private void initializeCaregiverCache() {
        SERVICE_CAREGIVER_CACHE.clear();
        for (Caregiver c : caregivers) {
            int id = c.getCacheId();
            for (String ability : c.getAbilities()) {
                SERVICE_CAREGIVER_CACHE
                        .computeIfAbsent(ability, k -> new HashSet<>())
                        .add(id);
            }
        }
        // Make cache immutable
        SERVICE_CAREGIVER_CACHE.replaceAll((k, v) -> Collections.unmodifiableSet(v));
    }

    public Set<Integer> getQualifiedCaregiver(String service) {
        // Return cached result if available
        Set<Integer> cached = SERVICE_CAREGIVER_CACHE.get(service);
        if (cached != null) {
            return cached;
        }

        // Fallback to computation if not in cache (shouldn't happen if cache was initialized)
        Set<Integer> caregiverList = new HashSet<>();
        for (Caregiver c : caregivers) {
            if (c.getAbilities().contains(service)) {
                caregiverList.add(c.getCacheId());
            }
        }
        Set<Integer> immutableSet = Collections.unmodifiableSet(caregiverList);
        SERVICE_CAREGIVER_CACHE.put(service, immutableSet);
        return immutableSet;
    }

    public Offices[] getDeparting_points() {
        return departing_points;
    }

    public Service[] getServices() {
        return services;
    }

}
