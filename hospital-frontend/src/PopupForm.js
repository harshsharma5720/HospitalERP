import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Lottie from "lottie-react";
import heartbeatAnimation from "./assets/HeartbeatLottieAnimation.json";
import "./App.css";

export default function PopupForm() {
  const [showPopup, setShowPopup] = useState(false);
  const navigate = useNavigate();

  // Show popup after 2 seconds, only if user is not logged in
  useEffect(() => {
    const token = localStorage.getItem("jwtToken");
    console.log("Token from localStorage:", token);

    if (!token) {
      console.log("No token found → showing popup soon...");
      const timer = setTimeout(() => {
        console.log("Triggering popup now");
        setShowPopup(true);
      }, 2000);
      return () => clearTimeout(timer);
    } else {
      console.log("Token exists → not showing popup");
    }
  }, []);

  // Stop background scroll when popup is open
  useEffect(() => {
    if (showPopup) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "auto";
    }

    return () => {
      document.body.style.overflow = "auto";
    };
  }, [showPopup]);

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
        <div className="popup-card animate-scaleUp relative overflow-hidden">
          {/*  Background Lottie Animation (behind content) */}
          <div className="absolute inset-0 opacity-30 pointer-events-none">
            <Lottie
              animationData={heartbeatAnimation}
              loop={true}
              autoplay={true}
              className="w-full h-full object-cover"
            />
          </div>

          {/* Close Button */}
          <button
            className="close-btn relative z-10"
            onClick={() => setShowPopup(false)}
          >
            &times;
          </button>

          <div className="relative z-10 text-center">
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
    </div>
  );
}
