import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useResource } from '../../hooks/useResource';
import { SafeContent } from '../../components/common/SafeContent';
import { Button } from '../../components/common/Button';
import { ErrorNotice } from '../../components/admin/AdminUI';
export type PublishedContent = {
    slug: string;
    kind: string;
    content: {
        title: string;
        body: string;
        configuration?: {
            productIds?: number[];
            petIds?: number[];
        };
    };
};
export const policyNames: Record<string, string> = { terms: 'Điều khoản sử dụng', privacy: 'Chính sách bảo mật', 'shipping-policy': 'Chính sách giao hàng', 'return-policy': 'Chính sách trả hàng', 'cancellation-policy': 'Chính sách hủy lịch', contact: 'Liên hệ' };
export function ContentPage({ page }: {
    page?: string;
}) {
    const { slug } = useParams();
    const code = page || slug || '';
    const r = useResource<PublishedContent>('/content/' + encodeURIComponent(code));
    const title = r.data?.content.title || policyNames[code] || 'Bài viết';
    useEffect(() => { const before = document.title; document.title = title + ' · PetCare Hub'; return () => { document.title = before; }; }, [title]);
    return <article className="mx-auto max-w-4xl px-4 py-10"><h1 className="text-3xl font-bold mb-5">{title}</h1>{r.data ? <SafeContent text={r.data.content.body}/> : r.error ? r.status === 404 ? <p>{policyNames[code] ? 'Nội dung đang được cửa hàng cập nhật. Vui lòng liên hệ trước khi đặt hàng để được giải đáp.' : 'Bài viết không tồn tại hoặc chưa được xuất bản.'}</p> : <ErrorNotice message={r.error} retry={r.retry}/> : <p role="status">Đang tải nội dung…</p>}</article>;
}
export function ContentList({ kind = 'ARTICLE' }: {
    kind?: 'ARTICLE' | 'FAQ';
}) {
    const [page, setPage] = useState(0);
    const r = useResource<{
        content: {
            id: number;
            slug: string;
            snapshot: string;
        }[];
        totalElements: number;
    }>('/content?kind=' + kind + '&size=12&page=' + page);
    return <section className="max-w-5xl mx-auto px-4 py-10"><h1 className="text-3xl font-bold mb-6">{kind === 'FAQ' ? 'Câu hỏi thường gặp' : 'Bài viết chăm sóc thú cưng'}</h1>{r.error ? <ErrorNotice message={r.error} retry={r.retry}/> : !r.data ? <p role="status">Đang tải…</p> : <><div className="space-y-5">{!r.data.content.length && <p>Chưa có nội dung được xuất bản.</p>}{r.data.content.map(c => { const snapshot = JSON.parse(c.snapshot) as {
        title: string;
        body: string;
    }; return kind === 'FAQ' ? <details key={c.id} className="rounded-xl border p-4"><summary className="font-bold cursor-pointer">{snapshot.title}</summary><SafeContent text={snapshot.body}/></details> : <article className="border rounded-xl p-5" key={c.id}><Link className="text-xl underline font-bold" to={'/articles/' + c.slug}>{snapshot.title}</Link><p className="mt-2 break-words">{snapshot.body.slice(0, 180)}…</p></article>; })}</div><div className="flex gap-3 mt-5"><Button disabled={!page} onClick={() => setPage(page - 1)}>Trước</Button><Button disabled={(page + 1) * 12 >= r.data.totalElements} onClick={() => setPage(page + 1)}>Sau</Button></div></>}</section>;
}
