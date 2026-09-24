import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import axios from "axios";
import ConsultationModal from "./ConsultationModal";

jest.mock("axios");

const appointment = { appointmentID: 12, ptInfoId: 3, patientName: "Asha", date: "2026-03-10", message: "Fever" };

afterEach(() => jest.resetAllMocks());

test("new consultation: sends vitals as numbers, skips empty medicine rows and completes", async () => {
  axios.get.mockImplementation((url) =>
    url.includes("/patient/")
      ? Promise.resolve({ data: [{ id: 1, appointmentId: 5, appointmentDate: "2026-01-02", doctorName: "Rao", diagnosis: "Cold", medicines: [] }] })
      : Promise.reject({ response: { status: 404 } })
  );
  axios.put.mockResolvedValue({ data: {} });
  const onSaved = jest.fn();
  render(<ConsultationModal appointment={appointment} onClose={() => {}} onSaved={onSaved} />);

  expect(await screen.findByText("▶ Previous visits (1)")).toBeInTheDocument();

  fireEvent.change(screen.getByLabelText("Pulse (bpm)"), { target: { value: "72" } });
  fireEvent.change(screen.getByLabelText("Diagnosis *"), { target: { value: "Viral fever" } });
  fireEvent.click(screen.getByText("+ Add medicine"));
  fireEvent.click(screen.getByText("+ Add medicine")); // left empty -> must not be sent
  fireEvent.change(screen.getAllByLabelText("Medicine name")[0], { target: { value: "Paracetamol" } });
  fireEvent.click(screen.getByText("Save & Complete"));

  await waitFor(() => expect(onSaved).toHaveBeenCalledWith(12));
  const [url, payload] = axios.put.mock.calls[0];
  expect(url).toContain("/api/consultations/appointment/12");
  expect(payload.pulse).toBe(72);
  expect(payload.temperature).toBeNull();
  expect(payload.diagnosis).toBe("Viral fever");
  expect(payload.medicines).toHaveLength(1);
  expect(payload.medicines[0].medicineName).toBe("Paracetamol");
});
