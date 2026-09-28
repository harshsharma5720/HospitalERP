package com.itmonteur.hospitalerp.identity;


/**
 * Published by the identity module (AuthService.createUser) right after a user is saved.
 * Other modules create their role-specific profile in a synchronous listener, which runs
 * inside the same transaction: if a profile can't be created, the whole registration
 * rolls back. Belongs to the identity module.
 *
 * @param user the newly saved user (managed entity, id already assigned)
 */
public record UserRegisteredEvent(User user) {
}
