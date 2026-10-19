package com.itmonteur.hospitalerp.identity.internal;

import com.itmonteur.hospitalerp.identity.Role;
import com.itmonteur.hospitalerp.identity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Login sessions (docs/SAFER_LOGIN_TOKENS_PLAN.md) with a clock moved by hand and the tokens kept in a list: the
 * lifetimes of patients and staff, rotation, reuse within and after the grace, deactivation, logout.
 */
class RefreshTokenServiceTest {

    /** A clock that only moves when told to. */
    private static final class HandClock extends Clock {
        private Instant now = Instant.parse("2026-10-16T09:00:00Z");

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final List<RefreshToken> rows = new ArrayList<>();
    private final HandClock clock = new HandClock();
    private final User patient = user(1L, Role.PATIENT);
    private final User doctor = user(2L, Role.DOCTOR);
    private RefreshTokenService service;

    private static User user(long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setRole(role);
        return user;
    }

    @BeforeEach
    void setUp() {
        RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
        when(tokens.save(any())).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(tokens.findByTokenHashForUpdate(anyString())).thenAnswer(inv -> byHash(inv.getArgument(0)));
        when(tokens.findByTokenHash(anyString())).thenAnswer(inv -> byHash(inv.getArgument(0)));
        when(tokens.findBySessionIdAndEndedAtIsNull(anyString())).thenAnswer(inv -> rows.stream()
                .filter(t -> t.getSessionId().equals(inv.getArgument(0)) && t.getEndedAt() == null).toList());
        when(tokens.findByUserIdAndEndedAtIsNull(anyLong())).thenAnswer(inv -> rows.stream()
                .filter(t -> t.getUserId().equals(inv.getArgument(0)) && t.getEndedAt() == null).toList());
        UserRepository users = mock(UserRepository.class);
        when(users.findById(1L)).thenReturn(Optional.of(patient));
        when(users.findById(2L)).thenReturn(Optional.of(doctor));
        service = new RefreshTokenService(tokens, users, clock, 7, 90, 12);
    }

    private Optional<RefreshToken> byHash(String hash) {
        return rows.stream().filter(t -> t.getTokenHash().equals(hash)).findFirst();
    }

    private void refused(String token) {
        assertThatThrownBy(() -> service.refresh(token)).isInstanceOf(RefreshTokenService.InvalidRefreshTokenException.class);
    }

    @Test
    void onlyTheHashIsKeptAndEveryUseReplacesTheToken() {
        RefreshTokenService.Issued first = service.start(patient);
        assertThat(first.token()).hasSize(43); // 256 bits, Base64 without padding
        assertThat(rows.get(0).getTokenHash()).isEqualTo(RefreshTokenService.hash(first.token())).isNotEqualTo(first.token());

        clock.advance(Duration.ofHours(1));
        RefreshTokenService.Refreshed second = service.refresh(first.token());
        assertThat(second.user()).isSameAs(patient);
        assertThat(second.next().token()).isNotEqualTo(first.token());
        assertThat(rows.get(0).getReplacedAt()).isNotNull();
        assertThat(rows.get(1).getSessionId()).as("the same login").isEqualTo(rows.get(0).getSessionId());
        assertThat(service.refresh(second.next().token()).user()).isSameAs(patient);
    }

    @Test
    void aPatientStaysLoggedInWhileComingBackWithinSevenDaysButAtMostNinetyDays() {
        RefreshTokenService.Issued issued = service.start(patient);
        assertThat(issued.maxAge()).isEqualTo(Duration.ofDays(7));
        String token = issued.token();
        for (int i = 0; i < 14; i++) { // every 6 days: 84 days
            clock.advance(Duration.ofDays(6));
            token = service.refresh(token).next().token();
        }
        clock.advance(Duration.ofDays(5)); // day 89
        RefreshTokenService.Issued last = service.refresh(token).next();
        assertThat(last.maxAge()).as("only a day left of the 90").isEqualTo(Duration.ofDays(1));
        clock.advance(Duration.ofDays(1));
        refused(last.token());

        // Not coming back for 7 days
        String idle = service.start(patient).token();
        clock.advance(Duration.ofDays(7));
        refused(idle);
    }

    @Test
    void staffAreLoggedOutTwelveHoursAfterLoggingInHoweverOftenTheyRefresh() {
        RefreshTokenService.Issued issued = service.start(doctor);
        assertThat(issued.maxAge()).isEqualTo(Duration.ofHours(12));
        clock.advance(Duration.ofHours(11));
        RefreshTokenService.Issued later = service.refresh(issued.token()).next();
        assertThat(later.maxAge()).isEqualTo(Duration.ofHours(1));
        clock.advance(Duration.ofHours(1));
        refused(later.token());
    }

    @Test
    void anOldTokenUsedAgainWithinThirtySecondsIsTwoTabsAfterwardsItEndsTheSession() {
        String old = service.start(patient).token();
        String otherLogin = service.start(patient).token();
        String newer = service.refresh(old).next().token();

        clock.advance(Duration.ofSeconds(30));
        String secondTab = service.refresh(old).next().token();
        assertThat(secondTab).isNotEqualTo(newer);

        clock.advance(Duration.ofSeconds(1));
        refused(old); // 31 seconds after it was replaced: copied
        refused(newer);
        refused(secondTab);
        assertThat(rows.stream().filter(t -> t.getEndReason() == SessionEndReason.REUSED)).hasSize(3);
        assertThat(service.refresh(otherLogin).user()).as("another login of the same user").isSameAs(patient);
    }

    @Test
    void aDeactivatedAccountCantRefreshAndLogoutsEndSessions() {
        String token = service.start(patient).token();
        patient.setActive(false);
        refused(token);
        assertThat(rows.get(0).getEndReason()).isEqualTo(SessionEndReason.DEACTIVATED);
        patient.setActive(true);
        refused(token);

        String here = service.start(patient).token();
        String there = service.start(patient).token();
        String doctors = service.start(doctor).token();
        service.end(here);
        refused(here);
        assertThat(service.refresh(there).user()).isSameAs(patient);
        service.end("not a token"); // nothing happens

        service.endAll(1L, SessionEndReason.LOGOUT_ALL);
        assertThat(rows.stream().filter(t -> t.getUserId() == 1L).allMatch(t -> t.getEndedAt() != null)).isTrue();
        assertThat(service.refresh(doctors).user()).as("other users keep theirs").isSameAs(doctor);

        refused(null);
        refused("");
    }
}
