package com.nova.ledger.service;

import com.nova.ledger.config.JwtUtil;
import com.nova.ledger.dto.*;
import com.nova.ledger.entity.User;
import com.nova.ledger.entity.UserLoginLog;
import com.nova.ledger.repository.UserLoginLogRepository;
import com.nova.ledger.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    private final UserRepository userRepository;
    private final UserLoginLogRepository loginLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    // ==================== Auth ====================

    public Map<String, Object> register(RegisterRequest request, HttpServletRequest httpRequest) {
        String username = request.getUsername().trim();
        String password = request.getPassword();

        // 用户名规则
        if (!Pattern.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]{3,20}$", username)) {
            throw new RuntimeException("用户名只支持字母、数字、下划线和中文(3-20位)");
        }

        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !Pattern.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$", request.getEmail())) {
            throw new RuntimeException("邮箱格式不正确");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .nickname(request.getNickname() != null ? request.getNickname() : username)
                .email(request.getEmail())
                .role(User.UserRole.USER)
                .enabled(true)
                .build();

        userRepository.save(user);

        // 生成 token + refreshToken
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Redis 存 refreshToken（7天过期）
        redisTemplate.opsForValue().set(
                "refresh:" + refreshToken,
                user.getId().toString(),
                7, TimeUnit.DAYS);

        logLogin(user.getId(), httpRequest, true, null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("role", user.getRole().name());
        return result;
    }

    public Map<String, Object> login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!user.getEnabled()) {
            logLogin(user.getId(), httpRequest, false, "账号已被禁用");
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logLogin(user.getId(), httpRequest, false, "密码错误");
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                "refresh:" + refreshToken,
                user.getId().toString(),
                7, TimeUnit.DAYS);

        // 更新最后登录信息
        String ip = getClientIp(httpRequest);
        user.setLastLoginIp(ip);
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        logLogin(user.getId(), httpRequest, true, null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("role", user.getRole().name());
        return result;
    }

    /**
     * Token 刷新
     */
    public Map<String, Object> refreshToken(RefreshTokenRequest request) {
        String key = "refresh:" + request.getRefreshToken();
        String userIdStr = redisTemplate.opsForValue().get(key);
        if (userIdStr == null) {
            throw new RuntimeException("refreshToken 已过期或无效");
        }

        Long userId = Long.parseLong(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 旧的 refreshToken 作废
        redisTemplate.delete(key);

        String newToken = jwtUtil.generateToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                "refresh:" + newRefreshToken,
                user.getId().toString(),
                7, TimeUnit.DAYS);

        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("refreshToken", newRefreshToken);
        return result;
    }

    /**
     * 退出登录
     */
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null && !token.isBlank()) {
            // 将当前 token 加入黑名单，剩余有效期
            redisTemplate.opsForValue().set(
                    "blacklist:" + token,
                    "1",
                    jwtUtil.getRemainingExpiration(token),
                    TimeUnit.MILLISECONDS);
        }
    }

    // ==================== Profile ====================

    public UserProfileDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return toProfileDTO(user);
    }

    public UserProfileDTO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            if (!request.getEmail().isBlank()
                    && !Pattern.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$", request.getEmail())) {
                throw new RuntimeException("邮箱格式不正确");
            }
            user.setEmail(request.getEmail().isBlank() ? null : request.getEmail());
        }
        if (request.getPhone() != null) {
            if (!request.getPhone().isBlank()
                    && !Pattern.matches("^1[3-9]\\d{9}$", request.getPhone())) {
                throw new RuntimeException("手机号格式不正确");
            }
            user.setPhone(request.getPhone().isBlank() ? null : request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar().isBlank() ? null : request.getAvatar());
        }
        if (request.getRemark() != null) {
            user.setRemark(request.getRemark());
        }

        userRepository.save(user);
        return toProfileDTO(user);
    }

    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }

        String pwd = request.getNewPassword();
        if (pwd.length() < 6 || pwd.length() > 100) {
            throw new RuntimeException("密码长度需在6-100之间");
        }

        user.setPassword(passwordEncoder.encode(pwd));
        userRepository.save(user);
    }

    // ==================== Admin ====================

    public java.util.List<UserProfileDTO> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toProfileDTO)
                .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
    }

    @Transactional
    public void toggleUserEnabled(Long targetUserId, boolean enabled) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Transactional
    public void setUserRole(Long targetUserId, String role) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setRole(User.UserRole.valueOf(role));
        userRepository.save(user);
    }

    // ==================== Login Logs ====================

    public java.util.List<UserLoginLog> getLoginLogs(Long userId) {
        return loginLogRepository.findByUserIdAndLoginTimeAfterOrderByLoginTimeDesc(
                userId, LocalDateTime.now().minusDays(30));
    }

    // ==================== Internal ====================

    private void logLogin(Long userId, HttpServletRequest request, boolean success, String reason) {
        UserLoginLog log = UserLoginLog.builder()
                .userId(userId)
                .ip(getClientIp(request))
                .success(success)
                .reason(reason)
                .loginTime(LocalDateTime.now())
                .build();
        loginLogRepository.save(log);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private UserProfileDTO toProfileDTO(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .remark(user.getRemark())
                .lastLoginIp(user.getLastLoginIp())
                .lastLoginTime(user.getLastLoginTime())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
