import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Lottie from "lottie-react";
import heartbeatAnimation from "./assets/HeartbeatLottieAnimation.json";
import "./App.css";

export default function PopupForm() {
  const [showPopup, setShowPopup] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem("jwtToken");
    if (!token) {
      const timer = setTimeout(() => {
        setShowPopup(true);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, []);

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
      {/* Overlay */}
      <div className="popup-overlay">
        {/* POPUP CARD */}
        <div
          className="
            popup-card animate-scaleUp relative overflow-hidden
            bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF]
            dark:from-[#111a3b] dark:to-[#0a1330]
            rounded-3xl shadow-2xl p-8 md:p-10
            transition-all duration-300
          "
        >
          {/* Background Animation */}
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
            className="
              close-btn relative z-10
              text-gray-700 dark:text-[#50d4f2]
              hover:text-[#007B9E] dark:hover:text-[#3bc2df]
              transition
            "
            onClick={() => setShowPopup(false)}
          >
            &times;
          </button>

          {/* TEXT + BUTTONS */}
          <div className="relative z-10 text-center">
            <h2
              className="
                text-3xl font-extrabold mb-4
                bg-gradient-to-r from-blue-600 to-cyan-400
                dark:from-[#50d4f2] dark:to-[#3bc2df]
                bg-clip-text text-transparent
              "
            >
              🏥 Welcome
            </h2>

            <p className="text-gray-700 dark:text-[#8ddff8]">
              Are you a registered user?
            </p>
            <p className="text-gray-700 dark:text-[#8ddff8] mb-6">
              If not, then register first.
            </p>

            <div className="flex flex-col items-center space-y-6 mt-6">
              {/* LOGIN BUTTON */}
              <button
                onClick={() => navigate("/login")}
                className="
                  w-72 py-3 rounded-xl text-lg font-semibold shadow-lg
                  bg-gradient-to-r from-blue-600 to-cyan-400
                  dark:from-[#50d4f2] dark:to-[#3bc2df]
                  text-white dark:text-black
                  hover:scale-105 transition-transform
                "
              >
                Login
              </button>

              {/* REGISTER BUTTON */}
              <button
                onClick={() => navigate("/register")}
                className="
                  w-72 py-3 rounded-xl text-lg font-semibold shadow-lg
                  bg-gradient-to-r from-cyan-400 to-blue-600
                  dark:from-[#3bc2df] dark:to-[#50d4f2]
                  text-white dark:text-black
                  hover:scale-105 transition-transform
                "
              >
                Register
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
