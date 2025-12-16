import React from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ResponsiveContainer,
} from "recharts";

export default function AdminDashboard() {
  const chartData = [
    { name: "Mon", value: 300 },
    { name: "Tue", value: 420 },
    { name: "Wed", value: 390 },
    { name: "Thu", value: 480 },
    { name: "Fri", value: 450 },
    { name: "Sat", value: 520 },
    { name: "Sun", value: 600 },
  ];

  return (
    <div className="w-full">
      {/* PAGE TITLE */}
      <h1 className="text-3xl font-bold mb-6">Admin Dashboard</h1>

      {/* SALES OVERVIEW CARD */}
      <div className="bg-white dark:bg-[#1e293b] rounded-2xl shadow-lg p-6 mb-6">
        <div className="flex justify-between items-start">
          <div>
            <h2 className="text-lg font-semibold text-gray-700 dark:text-gray-300">
              Sales Value
            </h2>
            <p className="text-4xl font-bold mt-2">$10,567</p>
            <p className="text-green-600 text-sm">↑ 10.57% Since yesterday</p>
          </div>

          <div className="flex gap-2">
            <button className="px-3 py-1 text-sm rounded-lg bg-[#0b1f36] text-white">
              Month
            </button>
            <button className="px-3 py-1 text-sm rounded-lg bg-gray-200 dark:bg-gray-700">
              Week
            </button>
          </div>
        </div>

        {/* LINE CHART */}
        <div className="h-64 mt-6">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" opacity={0.2} />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Line
                type="monotone"
                dataKey="value"
                stroke="#0ea5e9"
                strokeWidth={3}
                dot={{ r: 5 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* 3 CARDS: CUSTOMERS / REVENUE / TRAFFIC */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        {/* Customers */}
        <div className="bg-white dark:bg-[#1e293b] rounded-2xl shadow p-6">
          <h3 className="text-lg font-semibold mb-2">Customers</h3>
          <p className="text-3xl font-bold">345k</p>
          <p className="text-green-600 text-sm">↑ 18.2% Since last month</p>
        </div>

        {/* Revenue */}
        <div className="bg-white dark:bg-[#1e293b] rounded-2xl shadow p-6">
          <h3 className="text-lg font-semibold mb-2">Revenue</h3>
          <p className="text-3xl font-bold">$43,594</p>
          <p className="text-green-600 text-sm">↑ 28.4% Since last month</p>
        </div>

        {/* Traffic Share */}
        <div className="bg-white dark:bg-[#1e293b] rounded-2xl shadow p-6">
          <h3 className="text-lg font-semibold mb-2">Traffic Share</h3>

          <div className="mt-4 space-y-2 text-sm">
            <p>
              🖥️ Desktop <span className="float-right font-semibold">60%</span>
            </p>
            <p>
              📱 Mobile <span className="float-right font-semibold">30%</span>
            </p>
            <p>
              📟 Tablet <span className="float-right font-semibold">10%</span>
            </p>
          </div>
        </div>
      </div>

      {/* PAGE VISITS + TOTAL ORDER SECTION */}
      <div className="bg-white dark:bg-[#1e293b] rounded-2xl shadow p-6">
        <h3 className="text-lg font-semibold">Page Visits</h3>

        <div className="mt-4 flex justify-between">
          <p className="text-2xl font-bold">452</p>
          <button className="px-3 py-1 bg-[#0b1f36] text-white rounded-lg">
            See all
          </button>
        </div>
      </div>
    </div>
  );
}
