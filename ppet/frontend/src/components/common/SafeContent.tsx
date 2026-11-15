/** Markdown-lite: headings, bullet lines and paragraphs. React escapes every text node. */
export function SafeContent({ text }: {
    text: string;
}) {
    return <div className="space-y-3 break-words [overflow-wrap:anywhere]">{text.split('\n').map((line, i) => line.startsWith('## ') ? <h3 className="text-xl font-bold" key={i}>{line.slice(3)}</h3> : line.startsWith('# ') ? <h2 className="text-2xl font-bold" key={i}>{line.slice(2)}</h2> : line.startsWith('- ') ? <p key={i}>• {line.slice(2)}</p> : <p className="whitespace-pre-wrap" key={i}>{line || '\u00a0'}</p>)}</div>;
}
