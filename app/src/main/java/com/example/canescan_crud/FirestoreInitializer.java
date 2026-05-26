package com.example.canescan_crud;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirestoreInitializer {

    public static void initialize() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Initialize Pathogens
        Map<String, Object> p1 = new HashMap<>();
        p1.put("common_name", "Red Rot");
        p1.put("scientific_name", "Colletotrichum falcatum");
        p1.put("description", "A fungal disease causing red lesions on the midrib and internal stalk tissue.");
        db.collection("pathogens").document("p1").set(p1);

        Map<String, Object> p2 = new HashMap<>();
        p2.put("common_name", "Smut");
        p2.put("scientific_name", "Sporisorium scitamineum");
        p2.put("description", "Characterized by a black whip-like structure emerging from the apex of the stalk.");
        db.collection("pathogens").document("p2").set(p2);

        // 2. Initialize Treatment Recommendations
        Map<String, Object> t1 = new HashMap<>();
        t1.put("pathogen_id", "p1");
        t1.put("action_steps", "1. Remove and burn infected stalks. 2. Use resistant varieties. 3. Avoid waterlogging.");
        t1.put("material_requirements", "Resistant seed material, proper drainage equipment.");
        db.collection("treatment_recommendations").document("t1").set(t1);

        Map<String, Object> t2 = new HashMap<>();
        t2.put("pathogen_id", "p2");
        t2.put("action_steps", "1. Roguing of infected clumps. 2. Hot water treatment of sets. 3. Crop rotation.");
        t2.put("material_requirements", "Clean planting material, hot water treatment tank.");
        db.collection("treatment_recommendations").document("t2").set(t2);

        // Collections like 'scan_logs' and 'diagnostic_results' will be created
        // automatically when the user performs their first scan.
    }
}