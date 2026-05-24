package com.example.canescan_crud;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirestoreInitializer {

    public static void initialize() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Initialize Pathogens (Sample Data)
        Map<String, Object> pathogen = new HashMap<>();
        pathogen.put("common_name", "Red Rot");
        pathogen.put("scientific_name", "Colletotrichum falcatum");
        pathogen.put("description", "A fungal disease causing red lesions on the midrib.");

        db.collection("pathogens").document("p1").set(pathogen);

        // 2. Initialize Treatment Recommendations
        Map<String, Object> treatment = new HashMap<>();
        treatment.put("pathogen_id", "p1");
        treatment.put("action_steps", "Remove infected stalks and use healthy seed material.");
        treatment.put("material_requirements", "Fungicides, Clean tools");

        db.collection("treatment_recommendations").document("t1").set(treatment);

        // Collections like 'scan_logs' and 'diagnostic_results' will be created
        // automatically when the user performs their first scan.
    }
}