import { useState, useEffect, useCallback } from "react";
export function useResource(loader, dependencies = []) {
  const [state, setState] = useState({
    data: null,
    loading: true,
    error: null,
  });
  const [version, setVersion] = useState(0);
  useEffect(() => {
    let active = true;
    setState((previous) => ({ ...previous, loading: true, error: null }));
    Promise.resolve()
      .then(loader)
      .then((data) => {
        if (active) setState({ data, loading: false, error: null });
      })
      .catch((error) => {
        if (active) setState({ data: null, loading: false, error });
      });
    return () => {
      active = false;
    };
    // Callers supply the identity dependencies for their resource.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependencies, version]);
  const reload = useCallback(() => setVersion((value) => value + 1), []);
  return { ...state, reload };
}
