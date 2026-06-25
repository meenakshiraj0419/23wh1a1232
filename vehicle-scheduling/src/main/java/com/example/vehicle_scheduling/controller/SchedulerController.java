package com.example.vehicle_scheduling.controller;

import com.example.vehicle_scheduling.service.SchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SchedulerController {

    @Autowired
    private SchedulerService schedulerService;

    @GetMapping("/schedule")
    public ResponseEntity<?> getSchedule(
        @RequestHeader("Authorization") String authHeader
    ) {
        try {
            List<Map<String, Object>> results = schedulerService.getSchedule(authHeader);
            return ResponseEntity.ok(Map.of("success", true, "results", results));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
