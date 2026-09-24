// Part 1 of 2 — AppointmentPage (neon dark theme) - TOP
import React, { useState, useEffect } from "react";
import axios from "axios";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import { useLocation, useNavigate } from "react-router-dom";
import { getUserIdFromToken } from "./utils/jwtUtils";
import { calculateAgeFromDOB } from "./utils/calculateAgeFromDOB";
import Loader from "./components/common/Loader";
import { toLocalISODate, addDays } from "./utils/dateUtils";
import { getErrorMessage } from "./utils/apiError";
import { API_BASE_URL } from "./config";

export default function AppointmentPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const doctorName = location.state?.doctorName || "";
  const doctorId = location.state?.doctorId || "";
  const rescheduleData = location.state?.rescheduleAppointment || null;
  const [bookingStatus, setBookingStatus] = useState("idle");

  const emptyForm = {
    patientKey: "", // "USER-<patientId>" or "RELATIVE-<relativeId>"
    patientName: "",
    gender: "MALE",
    age: "",
    doctorId: doctorId,
    doctorName: doctorName,
    shift: "MORNING",
    date: "",
    message: "",
    ptInfoId: "",
    relativeId: "",
  };
  const [formData, setFormData] = useState(emptyForm);

  const [doctors, setDoctors] = useState([]);
  const [availableSlots, setAvailableSlots] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [slotError, setSlotError] = useState("");
  const [selectedSlotId, setSelectedSlotId] = useState(null);
  const [patientOptions, setPatientOptions] = useState([]);

  // Dates are handled as local "yyyy-MM-dd" strings (toISOString would use UTC)
  const todayStr = toLocalISODate();
  const maxDateStr = toLocalISODate(addDays(new Date(), 6));
  const [selectedDate, setSelectedDate] = useState(todayStr);

  // Fetch doctors and prefill doctor if passed from DoctorPage
  useEffect(() => {
    const fetchDoctors = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/api/patient/getAllDoctors`);
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
        alert(getErrorMessage(error, "Failed to load doctors. Please try again."));
      }
    };

    fetchDoctors();
  }, [doctorName]);

  useEffect(() => {
    const { doctorId, date, shift } = formData;
    if (doctorId && date && shift) {
      handleSlotFetch(); // auto trigger
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [formData.doctorId, formData.date, formData.shift]);

  useEffect(() => {
    setFormData((prev) => ({
      ...prev,
      date: selectedDate,
    }));
  }, [selectedDate]);


  // Prefill reschedule data if available
  useEffect(() => {
    if (rescheduleData) {
      setFormData((prev) => ({
        ...prev,
        patientKey: rescheduleData.relativeId
          ? `RELATIVE-${rescheduleData.relativeId}`
          : `USER-${rescheduleData.ptInfoId}`,
        patientName: rescheduleData.patientName || "",
        gender: rescheduleData.gender || prev.gender,
        age: rescheduleData.age || "",
        doctorId: rescheduleData.doctorId || prev.doctorId,
        doctorName: rescheduleData.doctorName || "",
        message: rescheduleData.message || "",
        ptInfoId: rescheduleData.ptInfoId || "",
        relativeId: rescheduleData.relativeId || "",
        shift: rescheduleData.shift || "MORNING",
      }));
    }
  }, [rescheduleData]);

  useEffect(() => {
    const fetchPatients = async () => {
      try {
        const token = localStorage.getItem("jwtToken");
        if (!token) return;
        // 1. Fetch logged-in user
        const userId = getUserIdFromToken(token);
        const userRes = await axios.get(`${API_BASE_URL}/api/patient/getAccount/${userId}`);
        const user = userRes.data;
        // 2. Fetch all relatives of user
        const relRes = await axios.get(`${API_BASE_URL}/api/patient/relative/patient/${user.patientId}`);
        const relatives = relRes.data;
        // 3. Dropdown options = user + relatives
        const options = [
          {
            key: `USER-${user.patientId}`,
            patientId: user.patientId,
            relativeId: null,
            name: user.patientName,
            gender: user.gender,
            age: calculateAgeFromDOB(user.dob),
          },
          ...relatives.map((rel) => ({
            key: `RELATIVE-${rel.id}`,
            patientId: user.patientId,
            relativeId: rel.id,
            name: `${rel.name} (${rel.relationship?.toLowerCase() || "relative"})`,
            gender: rel.gender,
            age: calculateAgeFromDOB(rel.dob),
          })),
        ];

        setPatientOptions(options);
      } catch (error) {
        console.error("Error fetching patient list:", error);
      }
    };

    fetchPatients();
  }, []);


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

  const handlePatientSelect = (key) => {
    const selected = patientOptions.find((p) => p.key === key);
    if (!selected) return;

    setFormData((prev) => ({
      ...prev,
      patientKey: selected.key,
      patientName: selected.name,
      gender: selected.gender || prev.gender,
      age: selected.age === "" ? prev.age : selected.age,
      ptInfoId: selected.patientId,
      relativeId: selected.relativeId || "",
    }));
  };

  const changeDate = (direction) => {
    const next = toLocalISODate(addDays(new Date(`${selectedDate}T00:00:00`), direction === "prev" ? -1 : 1));
    if (next >= todayStr && next <= maxDateStr) {
      setSelectedDate(next);
    }
  };



  // Fetch available slots (the server creates them on first request)
  const handleSlotFetch = async () => {
    const { doctorId, date, shift } = formData;
    if (!doctorId || !date || !shift) {
      alert("Please select doctor, date, and shift first.");
      return;
    }

    setLoadingSlots(true);
    setSlotError("");
    setSelectedSlotId(null);

    try {
      const response = await axios.get(`${API_BASE_URL}/api/slots/available/${doctorId}`, {
        params: { date, shift },
      });
      setAvailableSlots(response.data);
    } catch (error) {
      setAvailableSlots([]);
      setSlotError(getErrorMessage(error, "Unable to load slots. Please try again."));
    } finally {
      setLoadingSlots(false);
    }
  };

  // Handle appointment booking / rescheduling
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!selectedSlotId) {
      alert("Please select a slot before submitting.");
      return;
    }

    try {
      setBookingStatus("loading");
      if (rescheduleData) {
        // Moves the existing appointment to the new slot (the old slot is released by the server)
        await axios.put(`${API_BASE_URL}/appointment/update/${rescheduleData.appointmentID}`, {
          slotId: selectedSlotId,
          message: formData.message,
        });
      } else {
        await axios.post(`${API_BASE_URL}/appointment/NewAppointment`, {
          patientName: formData.patientName,
          gender: formData.gender,
          age: Number(formData.age) || 0,
          message: formData.message,
          relativeId: formData.relativeId ? Number(formData.relativeId) : null,
          slotId: selectedSlotId,
        });
      }
      setBookingStatus("success");

      setTimeout(() => {
        setBookingStatus("idle");
        if (rescheduleData) {
          navigate("/appointment-details");
        }
      }, 2000);

      setFormData({ ...emptyForm, doctorId: "", doctorName: "", date: selectedDate });
      setAvailableSlots([]);
      setSelectedSlotId(null);
    } catch (error) {
      setBookingStatus("idle");
      alert(getErrorMessage(error, "Failed to book appointment. Please try again."));
      // The slot may have been taken meanwhile - refresh the list
      handleSlotFetch();
    }
  };

  return (
  <>
      {/* Booking Loader Overlay */}
      {bookingStatus === "loading" && (
        <Loader
          type="heartbeat"
          text={rescheduleData ? "Rescheduling your appointment..." : "Booking your appointment..."}
        />
      )}

      {bookingStatus === "success" && (
        <Loader
          type="success"
          text={rescheduleData ? "Your appointment has been rescheduled!" : "Your appointment is booked successfully!"}
        />
      )}
    <div className="min-h-screen bg-white dark:bg-[#0a1124] text-gray-900 dark:text-[#50d4f2] transition-all">
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
          className="bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF] animate-scaleUp dark:from-[#111a3b] dark:to-[#0a1330] shadow-2xl rounded-3xl p-8 md:p-10 w-full max-w-6xl relative z-10 grid md:grid-cols-2 gap-10 transition"
        >
          {/* Form Section */}
          <div>
            <h2 className="text-3xl font-bold text-center text-[#1E63DB] dark:text-[#50d4f2] mb-6">
              {rescheduleData ? "Reschedule Appointment" : "Book an Appointment"}
            </h2>

            <form onSubmit={handleSubmit} className="space-y-5">
             <select
               name="patientKey"
               value={formData.patientKey}
               onChange={(e) => handlePatientSelect(e.target.value)}
               disabled={!!rescheduleData}
               className="w-full p-3 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2]"
               required
             >
               <option value="">Select Patient</option>
               {patientOptions.map((p) => (
                 <option key={p.key} value={p.key}>
                   {p.name}
                 </option>
               ))}
             </select>

              <select
                name="gender"
                value={formData.gender}
                onChange={handleChange}
                disabled={!!rescheduleData}
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
                min="0"
                max="120"
                value={formData.age}
                onChange={handleChange}
                disabled={!!rescheduleData}
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
                    {doctor.name}
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

              <div className="space-y-1 relative">
                <label className="block text-xs text-gray-600 font-medium">Date</label>
                <div className="flex items-center gap-2 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a]">
                  {/* Left Arrow */}
                  <button
                    type="button"
                    onClick={() => changeDate("prev")}
                    className="px-2 py-1 bg-gray-200 rounded-lg hover:bg-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a]"
                  >
                    ◀
                  </button>

                  {/* Date Input */}
                  <input
                    type="date"
                    name="date"
                    value={selectedDate}
                    onChange={(e) => setSelectedDate(e.target.value)}
                    min={todayStr}
                    max={maxDateStr}
                    className="w-full bg-white p-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-[#4CAF50] dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a]"
                  />

                  {/* Right Arrow */}
                  <button
                    type="button"
                    onClick={() => changeDate("next")}
                    className="px-2 py-1 bg-gray-200 rounded-lg hover:bg-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a]"
                  >
                    ▶
                  </button>

                </div>
              </div>

              <textarea
                name="message"
                value={formData.message}
                onChange={handleChange}
                rows="3"
                placeholder="Enter message (optional)"
                className="w-full p-3 border border-gray-300 dark:border-[#16224a] rounded bg-white dark:bg-[#0f172a] text-black dark:text-[#50d4f2] focus:ring-2 focus:ring-[#50d4f2]"
              ></textarea>


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
                    type="button"
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
                {slotError || "No slots available. Please select doctor, date & shift."}
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
    </>
  );
}
