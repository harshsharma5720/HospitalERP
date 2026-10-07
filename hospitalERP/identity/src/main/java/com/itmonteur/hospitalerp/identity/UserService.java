package com.itmonteur.hospitalerp.identity;

import com.itmonteur.hospitalerp.identity.internal.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Public API of the identity module for login accounts. Other modules use this instead of
 * UserRepository, which stays internal to identity. Belongs to the identity module.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findUser(Long userId) {
        return userRepository.findById(userId);
    }

    // ---------- admin dashboard (docs/ADMIN_DASHBOARD_PLAN.md) ----------

    /** Accounts that aren't deactivated, per role; every role is present (0 when there are none). */
    public Map<Role, Long> countActiveAccountsPerRole() {
        Map<Role, Long> counts = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            counts.put(role, 0L);
        }
        for (Object[] row : userRepository.countActivePerRole()) {
            if (row[0] != null) {
                counts.put((Role) row[0], ((Number) row[1]).longValue());
            }
        }
        return counts;
    }

    /**
     * New accounts of a role per day, from {@code from} to {@code to} (both included); only days with new
     * accounts are present. Accounts created before users.created_at existed are never counted.
     */
    public Map<LocalDate, Long> countNewAccountsPerDay(Role role, LocalDate from, LocalDate to) {
        Map<LocalDate, Long> perDay = new TreeMap<>();
        userRepository.findCreationTimes(role, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .forEach(createdAt -> perDay.merge(createdAt.toLocalDate(), 1L, Long::sum));
        return perDay;
    }

    /** When the first account with a creation time was created, i.e. since when new accounts are counted. */
    public Optional<LocalDateTime> firstAccountCreationTime() {
        return userRepository.findFirstCreationTime();
    }

    /** Keeps the login account's contact details in sync with a profile; a null phone is left unchanged. */
    public void updateContactDetails(User user, String email, String phoneNumber) {
        user.setEmail(email);
        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber);
        }
        userRepository.save(user);
    }

    /** No login any more, hidden from directories; all history stays (docs/ACCOUNT_DEACTIVATION_PLAN.md). */
    public void deactivate(User user) {
        user.setActive(false);
        user.setDeactivatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void reactivate(User user) {
        user.setActive(true);
        user.setDeactivatedAt(null);
        userRepository.save(user);
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    /** By id, for callers whose persistence context was cleared (see UserAccountService). */
    public void deleteUserById(Long userId) {
        userRepository.deleteById(userId);
    }
}
