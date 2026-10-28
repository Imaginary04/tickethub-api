package com.tickethub.user;

import com.tickethub.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public List<UserResponse> listUsers() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(UserResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
        user.setRole(request.role());
        return UserResponse.from(userRepository.save(user));
    }
}