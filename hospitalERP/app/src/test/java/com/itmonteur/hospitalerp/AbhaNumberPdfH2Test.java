package com.itmonteur.hospitalerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The ABHA number on the prescription and the receipt (docs/ABHA_NUMBER_PLAN.md, AB.2), read back from the PDFs the
 * app hands out. Same configuration as FeatureFlowH2Test, so both share one application context; the accounts
 * *abhapdf* and the numbers 910000000000 2x are used by no other test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:h2:mem:flows;DB_CLOSE_DELAY=-1", "DB_USERNAME=sa", "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "OTP_REQUIRED=false", "REMINDERS_ENABLED=false",
        "ADMIN_USERNAME=flowadmin", "ADMIN_PASSWORD=flow-admin-123"
})
class AbhaNumberPdfH2Test {

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    private TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(rest, json);
    }

    private long relative(String token, String name, String abha) throws Exception {
        ResponseEntity<String> res = api.call(HttpMethod.POST, "/api/patient/relative/add", token, "{\"name\":\"" + name
                + "\",\"gender\":\"MALE\",\"dob\":\"2016-02-01\",\"relationship\":\"SON\""
                + (abha == null ? "" : ",\"abhaNumber\":\"" + abha + "\"") + "}");
        assertThat(res.getStatusCode()).as(res.getBody()).isEqualTo(HttpStatus.OK);
        return json.readTree(res.getBody()).get("id").asLong();
    }

    private String pdfText(String token, String url) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<byte[]> pdf = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
        assertThat(pdf.getStatusCode()).as(url).isEqualTo(HttpStatus.OK);
        PdfReader reader = new PdfReader(pdf.getBody());
        StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(new PdfTextExtractor(reader).getTextFromPage(page)).append('\n');
        }
        return text.toString();
    }

    // Checked in (the bill is made), seen by the doctor, paid; the bill's id
    private long seenAndPaid(String desk, String doctor, long visit) throws Exception {
        assertThat(api.call(HttpMethod.POST, "/api/receptionist/appointments/" + visit + "/check-in", desk, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(api.call(HttpMethod.PUT, "/api/consultations/appointment/" + visit, doctor, "{\"diagnosis\":\"Viral fever\"}")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        long billId = api.get(desk, "/api/receptionist/bills/by-appointment/" + visit).get("id").asLong();
        assertThat(api.call(HttpMethod.POST, "/api/receptionist/bills/" + billId + "/pay", desk, "{\"method\":\"CASH\"}")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        return billId;
    }

    @Test
    void thePrescriptionAndTheReceiptShowTheNumberOfThePersonTheVisitWasFor() throws Exception {
        String desk = api.staff("deskabhapdf", "RECEPTIONIST");
        TestApi.Doctor doctor = api.doctorWithSchedule("drabhapdf");
        String doctorToken = api.login("drabhapdf", "doctor-123");
        assertThat(api.call(HttpMethod.PUT, "/api/admin/fees/" + doctor.id(), api.admin(), "{\"consultationFee\":300}").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        String neha = api.registerPatient("abhapdf");
        long nehaId = jdbc.queryForObject("SELECT p.patient_id FROM patient p JOIN users u ON u.id = p.user_id "
                + "WHERE u.username = 'abhapdf'", Long.class);
        String deskPatient = "/api/receptionist/patients/" + nehaId;
        assertThat(api.call(HttpMethod.PUT, deskPatient + "/abha", desk, "{\"abhaNumber\":\"91-0000-0000-0021\"}").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        jdbc.update("UPDATE patient SET patient_name = 'Neha Pdf' WHERE patient_id = ?", nehaId);
        long tara = relative(neha, "Tara Pdf", "91000000000022");
        long dev = relative(neha, "Dev Pdf", null);

        JsonNode evening = api.freeSlotsTomorrow(neha, doctor.id(), "EVENING");
        long taraVisit = api.bookedForToday(neha, evening.get(0).get("id").asLong(), tara, jdbc);
        long devVisit = api.bookedForToday(neha, evening.get(1).get("id").asLong(), dev, jdbc);
        long nehaVisit = api.bookedForToday(neha, api.freeSlotsTomorrow(neha, doctor.id(), "MORNING").get(0).get("id").asLong(), null, jdbc);
        long taraBill = seenAndPaid(desk, doctorToken, taraVisit);
        long devBill = seenAndPaid(desk, doctorToken, devVisit);
        long nehaBill = seenAndPaid(desk, doctorToken, nehaVisit);
        String prescription = "/api/consultations/appointment/%d/prescription.pdf";
        String receipt = "/api/receptionist/bills/%d/receipt.pdf";

        assertThat(pdfText(neha, prescription.formatted(taraVisit))).contains("Tara Pdf", "ABHA 91-0000-0000-0022")
                .doesNotContain("0021");
        assertThat(pdfText(desk, receipt.formatted(taraBill))).contains("Tara Pdf", "ABHA 91-0000-0000-0022").doesNotContain("0021");
        assertThat(pdfText(neha, prescription.formatted(nehaVisit))).contains("Neha Pdf", "ABHA 91-0000-0000-0021");
        assertThat(pdfText(desk, receipt.formatted(nehaBill))).contains("Neha Pdf", "ABHA 91-0000-0000-0021");
        assertThat(pdfText(neha, prescription.formatted(devVisit))).as("Dev has none: not his mother's either")
                .contains("Dev Pdf").doesNotContain("ABHA");
        assertThat(pdfText(desk, receipt.formatted(devBill))).doesNotContain("ABHA");

        // Added later: on what is printed later
        assertThat(api.call(HttpMethod.PUT, deskPatient + "/relatives/" + dev + "/abha", desk, "{\"abhaNumber\":\"91000000000023\"}")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdfText(neha, prescription.formatted(devVisit))).contains("ABHA 91-0000-0000-0023");
        assertThat(pdfText(neha, "/api/bills/my/%d/receipt.pdf".formatted(devBill))).as("the patient's own copy")
                .contains("ABHA 91-0000-0000-0023");

        // Tara removed from the family: her visit is no longer linked to her, and her mother's number isn't hers
        assertThat(api.call(HttpMethod.DELETE, "/api/patient/relative/delete/" + tara, neha, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdfText(neha, prescription.formatted(taraVisit))).contains("Tara Pdf").doesNotContain("ABHA");
        assertThat(pdfText(desk, receipt.formatted(taraBill))).doesNotContain("ABHA");
    }
}
