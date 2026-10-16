import React from "react";
import { XAxis, useChartWidth } from "recharts";
import { tickInterval } from "./dashboardFormat";

// An x axis that shows as many of its count labels as fit the chart's measured width, evenly; labelWidth: the px a
// label needs (a date about 80, an hour 44, a weekday 36). recharts 3 measures the chart (ResponsiveContainer) and
// lets a component inside it read the width.
export default function FittedXAxis({ count, labelWidth = 80, ...axis }) {
  const width = useChartWidth();
  return <XAxis {...axis} interval={tickInterval(count, width, labelWidth)} />;
}
