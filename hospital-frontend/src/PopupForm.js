import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./App.css";

export default function PopupForm() {
  const [showPopup, setShowPopup] = useState(false);
  const navigate = useNavigate();

  // Show popup after 2 seconds, only if user is not logged in
  useEffect(() => {
    const token = localStorage.getItem("jwtToken"); //  token stored during login

    if (!token) {
      const timer = setTimeout(() => {
        setShowPopup(true);
      }, 2000);

      return () => clearTimeout(timer);
    }
  }, []);

  if (!showPopup) return null;

  return (
    <div
      className="popup-background"
      onClick={(e) => {
        if (e.target.classList.contains("popup-background")) setShowPopup(false);
      }}
    >
      {/* Dark overlay on top of the image */}
      <div className="popup-overlay">
        <div className="popup-card animate-scaleUp">
          {/* Close Button */}
          <button
            className="close-btn"
            onClick={() => setShowPopup(false)}
          >
            &times;
          </button>

          <h2>🏥 Welcome</h2>
          <p>You are a registered user?</p>
          <p>If not, then register first.</p>

          <button
            className="popup-btn btn-login"
            onClick={() => navigate("/login")}
          >
            Login
          </button>

          <button
            className="popup-btn btn-register"
            onClick={() => navigate("/register")}
          >
            Register
          </button>
        </div>
      </div>
    </div>
  );
}
