package com.nova.ledger.controller;

import com.nova.ledger.dto.*;
import com.nova.ledger.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        try {
            var result = userService.register(request, httpRequest);
            return ResponseEntity.ok(ApiResponse.success("注册成功", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            var result = userService.login(request, httpRequest);
            return ResponseEntity.ok(ApiResponse.success("登录成功", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(401, e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<?>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            var result = userService.refreshToken(request);
            return ResponseEntity.ok(ApiResponse.success("Token 刷新成功", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(401, e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication auth,
            HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        userService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("已退出登录", null));
    }

    // ================== 个人信息 ==================

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<?>> profile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(userId)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<?>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        try {
            return ResponseEntity.ok(ApiResponse.success("更新成功", userService.updateProfile(userId, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        try {
            userService.updatePassword(userId, request);
            return ResponseEntity.ok(ApiResponse.success("密码修改成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @GetMapping("/login-logs")
    public ResponseEntity<ApiResponse<?>> loginLogs(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(userService.getLoginLogs(userId)));
    }

    // ================== 管理员接口 ==================

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<?>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.listUsers()));
    }

    @PutMapping("/admin/users/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleUserEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        userService.toggleUserEnabled(id, enabled);
        return ResponseEntity.ok(ApiResponse.success("用户状态已更新", null));
    }

    @PutMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setUserRole(
            @PathVariable Long id,
            @RequestParam String role) {
        userService.setUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.success("用户角色已更新", null));
    }
}
