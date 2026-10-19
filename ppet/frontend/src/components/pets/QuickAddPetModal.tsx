import { FormEvent, useEffect, useId, useRef, useState } from 'react';
import { isAxiosError } from 'axios';
import toast from 'react-hot-toast';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { PetDTO, QuickPetSpecies, petManagementService } from '../../services/petManagementService';

export const QuickAddPetModal = ({ onClose, onCreated }: { onClose: () => void; onCreated: (pet: PetDTO) => void }) => {
  const [name, setName] = useState(''); const [speciesId, setSpeciesId] = useState(''); const [age, setAge] = useState('');
  const [species, setSpecies] = useState<QuickPetSpecies[]>([]); const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(''); const [retry, setRetry] = useState(0);
  const [errors, setErrors] = useState<Record<string, string>>({}); const [error, setError] = useState('');
  const [busy, setBusy] = useState(false); const submitting = useRef(false); const selectId = useId();
  useEffect(() => {
    const controller = new AbortController(); setLoading(true); setLoadError('');
    petManagementService.getQuickPetSpecies(controller.signal).then(data => {
      if (controller.signal.aborted) return;
      setSpecies(data); if (!data.length) setLoadError('Chưa có loài Chó/Mèo trong danh mục. Vui lòng liên hệ cửa hàng.');
    }).catch(() => { if (!controller.signal.aborted) setLoadError('Không tải được danh mục loài.'); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [retry]);
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault(); if (submitting.current || loading || loadError) return;
    const fields: Record<string, string> = {};
    if (!name.trim() || name.trim().length > 100) fields.name = 'Tên thú cưng phải có từ 1 đến 100 ký tự';
    if (!species.some(item => item.id === Number(speciesId))) fields.speciesId = 'Vui lòng chọn Chó hoặc Mèo';
    if (!age.trim() || !Number.isInteger(Number(age)) || Number(age) < 0 || Number(age) > 30) fields.age = 'Tuổi phải là số nguyên từ 0 đến 30';
    setErrors(fields); setError(''); if (Object.keys(fields).length) return;
    submitting.current = true; setBusy(true);
    try {
      const pet = await petManagementService.createMyPet({ name: name.trim(), speciesId: Number(speciesId), age: Number(age) });
      onCreated(pet); toast.success('Đã thêm thú cưng');
    } catch (failure) {
      const message = isAxiosError(failure) && typeof failure.response?.data?.message === 'string' ? failure.response.data.message : 'Không thêm được thú cưng. Vui lòng thử lại.';
      // Existing ErrorResponse encodes field errors in message as {name=..., age=...}.
      const fieldErrors: Record<string, string> = {};
      for (const field of ['name', 'speciesId', 'age']) {
        const match = message.match(new RegExp(`(?:^|[,{]\\s*)${field}\\s*[:=]\\s*([^,}]+)`));
        if (match) fieldErrors[field] = match[1];
      }
      setErrors(fieldErrors); setError(Object.keys(fieldErrors).length ? 'Vui lòng kiểm tra các trường được đánh dấu.' : message);
    } finally { submitting.current = false; setBusy(false); }
  };
  return <Modal title="Thêm nhanh thú cưng" onClose={onClose} busy={busy}>
    <form noValidate onSubmit={submit} className="space-y-4">
      <Input label="Tên thú cưng" name="name" required maxLength={100} value={name} onChange={event => setName(event.target.value)} error={errors.name} disabled={busy} />
      <div><label htmlFor={selectId} className="block font-medium mb-2">Loài</label>
        {loading ? <p>Đang tải danh mục loài…</p> : loadError ? <p role="alert">{loadError} <Button type="button" size="sm" variant="outline" onClick={() => setRetry(value => value + 1)}>Thử lại</Button></p> :
          <select id={selectId} value={speciesId} onChange={event => setSpeciesId(event.target.value)} required disabled={busy}
            aria-invalid={!!errors.speciesId} aria-describedby={errors.speciesId ? `${selectId}-error` : undefined} className="w-full border rounded-xl p-3">
            <option value="">Chọn loài</option>{species.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>}
        {errors.speciesId && <p id={`${selectId}-error`} className="text-red-600 text-sm">{errors.speciesId}</p>}
      </div>
      <Input label="Tuổi (năm)" name="age" type="number" required min={0} max={30} step={1} value={age} onChange={event => setAge(event.target.value)} error={errors.age} disabled={busy} />
      {error && <p role="alert" className="text-red-600">{error}</p>}
      <Button type="submit" fullWidth isLoading={busy} disabled={loading || !!loadError}>Lưu thú cưng</Button>
    </form>
  </Modal>;
};
