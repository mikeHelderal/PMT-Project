package com.exercice.pmt.controller;

import com.exercice.pmt.model.ProjectMember;
import com.exercice.pmt.model.Role;
import com.exercice.pmt.service.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ProjectMemberController {

    private final ProjectMemberService memberService;


    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectMember>> getMembers(@PathVariable Integer projectId) {
        return ResponseEntity.ok(memberService.getMembersByProject(projectId));
    }

    @PostMapping("/project/{projectId}/invite")
    public ResponseEntity<ProjectMember> inviteMember(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String roleName = request.get("roleName");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.addMemberByEmail(projectId, email, roleName));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectMember> updateRole(@PathVariable Long id, @RequestBody Role newRole) {
        return ResponseEntity.ok(memberService.updateMemberRole(id, newRole));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id) {
        memberService.removeMember(id);
        return ResponseEntity.noContent().build();
    }
}
