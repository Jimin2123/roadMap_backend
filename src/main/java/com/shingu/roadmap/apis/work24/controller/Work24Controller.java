package com.shingu.roadmap.apis.work24.controller;

import com.shingu.roadmap.apis.work24.dto.response.TrainingCourseResponse;
import com.shingu.roadmap.apis.work24.service.Work24Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class Work24Controller {

    private final Work24Service work24Service;

    @GetMapping("/training-courses")
    public List<TrainingCourseResponse.TrainCourseItem> getCourses(
            @RequestParam List<String> ncsCodes) {
        return work24Service.getAllMatchingCourses(ncsCodes);
    }
}