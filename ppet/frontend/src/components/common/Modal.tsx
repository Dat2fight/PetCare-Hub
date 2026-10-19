import { ReactNode, useEffect, useId, useRef } from 'react';
import { createPortal } from 'react-dom';
import { Button } from './Button';

interface ModalProps { title: string; children: ReactNode; onClose: () => void; busy?: boolean }

export const Modal = ({ title, children, onClose, busy = false }: ModalProps) => {
  const dialog = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  useEffect(() => {
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const element = dialog.current;
    element?.showModal(); // Native modal traps focus and makes the rest of the page inert.
    return () => { element?.close(); previous?.focus(); };
  }, []);
  return createPortal(<dialog ref={dialog} role="dialog" aria-modal="true" aria-labelledby={titleId}
    onCancel={event => { event.preventDefault(); if (!busy) onClose(); }}
    className="m-auto w-[calc(100%-2rem)] max-w-lg max-h-[90vh] overflow-y-auto rounded-2xl p-6 bg-white backdrop:bg-black/50">
    <div className="flex items-center justify-between gap-4 mb-5">
      <h2 id={titleId} className="text-xl font-bold">{title}</h2>
      <Button type="button" size="sm" variant="outline" onClick={onClose} disabled={busy} aria-label="Đóng hộp thoại">Đóng</Button>
    </div>
    {children}
  </dialog>, document.body);
};
