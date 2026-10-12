import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import useAuthStore from "../auth/useAuthStore";
import PopupForm from "../../shared/PopupForm";

// Booking a doctor from the public site: a logged-in user goes to the booking page with the doctor chosen; a
// visitor is asked to log in first. Render loginPrompt somewhere on the page.
export default function useBooking() {
  const role = useAuthStore((s) => s.role);
  const navigate = useNavigate();
  const [askLogin, setAskLogin] = useState(false);

  const book = (doctor) => {
    if (!role) {
      setAskLogin(true);
      return;
    }
    navigate("/appointments", { state: { doctorName: doctor.name } });
  };

  const loginPrompt = askLogin ? <PopupForm onClose={() => setAskLogin(false)} /> : null;
  return { book, loginPrompt };
}
