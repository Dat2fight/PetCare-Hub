import { useEffect, useState } from 'react';
import apiClient from '../api/client';

export function useResource<T>(url: string | null, revision = 0) {
  const [attempt, setAttempt] = useState(0);
  const key = JSON.stringify([url, revision, attempt]);
  const [result, setResult] = useState<{key: string; data?: T; error?: string; status?: number}>();
  useEffect(() => {
    if (!url) return;
    const control = new AbortController();
    apiClient.get<T>(url, {signal: control.signal}).then(({data}) => {
      if (!control.signal.aborted) setResult({key, data});
    }).catch(err => {
      if (!control.signal.aborted) setResult({key, status: err.response?.status, error: (err.response?.data?.correlationId ? (err.response.data.message || 'Không tải được dữ liệu.') + ' · Mã lỗi: ' + err.response.data.correlationId : err.response?.data?.message) || 'Không tải được dữ liệu.'});
    });
    return () => control.abort();
  }, [url, key]);
  return {...(result?.key === key ? result : {}), retry: () => setAttempt(n=>n+1)};
}
