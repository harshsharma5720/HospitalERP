import React from "react";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";

// A page with the site's navigation (receptionist pages; admins get the same content inside the admin portal)
export default function SitePage({ children }) {
  return (
    <div className="min-h-screen bg-gray-100 dark:bg-[#0a1124] dark:text-gray-100">
      <TopNavbar />
      <Navbar />
      {children}
    </div>
  );
}
