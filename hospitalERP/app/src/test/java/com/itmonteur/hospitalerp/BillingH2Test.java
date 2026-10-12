package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Billing (docs/BILLING_PLAN.md): consultation fees per doctor (step B.1). Same configuration as FeatureFlowH2Test,
 * so both share one application context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class BillingH2Test {

    private static final String FEES = "/api/admin/fees";

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    private TestApi api;

    @BeforeEach
    void setUpApi() {
        api = new TestApi(rest, json);
    }

    private JsonNode feeOf(String token, long doctorId) throws Exception {
        for (JsonNode d : api.get(token, FEES).get("doctors")) {
            if (d.get("doctorId").asLong() == doctorId) {
                return d;
            }
        }
        throw new AssertionError("doctor " + doctorId + " not in the fee list");
    }

    private ResponseEntity<String> setFee(String token, long doctorId, String amount) {
        return api.call(HttpMethod.PUT, FEES + "/" + doctorId, token, "{\"consultationFee\":" + amount + "}");
    }

    @Test
    void adminsSetEachDoctorsConsultationFee() throws Exception {
        String admin = api.admin();
        TestApi.Doctor doctor = api.doctorWithSchedule("drfees");

        JsonNode fees = api.get(admin, FEES);
        assertThat(fees.get("defaultFee").asDouble()).as("BILLING_DEFAULT_FEE is not set").isZero();
        JsonNode mine = feeOf(admin, doctor.id());
        assertThat(mine.get("doctorName").asText()).as("the user name while the profile has none").isEqualTo("drfees");
        assertThat(mine.get("consultationFee").isNull()).as("no fee yet").isTrue();
        assertThat(mine.get("active").asBoolean()).isTrue();

        ResponseEntity<String> set = setFee(admin, doctor.id(), "500");
        assertThat(set.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(set.getBody()).contains("\"consultationFee\":500.00");
        assertThat(feeOf(admin, doctor.id()).get("consultationFee").asDouble()).isEqualTo(500.0);
        assertThat(setFee(admin, doctor.id(), "750.5").getBody()).contains("\"consultationFee\":750.50");
        assertThat(setFee(admin, doctor.id(), "0").getStatusCode()).as("free consultations").isEqualTo(HttpStatus.OK);

        // A deactivated doctor stays in the list, marked
        api.call(HttpMethod.PUT, "/api/admin/users/" + doctor.userId() + "/deactivate", admin, null);
        assertThat(feeOf(admin, doctor.id()).get("active").asBoolean()).isFalse();
    }

    @Test
    void wrongFeesAndOtherRolesAreRefused() throws Exception {
        String admin = api.admin();
        long doctorId = api.doctorWithSchedule("drfees2").id();

        for (String wrong : List.of("-1", "100000.01", "12.345", "null")) {
            ResponseEntity<String> res = setFee(admin, doctorId, wrong);
            assertThat(res.getStatusCode()).as(wrong).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(setFee(admin, doctorId, "100000").getStatusCode()).as("the highest fee").isEqualTo(HttpStatus.OK);
        assertThat(setFee(admin, 987654, "500").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        for (String token : List.of(api.staff("deskfees", "RECEPTIONIST"), api.login("drfees2", "doctor-123"),
                api.registerPatient("feespeek"))) {
            assertThat(api.call(HttpMethod.GET, FEES, token, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(setFee(token, doctorId, "1").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
