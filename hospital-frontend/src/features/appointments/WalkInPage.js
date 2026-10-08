import React from "react";
import Navbar from "../../shared/Navbar";
import TopNavbar from "../../shared/TopNavbar";
import WalkInBooking from "./WalkInBooking";

// /walk-in for receptionists (admins use /admin/walk-in inside the admin portal)
export default function WalkInPage() {
  return (
    <div className="min-h-screen bg-gray-100 dark:bg-[#0a1124] dark:text-gray-100">
      <TopNavbar />
      <Navbar />
      <WalkInBooking />
    </div>
  );
}
