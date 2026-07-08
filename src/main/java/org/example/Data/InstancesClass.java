package org.example.Data;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Unified representation of both the legacy instance format and the Extended format.
 *
 * <p>The formats use different names for the same concept: legacy files contain
 * {@code central_offices}, while Extended files contain {@code departing_points}.
 * Both JSON properties are normalized to {@link #departingPoints}.</p>
 */
public class InstancesClass {
    private String name;
    private double[] area;
    private Patient[] patients;
    private Service[] services;
    private Caregiver[] caregivers;
    private Offices[] departingPoints;
    private double[][] distances;

    private final Map<String, Set<Integer>> serviceCaregiverCache = new HashMap<>();

    public String getName() {
        return name;
    }

    public double[] getArea() {
        return area;
    }

    public double[][] getDistances() {
        return distances;
    }

    /**
     * Legacy accessor. It returns the same normalized data as
     * {@link #getDeparting_points()}.
     */
    public Offices[] getCentral_offices() {
        return departingPoints;
    }

    /**
     * Extended-format accessor. It returns the same normalized data as
     * {@link #getCentral_offices()}.
     */
    public Offices[] getDeparting_points() {
        return departingPoints;
    }

    @JsonSetter("central_offices")
    public void setCentralOffices(Offices[] centralOffices) {
        this.departingPoints = centralOffices;
    }

    @JsonSetter("departing_points")
    public void setDepartingPoints(Offices[] departingPoints) {
        this.departingPoints = departingPoints;
    }

    public Patient[] getPatients() {
        return patients;
    }

    public Caregiver[] getCaregivers() {
        return caregivers;
    }

    /**
     * Jackson setter only. Derived caregiver assignments are initialized by
     * {@link ReadData} after the complete JSON document has been deserialized.
     */
    public void setCaregivers(Caregiver[] caregivers) {
        this.caregivers = caregivers;
    }

    public Service[] getServices() {
        return services;
    }

    void initializeAfterDeserialization() {
        requireCoreData();
        assignStableArrayIndexes();
        initializeCaregiverCache();
        initializePatientCaregivers();
        initializeCaregiverCombinations();
    }

    private void requireCoreData() {
        Objects.requireNonNull(patients, "Missing required property 'patients'");
        Objects.requireNonNull(services, "Missing required property 'services'");
        Objects.requireNonNull(caregivers, "Missing required property 'caregivers'");
        Objects.requireNonNull(departingPoints,
                "Missing required property 'central_offices' or 'departing_points'");
        Objects.requireNonNull(distances, "Missing required property 'distances'");

        if (distances.length == 0) {
            throw new IllegalArgumentException("Property 'distances' must not be empty");
        }
        for (double[] row : distances) {
            if (row == null || row.length != distances.length) {
                throw new IllegalArgumentException("Property 'distances' must be a square matrix");
            }
        }
    }

    /**
     * Cache IDs are array indexes, independently of whether source IDs begin at
     * c0/s0 (Extended) or c1/s1 (legacy).
     */
    private void assignStableArrayIndexes() {
        Map<String, Integer> pointIndexes = new LinkedHashMap<>();
        for (int i = 0; i < departingPoints.length; i++) {
            Offices point = Objects.requireNonNull(departingPoints[i],
                    "Null entry in office/departing point array");
            point.assignCacheId(i);
            pointIndexes.put(point.getId(), i);
        }

        for (int i = 0; i < services.length; i++) {
            Objects.requireNonNull(services[i], "Null entry in service array").assignCacheId(i);
        }

        for (int i = 0; i < caregivers.length; i++) {
            Caregiver caregiver = Objects.requireNonNull(caregivers[i],
                    "Null entry in caregiver array");
            caregiver.assignCacheId(i);

            Integer startingPointIndex = pointIndexes.get(caregiver.getStarting_point_id());
            if (startingPointIndex == null && caregiver.getStarting_point_id() == null
                    && departingPoints.length == 1) {
                startingPointIndex = 0;
            }
            caregiver.assignCacheStartingPoint(
                    startingPointIndex == null ? -1 : startingPointIndex);
            if (caregiver.getDistance_matrix_index() < 0 && startingPointIndex != null) {
                caregiver.assignDistanceMatrixIndex(startingPointIndex);
            }
        }

        for (int i = 0; i < patients.length; i++) {
            Patient patient = Objects.requireNonNull(patients[i], "Null entry in patient array");
            if (patient.getDistance_matrix_index() < 0) {
                patient.assignDistanceMatrixIndex(departingPoints.length + i);
            }
        }
    }

    private void initializeCaregiverCache() {
        serviceCaregiverCache.clear();
        for (Caregiver caregiver : caregivers) {
            if (caregiver.getAbilities() == null) {
                continue;
            }
            for (String ability : caregiver.getAbilities()) {
                serviceCaregiverCache
                        .computeIfAbsent(ability, ignored -> new LinkedHashSet<>())
                        .add(caregiver.getCacheId());
            }
        }
        serviceCaregiverCache.replaceAll(
                (ignored, caregiverIds) -> Collections.unmodifiableSet(caregiverIds));
    }

    private void initializePatientCaregivers() {
        Map<String, Integer> caregiverIndexesById = new HashMap<>();
        for (Caregiver caregiver : caregivers) {
            caregiverIndexesById.put(caregiver.getId(), caregiver.getCacheId());
        }

        for (Patient patient : patients) {
            Required_Caregiver[] required = patient.getRequired_caregivers();
            if (required == null || required.length == 0) {
                throw new IllegalArgumentException(
                        "Patient '" + patient.getId() + "' has no required caregivers");
            }

            Set<Integer> incompatible = getIncompatibleIndexes(patient, caregiverIndexesById);
            Set<Integer> first = new LinkedHashSet<>(
                    getQualifiedCaregiver(required[0].getService()));
            first.removeAll(incompatible);
            patient.setPossibleFirstCaregiver(first);

            if (required.length > 1) {
                Set<Integer> second = new LinkedHashSet<>(
                        getQualifiedCaregiver(required[1].getService()));
                second.removeAll(incompatible);
                patient.setPossibleSecondCaregiver(second);
            } else {
                patient.setPossibleSecondCaregiver(Collections.emptySet());
            }
        }
    }

    private static Set<Integer> getIncompatibleIndexes(
            Patient patient, Map<String, Integer> caregiverIndexesById) {
        Set<Integer> incompatible = new LinkedHashSet<>();
        if (patient.getIncompatible_caregivers() == null) {
            return incompatible;
        }
        for (String caregiverId : patient.getIncompatible_caregivers()) {
            Integer index = caregiverIndexesById.get(caregiverId);
            if (index != null) {
                incompatible.add(index);
            }
        }
        return incompatible;
    }

    private void initializeCaregiverCombinations() {
        for (Patient patient : patients) {
            List<CaregiverPair> pairs;
            if (patient.getRequired_caregivers().length > 1) {
                pairs = getListOfCaregiverPairs(patient);
            } else {
                pairs = new ArrayList<>();
                for (int caregiver : patient.getPossibleFirstCaregiver()) {
                    pairs.add(new CaregiverPair(caregiver, -1));
                }
            }
            patient.setAllPossibleCaregiverCombinations(pairs);
            patient.setAllPossibleCaregiverCombinationsCrossover(new LinkedHashSet<>(pairs));
        }
    }

    private static List<CaregiverPair> getListOfCaregiverPairs(Patient patient) {
        Set<Integer> firstCaregivers = patient.getPossibleFirstCaregiver();
        Set<Integer> secondCaregivers = patient.getPossibleSecondCaregiver();
        Set<Integer> allCaregivers = new LinkedHashSet<>(firstCaregivers);
        allCaregivers.addAll(secondCaregivers);
        patient.setAllCaregiversForDoubleService(allCaregivers);

        List<CaregiverPair> pairs = new ArrayList<>();
        for (int first : firstCaregivers) {
            for (int second : secondCaregivers) {
                if (first != second) {
                    pairs.add(new CaregiverPair(first, second));
                }
            }
        }
        return pairs;
    }

    public Set<Integer> getQualifiedCaregiver(String service) {
        return serviceCaregiverCache.getOrDefault(service, Collections.emptySet());
    }
}
