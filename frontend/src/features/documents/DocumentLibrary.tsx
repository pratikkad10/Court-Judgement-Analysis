import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useDropzone } from "react-dropzone";
import { documentsApi } from "../../api/judgmentApi";
import { ErrorState, Loading } from "../../components/QueryState";
const colors = {
  UPLOADED: "bg-slate-100",
  PROCESSING: "bg-amber-100",
  READY: "bg-emerald-100",
  FAILED: "bg-red-100",
};
export function DocumentLibrary() {
  const client = useQueryClient();
  const query = useQuery({
    queryKey: ["documents"],
    queryFn: documentsApi.list,
    refetchInterval: (data) =>
      data.state.data?.some(
        (d) => d.status === "UPLOADED" || d.status === "PROCESSING",
      )
        ? 3000
        : false,
  });
  const drop = useDropzone({
    accept: { "application/pdf": [".pdf"] },
    multiple: false,
    onDropAccepted: async ([file]) => {
      await documentsApi.upload(file);
      client.invalidateQueries({ queryKey: ["documents"] });
    },
  });
  if (query.isLoading) return <Loading />;
  if (query.error) return <ErrorState error={query.error} />;
  return (
    <section>
      <h1 className="text-2xl font-bold">Document library</h1>
      <p className="mt-1 text-slate-600">
        Upload existing court judgment PDFs for private research.
      </p>
      <div
        {...drop.getRootProps()}
        className="mt-5 cursor-pointer rounded-lg border-2 border-dashed border-blue-300 bg-blue-50 p-8 text-center"
      >
        <input {...drop.getInputProps()} />
        <p>Drag a PDF here, or select a file</p>
        <p className="text-sm text-slate-500">
          PDF only; duplicate uploads are rejected.
        </p>
      </div>
      <div className="mt-6 overflow-x-auto rounded bg-white shadow">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b">
              <th className="p-3">Judgment</th>
              <th>Pages</th>
              <th>Status</th>
              <th>Metadata</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {query.data?.map((doc) => (
              <tr key={doc.id} className="border-b">
                <td className="p-3">
                  <strong>{doc.title}</strong>
                  <br />
                  <span className="text-xs text-slate-500">{doc.filename}</span>
                </td>
                <td>{doc.pageCount}</td>
                <td>
                  <span
                    className={`rounded px-2 py-1 text-xs ${colors[doc.status]}`}
                  >
                    {doc.status}
                  </span>
                </td>
                <td className="text-sm">
                  {doc.court ?? "—"}
                  <br />
                  {doc.caseNumber ?? ""}
                </td>
                <td>
                  <button
                    aria-label={`Delete ${doc.title}`}
                    className="text-red-700"
                    onClick={async () => {
                      await documentsApi.remove(doc.id);
                      client.invalidateQueries({ queryKey: ["documents"] });
                    }}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!query.data?.length && (
          <p className="p-4">No judgments uploaded yet.</p>
        )}
      </div>
    </section>
  );
}
