import { useEffect, useState } from "react";
import { Document, Page, pdfjs } from "react-pdf";
import { documentsApi } from "../../api/judgmentApi";
pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdfjs-dist/build/pdf.worker.min.mjs",
  import.meta.url,
).toString();
export function PdfViewer({
  documentId,
  page,
}: {
  documentId?: string;
  page?: number;
}) {
  const [file, setFile] = useState<Blob>();
  const [pages, setPages] = useState(0);
  useEffect(() => {
    if (!documentId) return;
    documentsApi
      .pdf(documentId)
      .then(setFile)
      .catch(() => setFile(undefined));
  }, [documentId]);
  if (!documentId)
    return (
      <aside className="rounded bg-slate-100 p-4">
        Select a citation to view its PDF page.
      </aside>
    );
  return (
    <aside className="max-h-[75vh] overflow-auto rounded bg-slate-100 p-3">
      <Document
        file={file}
        loading="Loading PDF…"
        onLoadSuccess={({ numPages }) => setPages(numPages)}
      >
        <Page pageNumber={Math.min(page ?? 1, pages || 1)} width={380} />
      </Document>
      <p className="mt-2 text-sm">
        Page {page ?? 1} of {pages || "…"}
      </p>
    </aside>
  );
}
