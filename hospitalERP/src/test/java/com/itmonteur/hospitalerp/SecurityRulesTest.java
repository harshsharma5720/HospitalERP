package com.itmonteur.hospitalerp;

import ITmonteur.example.hospitalERP.controller.*;
import ITmonteur.example.hospitalERP.services.*;
import ITmonteur.example.hospitalERP.configuration.JWTAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies the URL-level access rules in SecurityConfig. */
@WebMvcTest(controllers = {AppointmentController.class, DoctorController.class, AccountController.class,
        LeaveRequestController.class, AuthController.class,
        // moved endpoints (steps 1.6 / 1.7): same URLs, same rules
        DoctorAppointmentController.class, ReceptionistAppointmentController.class,
        DoctorScheduleController.class, DoctorDirectoryController.class})
@Import({SecurityConfig.class, JWTAuthenticationFilter.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000")
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AppointmentService appointmentService;
    @MockitoBean private DoctorService doctorService;
    @MockitoBean private LeaveRequestService leaveRequestService;
    @MockitoBean private AuthService authService;
    @MockitoBean private OtpService otpService;
    @MockitoBean private SmsService smsService;
    @MockitoBean private JWTService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private PasswordResetService passwordResetService;
    @MockitoBean private DoctorScheduleService doctorScheduleService;
    @MockitoBean private UserAccountService userAccountService;

    @Test
    void anonymousUserGets401OnProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/appointment/getPatientAppointments")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsTreatedAsAnonymous() throws Exception {
        when(jwtService.extractUsername("garbage")).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad"));
        mockMvc.perform(get("/appointment/getPatientAppointments").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicDoctorDirectoryNeedsNoLogin() throws Exception {
        when(doctorService.getAllDoctors()).thenReturn(List.of());
        mockMvc.perform(get("/api/doctor/getAll")).andExpect(status().isOk());
        mockMvc.perform(get("/api/doctor/getAllBySpecialization").param("specialisation", "CARDIOLOGY"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void patientCannotListAllAppointments() throws Exception {
        mockMvc.perform(get("/appointment/getAll")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void bulkDeleteEndpointNoLongerExists() throws Exception {
        mockMvc.perform(delete("/appointment/cancelALlAppointments")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistCanListAllAppointments() throws Exception {
        mockMvc.perform(get("/appointment/getAll")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCannotDeleteDoctors() throws Exception {
        mockMvc.perform(delete("/api/doctor/delete/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCannotApproveLeave() throws Exception {
        mockMvc.perform(put("/api/leaves/5/status").param("status", "APPROVED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanApproveLeave() throws Exception {
        mockMvc.perform(put("/api/leaves/5/status").param("status", "APPROVED"))
                .andExpect(status().isOk());
    }

    @Test
    void registerValidatesInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"a\",\"email\":\"not-an-email\",\"password\":\"1\",\"phoneNumber\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- moved endpoints keep their rules

    @Test
    void publicDoctorDirectoryUnderPatientPathNeedsNoLogin() throws Exception {
        when(doctorService.getAllDoctors()).thenReturn(List.of());
        mockMvc.perform(get("/api/patient/getAllDoctors")).andExpect(status().isOk());
        mockMvc.perform(get("/api/patient/getAllBySpecialization").param("specialization", "NEUROLOGY"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void patientCannotUseDoctorOrFrontDeskAppointmentEndpoints() throws Exception {
        mockMvc.perform(put("/api/doctor/complete/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctor/doctorPendingAppointments/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctor/1/schedule")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/receptionist/getAppointments")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/receptionist/deleteAppointment/1")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserGets401OnMovedEndpoints() throws Exception {
        mockMvc.perform(put("/api/doctor/complete/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/receptionist/getAppointments")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/patient/deleteAccount/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistCanUseFrontDeskEndpointsButNotDeleteStaff() throws Exception {
        mockMvc.perform(get("/api/receptionist/getAppointments")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/receptionist/delete/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCanUseTheirAppointmentAndScheduleEndpoints() throws Exception {
        mockMvc.perform(put("/api/doctor/complete/1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/doctor/1/schedule")).andExpect(status().isOk());
    }
}
