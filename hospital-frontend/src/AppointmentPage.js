// Part 1 of 2 — AppointmentPage (neon dark theme) - TOP
import React, { useState, useEffect } from "react";
import axios from "axios";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import { useLocation } from "react-router-dom";

export default function AppointmentPage() {
  const location = useLocation();
  const doctorName = location.state?.doctorName || "";
  const doctorId = location.state?.doctorId || "";
  const rescheduleData = location.state?.rescheduleAppointment || null;

  const [formData, setFormData] = useState({
    patientName: "",
    gender: "MALE",
    age: "",
    doctorId: doctorId,
    doctorName: doctorName,
    shift: "MORNING",
    date: "",
    message: "",
    ptInfoId: "",
    slotId: "",
  });

  const [doctors, setDoctors] = useState([]);
  const [availableSlots, setAvailableSlots] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [selectedSlotId, setSelectedSlotId] = useState(null);

  // Fetch doctors and prefill doctor if passed from DoctorPage
  useEffect(() => {
    const fetchDoctors = async () => {
      try {
        const token = localStorage.getItem("jwtToken");
        if (!token) {
          alert("Please login first!");
          return;
        }

        const response = await axios.get(
          "http://localhost:8080/api/patient/getAllDoctors",
          { headers: { Authorization: `Bearer ${token}` } }
        );
        setDoctors(response.data);

        // Prefill doctor if passed from DoctorPage
        if (doctorName) {
          const selectedDoctor = response.data.find(
            (doc) => doc.name.toLowerCase() === doctorName.toLowerCase()
          );
          if (selectedDoctor) {
            setFormData((prev) => ({
              ...prev,
              doctorId: selectedDoctor.id,
              doctorName: selectedDoctor.name,
            }));
          }
        }
      } catch (error) {
        console.error("Error fetching doctors:", error);
        alert("Failed to load doctors. Please try again.");
      }
    };

    fetchDoctors();
  }, [doctorName]);

  // Prefill reschedule data if available
  useEffect(() => {
    if (rescheduleData) {
      setFormData((prev) => ({
        ...prev,
        patientName: rescheduleData.patientName || "",
        doctorName: rescheduleData.doctorName || "",
        message: rescheduleData.message || "",
        ptInfoId: rescheduleData.ptInfoId || "",
        shift: rescheduleData.shift || "MORNING",
        date: "",
      }));

      if (rescheduleData.doctorName && doctors.length > 0) {
        const selectedDoctor = doctors.find(
          (doc) =>
            doc.name.toLowerCase() === rescheduleData.doctorName.toLowerCase()
        );
        if (selectedDoctor) {
          setFormData((prev) => ({
            ...prev,
            doctorId: selectedDoctor.id,
            doctorName: selectedDoctor.name,
          }));
        }
      }
    }
  }, [rescheduleData, doctors]);

  // Handle input changes
  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  // Handle doctor dropdown
  const handleDoctorChange = (e) => {
    const selected = doctors.find((doc) => doc.id === Number(e.target.value));
    if (selected) {
      setFormData({
        ...formData,
        doctorId: selected.id,
        doctorName: selected.name,
      });
    }
  };

  // Fetch available slots
  const handleSlotFetch = async () => {
    const { doctorId, date, shift } = formData;
    if (!doctorId || !date || !shift) {
      alert("Please select doctor, date, and shift first.");
      return;
    }

    setLoadingSlots(true);
    const token = localStorage.getItem("jwtToken");

    try {
      await axios.post(
        `http://localhost:8080/api/slots/generate/${doctorId}`,
        null,
        {
          params: { date, shift },
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      const response = await axios.get(
        `http://localhost:8080/api/slots/available/${doctorId}`,
        {
          params: { date, shift },
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      setAvailableSlots(response.data);
    } catch (error) {
      console.error("Error fetching slots:", error);
      alert("Unable to load slots. Please try again.");
    } finally {
      setLoadingSlots(false);
    }
  };

  // Handle appointment booking / rescheduling
  const handleSubmit = async (e) => {
    e.preventDefault();
    const token = localStorage.getItem("jwtToken");
    if (!token) {
      alert("Please login first!");
      return;
    }

    if (!selectedSlotId) {
      alert("Please select a slot before submitting.");
      return;
    }

    try {
      const payload = {
        ...formData,
        age: Number(formData.age),
        ptInfoId: formData.ptInfoId ? Number(formData.ptInfoId) : null,
        slotId: selectedSlotId,
      };

      const response = await axios.post(
        "http://localhost:8080/appointment/NewAppointment",
        payload,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      alert(
        rescheduleData
          ? "Appointment rescheduled successfully!"
          : "Appointment booked successfully!"
      );

      setFormData({
        patientName: "",
        gender: "MALE",
        age: "",
        doctorId: "",
        doctorName: "",
        shift: "MORNING",
        date: "",
        message: "",
        ptInfoId: "",
        slotId: "",
      });
      setAvailableSlots([]);
      setSelectedSlotId(null);
    } catch (error) {
      console.error("Error submitting appointment:", error);
      alert("Failed to book appointment. Please try again.");
    }
  };

  return (
    <div className="min-h-screen bg-white dark:bg-[#0a1330] text-gray-900 dark:text-[#50d4f2] transition-all">
      <TopNavbar />
      <Navbar />

      <div className="flex justify-center items-start py-12 px-6 relative">
        {/* subtle decorative background image with low opacity */}
        <img
          src="Shreyahospital.jpg"
          alt="Hospital Background"
          className="absolute top-0 left-0 w-full h-full object-cover opacity-10 dark:opacity-20 -z-10"
        />

        <div
          className="bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF] dark:bg-[#111a3b]/80 shadow-2xl rounded-3xl p-8 md:p-10 w-full max-w-6xl relative z-10 grid md:grid-cols-2 gap-10 transition"
        >
          {/* Form Section */}
          <div>
            <h2 className="text-3xl font-bold text-center text-[#1E63DB] dark:text-[#50d4f2] mb-6">
              {rescheduleData ? "Reschedule Appointment" : "Book an Appointment"}
            </h2>

            <form onSubmit={handleSubmit} className="space-y-5">
              <input
                type="text"
                name="patientName"
                value={formData.patientName}
                onChange={handleChange}
                placeholder="Patient Name"
                className="w-full p-3 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2] placeholder-gray-500 dark:placeholder-gray-400 focus:ring-2 focus:ring-[#50d4f2]"
                required
              />

              <select
                name="gender"
                value={formData.gender}
                onChange={handleChange}
                className="w-full p-3 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2] focus:ring-2 focus:ring-[#50d4f2]"
                required
              >
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>

              <input
                type="number"
                name="age"
                value={formData.age}
                onChange={handleChange}
                placeholder="Age"
                className="w-full p-3 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2] focus:ring-2 focus:ring-[#50d4f2]"
                required
              />

              <select
                name="doctorId"
                value={formData.doctorId}
                onChange={handleDoctorChange}
                className="w-full p-3 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2] focus:ring-2 focus:ring-[#50d4f2]"
                required
              >
                <option value="">Select Doctor</option>
                {doctors.map((doctor) => (
                  <option key={doctor.id} value={doctor.id}>
                    {doctor.name} — {doctor.specialist}
                  </option>
                ))}
              </select>

              <select
                name="shift"
                value={formData.shift}
                onChange={handleChange}
                className="w-full p-3 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2] focus:ring-2 focus:ring-[#50d4f2]"
                required
              >
                <option value="MORNING">Morning</option>
                <option value="EVENING">Evening</option>
              </select>

              <input
                type="date"
                name="date"
                value={formData.date}
                onChange={handleChange}
                className="w-full p-3 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2] focus:ring-2 focus:ring-[#50d4f2]"
                required
              />

              <textarea
                name="message"
                value={formData.message}
                onChange={handleChange}
                placeholder="Message (optional)"
                className="w-full p-3 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2] focus:ring-2 focus:ring-[#50d4f2]"
              />

              <input
                type="number"
                name="ptInfoId"
                value={formData.ptInfoId}
                onChange={handleChange}
                placeholder="Patient Info ID (optional)"
                className="w-full p-3 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2] focus:ring-2 focus:ring-[#50d4f2]"
              />

              <button
                type="button"
                onClick={handleSlotFetch}
                className="w-full bg-gradient-to-br from-[#1E63DB] to-[#27496d] dark:from-[#50d4f2] dark:to-[#3bc2df] text-white dark:text-black py-3 rounded-lg font-semibold hover:opacity-90 transition"
              >
                Show Available Slots
              </button>

              <button
                type="submit"
                className="w-full bg-green-600 text-white dark:bg-green-600 py-3 rounded-lg font-semibold hover:bg-green-700 transition"
              >
                {rescheduleData ? "Reschedule Appointment" : "Book Appointment"}
              </button>
            </form>
          </div>

          {/* Slots Section */}
          <div>
            <h3 className="text-2xl font-semibold text-center text-[#1E63DB] dark:text-[#50d4f2] mb-4">
              Available Slots
            </h3>

            {loadingSlots ? (
              <p className="text-center text-[#1E63DB] font-semibold dark:text-[#50d4f2]">
                Loading slots...
              </p>
            ) : availableSlots.length > 0 ? (
              <div className="grid grid-cols-3 gap-3">
                {availableSlots.map((slot) => (
                  <button
                    key={slot.id}
                    onClick={() => setSelectedSlotId(slot.id)}
                    className={`p-3 rounded-lg border text-sm font-medium transition ${
                      selectedSlotId === slot.id
                        ? "bg-[#1E63DB] text-white dark:bg-[#50d4f2] dark:text-black"
                        : "bg-gray-100 hover:bg-[#e7f0ff] dark:bg-[#0f172a] dark:hover:bg-[#16224a] dark:text-[#50d4f2]"
                    }`}
                  >
                    {slot.startTime} - {slot.endTime}
                  </button>
                ))}
              </div>
            ) : (
              <p className="text-center text-gray-500 dark:text-[#8ddff8]">
                No slots available. Please select doctor, date & shift.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
