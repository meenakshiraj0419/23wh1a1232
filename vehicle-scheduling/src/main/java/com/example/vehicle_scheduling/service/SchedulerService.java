package com.example.vehicle_scheduling.service;

import com.example.vehicle_scheduling.model.Depot;
import com.example.vehicle_scheduling.model.Vehicle;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.core.ParameterizedTypeReference;

import java.util.*;

@Service
public class SchedulerService {

    private final String BASE_URL = "http://4.224.186.213/evaluation-service";

    public List<Map<String, Object>> getSchedule(String authHeader) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Fetch depots
        ResponseEntity<Map<String, Object>> depotsResponse = restTemplate.exchange(
            BASE_URL + "/depots", HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        Map<String, Object> depotsBody = depotsResponse.getBody();
        if (depotsBody == null) {
            return new ArrayList<>();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> depots = (List<Map<String, Object>>) depotsBody.get("depots");

        // Fetch vehicles
        ResponseEntity<Map<String, Object>> vehiclesResponse = restTemplate.exchange(
            BASE_URL + "/vehicles", HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        Map<String, Object> vehiclesBody = vehiclesResponse.getBody();
        if (vehiclesBody == null) {
            return new ArrayList<>();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> vehicles = (List<Map<String, Object>>) vehiclesBody.get("vehicles");

        // Run knapsack for each depot
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> depot : depots) {
            int depotID = (int) depot.get("ID");
            int mechanicHours = (int) depot.get("MechanicHours");

            Map<String, Object> result = knapsack(vehicles, mechanicHours);
            result.put("depotID", depotID);
            result.put("mechanicHoursBudget", mechanicHours);
            results.add(result);
        }

        return results;
    }

    private Map<String, Object> knapsack(List<Map<String, Object>> tasks, int capacity) {
        int n = tasks.size();
        int[][] dp = new int[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            Map<String, Object> task = tasks.get(i - 1);
            int duration = getIntValue(task, "Duration");
            int impact = getIntValue(task, "Impact");

            for (int w = 0; w <= capacity; w++) {
                dp[i][w] = dp[i - 1][w];
                if (duration <= w) {
                    dp[i][w] = Math.max(dp[i][w], dp[i - 1][w - duration] + impact);
                }
            }
        }

        // Backtrack to find selected tasks
        List<String> selectedTaskIDs = new ArrayList<>();
        int w = capacity;
        for (int i = n; i >= 1; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                Map<String, Object> task = tasks.get(i - 1);
                Object taskID = task.get("TaskID");
                if (taskID instanceof String) {
                    selectedTaskIDs.add((String) taskID);
                }
                w -= getIntValue(task, "Duration");
            }
        }

        int totalDuration = capacity - w;

        Map<String, Object> result = new HashMap<>();
        result.put("totalImpact", dp[n][capacity]);
        result.put("totalDuration", totalDuration);
        result.put("selectedTaskIDs", selectedTaskIDs);
        return result;
    }

    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}
