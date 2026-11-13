import { useEffect, useState } from 'react';
import api from '../api/client';
import { getErrorMessage } from '../utils/getErrorMessage';

export function useAdminResource<T>(url: string) {
  const [data, setData] = useState<T>();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError(''); setData(undefined);
    api.get<T>(url, { signal: controller.signal }).then(({ data }) => { if (!controller.signal.aborted) setData(data); })
      .catch(error => { if (!controller.signal.aborted) setError(getErrorMessage(error)); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [url, version]);
  return { data, loading, error, reload: () => setVersion(value => value + 1) };
}
