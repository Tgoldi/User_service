package com.example.userservice.controller;

import com.example.userservice.model.User;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Validated
public class UserController {

    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        if (userService.emailExists(user.getEmail())) {
            return new ResponseEntity<>("Email already in use", HttpStatus.BAD_REQUEST);
        }
        User newUser = userService.saveUser(user);
        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllUsers(Authentication authentication,
                                          @PageableDefault(size = 20) Pageable pageable) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable boundedPageable = pageable.first().getPageSize() == pageSize
                ? pageable
                : org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageSize, pageable.getSort());

        if (isAdmin) {
            Page<User> users = userService.getAllUsersPaged(boundedPageable);
            return ResponseEntity.ok(users);
        }

        String currentUsername = authentication.getName();
        return userService.getUserByUsername(currentUsername)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(List.of(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getUserById(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return userService.getUserById(id)
                .filter(user -> isAdmin || user.getUsername().equals(authentication.getName()))
                .map(user -> ResponseEntity.ok(user))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails,
                                         Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return userService.getUserById(id)
                .<ResponseEntity<?>>map(existing -> {
                    if (!isAdmin && !existing.getUsername().equals(authentication.getName())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                    try {
                        User updatedUser = userService.updateUser(id, userDetails);
                        return ResponseEntity.ok(updatedUser);
                    } catch (RuntimeException ex) {
                        return ResponseEntity.notFound().build();
                    }
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return userService.getUserById(id)
                .<ResponseEntity<?>>map(existing -> {
                    if (!isAdmin && !existing.getUsername().equals(authentication.getName())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                    try {
                        userService.deleteUser(id);
                        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                    } catch (RuntimeException ex) {
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    }
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
