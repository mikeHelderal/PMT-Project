package com.exercice.pmt.controller;

import com.exercice.pmt.model.ProjectMember;
import com.exercice.pmt.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService memberService;

    @PostMapping("/{projectId}")
    public ResponseEntity<ProjectMember> addMember(
            @PathVariable Long projectId,
            @RequestParam String email,
            @RequestParam String role) {

        return ResponseEntity.ok(memberService.addMemberByEmail(projectId, email, role));
    }
}
