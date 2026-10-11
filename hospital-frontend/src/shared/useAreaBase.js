import { useLocation } from "react-router-dom";

// "/admin" inside the admin portal, "" elsewhere: front-desk pages run in both and link within their own area
export default function useAreaBase() {
  return useLocation().pathname.startsWith("/admin") ? "/admin" : "";
}
