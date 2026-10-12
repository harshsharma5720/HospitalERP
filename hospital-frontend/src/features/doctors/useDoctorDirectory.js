import { useCallback, useEffect, useState } from "react";
import { getAllDoctors } from "./api";

// The public doctor directory, without deactivated doctors: { doctors (null until loaded or when it failed),
// loading, error, retry }
export default function useDoctorDirectory() {
  const [state, setState] = useState({ doctors: null, loading: true, error: false });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let current = true;
    setState((previous) => ({ ...previous, loading: true, error: false }));
    getAllDoctors()
      .then((response) => {
        if (current) setState({ doctors: (response.data || []).filter((d) => d.active !== false), loading: false, error: false });
      })
      .catch(() => {
        if (current) setState({ doctors: null, loading: false, error: true });
      });
    return () => {
      current = false;
    };
  }, [attempt]);

  const retry = useCallback(() => setAttempt((n) => n + 1), []);
  return { ...state, retry };
}
