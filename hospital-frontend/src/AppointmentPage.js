import React, { useState, useEffect } from "react";
import axios from "axios";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";

export default function AppointmentPage() {
  const [formData, setFormData] = useState({
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

  const [doctors, setDoctors] = useState([]);
  const [availableSlots, setAvailableSlots] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [selectedSlotId, setSelectedSlotId] = useState(null);

  // ✅ Fetch all doctors on load
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
      } catch (error) {
        console.error("Error fetching doctors:", error);
        alert("Failed to load doctors. Please try again.");
      }
    };
    fetchDoctors();
  }, []);

  // ✅ Handle input changes
  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  // ✅ Handle doctor dropdown change
  const handleDoctorChange = (e) => {
    const selected = doctors.find(
      (doc) => doc.id === Number(e.target.value)
    );
    if (selected) {
      setFormData({
        ...formData,
        doctorId: selected.id,
        doctorName: selected.name,
      });
    }
  };

  // ✅ Generate & Fetch available slots
  const handleSlotFetch = async () => {
    const { doctorId, date, shift } = formData;
    if (!doctorId || !date || !shift) {
      alert("Please select doctor, date, and shift first.");
      return;
    }

    setLoadingSlots(true);
    const token = localStorage.getItem("jwtToken");

    try {
      // Step 1: Generate slots for selected doctor/date/shift
      await axios.post(
        `http://localhost:8080/api/slots/generate/${doctorId}`,
        null,
        {
          params: { date, shift },
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      // Step 2: Fetch available slots
      const response = await axios.get(
        `http://localhost:8080/api/slots/available/${doctorId}`,
        {
          params: { date, shift },
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      console.log("🎯 Available slots received:", response.data);

      setAvailableSlots(response.data);
    } catch (error) {
      console.error("Error fetching slots:", error);
      alert("Unable to load slots. Please try again.");
    } finally {
      setLoadingSlots(false);
    }
  };

  // ✅ Handle form submit (book appointment)
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

      alert("Appointment booked successfully!");
      localStorage.setItem("appointmentData", JSON.stringify(payload));

      // Reset form
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
    <div className="min-h-screen bg-gray-50 relative">
      <TopNavbar />
      <Navbar />

      <div className="flex justify-center items-start py-12 px-6 relative">
        <img
          src="Shreyahospital.jpg"
          alt="Hospital Background"
          className="absolute top-0 left-0 w-full h-full object-cover opacity-20 -z-10"
        />

        <div className="bg-white bg-opacity-95 backdrop-blur-md shadow-2xl rounded-2xl p-8 w-full max-w-6xl relative z-10 grid md:grid-cols-2 gap-10">
          {/* Form Section */}
          <div>
            <h2 className="text-3xl font-bold text-center text-teal-700 mb-6">
              Book an Appointment
            </h2>

            <form onSubmit={handleSubmit} className="space-y-5">
              <input
                type="text"
                name="patientName"
                value={formData.patientName}
                onChange={handleChange}
                placeholder="Patient Name"
                className="w-full p-3 border border-gray-300 rounded focus:ring-2 focus:ring-teal-600"
                required
              />

              <select
                name="gender"
                value={formData.gender}
                onChange={handleChange}
                className="w-full p-3 border border-gray-300 rounded focus:ring-2 focus:ring-teal-600"
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
                className="w-full p-3 border border-gray-300 rounded focus:ring-2 focus:ring-teal-600"
                required
              />

              <select
                name="doctorId"
                value={formData.doctorId}
                onChange={handleDoctorChange}
                className="w-full p-3 border border-gray-300 rounded focus:ring-2 focus:ring-teal-600"
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
                className="w-full p-3 border border-gray-300 rounded focus:ring-2 focus:ring-teal-600"
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
                className="w-full p-3 border border-gray-300 rounded focus:ring-2 focus:ring-teal-600"
                required
              />

              <textarea
                name="message"
                value={formData.message}
                onChange={handleChange}
                placeholder="Message (optional)"
                className="w-full p-3 border border-gray-300 rounded focus:ring-2 focus:ring-teal-600"
              />

              <input
                type="number"
                name="ptInfoId"
                value={formData.ptInfoId}
                onChange={handleChange}
                placeholder="Patient Info ID (optional)"
                className="w-full p-3 border border-gray-300 rounded focus:ring-2 focus:ring-teal-600"
              />

              <button
                type="button"
                onClick={handleSlotFetch}
                className="w-full bg-teal-600 text-white py-3 rounded-lg font-semibold hover:bg-teal-700 transition"
              >
                Show Available Slots
              </button>

              <button
                type="submit"
                className="w-full bg-green-600 text-white py-3 rounded-lg font-semibold hover:bg-green-700 transition"
              >
                Book Appointment
              </button>
            </form>
          </div>

          {/* Slots Section */}
          <div>
            <h3 className="text-2xl font-semibold text-center text-teal-700 mb-4">
              Available Slots
            </h3>

            {loadingSlots ? (
              <p className="text-center text-teal-700 font-semibold">
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
                        ? "bg-teal-700 text-white"
                        : "bg-gray-100 hover:bg-teal-100"
                    }`}
                  >
                    {slot.startTime} - {slot.endTime}
                  </button>
                ))}
              </div>
            ) : (
              <p className="text-center text-gray-500">
                No slots available. Please select doctor, date & shift.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
