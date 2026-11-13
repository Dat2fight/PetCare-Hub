import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { isAxiosError } from 'axios';
import { useAuth } from './AuthContext';
import api from '../api/client';
import { getErrorMessage } from '../utils/getErrorMessage';

const Context = createContext({ permissions: [] as string[], loading: true, error: '', retry: () => {} });
export function PermissionProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [permissions, setPermissions] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [version, setVersion] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setPermissions([]); setError('');
    if (!user || user.mustChangePassword) { setLoading(false); return () => controller.abort(); }
    setLoading(true);
    api.get<{ permissions: string[] }>('/admin/me/permissions', { signal: controller.signal })
      .then(({ data }) => { if (!controller.signal.aborted) setPermissions(data.permissions); })
      .catch(error => { if (!controller.signal.aborted && !(isAxiosError(error) && error.response?.status === 403)) setError(getErrorMessage(error)); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [user, version]);
  return <Context.Provider value={{ permissions, loading, error, retry: () => setVersion(value => value + 1) }}>{children}</Context.Provider>;
}
export const usePermissions = () => useContext(Context);
