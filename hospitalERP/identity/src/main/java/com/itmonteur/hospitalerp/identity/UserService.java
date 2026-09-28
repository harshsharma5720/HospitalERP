package com.itmonteur.hospitalerp.identity;

import com.itmonteur.hospitalerp.identity.internal.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    /** Keeps the login account's contact details in sync with a profile; a null phone is left unchanged. */
    public void updateContactDetails(User user, String email, String phoneNumber) {
        user.setEmail(email);
        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber);
        }
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
