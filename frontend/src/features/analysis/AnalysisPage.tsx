import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { documentsApi, researchApi } from "../../api/judgmentApi";
import { ErrorState, Loading } from "../../components/QueryState";
export function AnalysisPage({
  type,
}: {
  type: "summary" | "arguments" | "compare";
}) {
  const docs = useQuery({
    queryKey: ["documents"],
    queryFn: documentsApi.list,
  });
  const [selected, setSelected] = useState<string[]>([]);
  const [issue, setIssue] = useState("");
  const [result, setResult] = useState<unknown>();
  const [error, setError] = useState("");
  if (docs.isLoading) return <Loading />;
  if (docs.error) return <ErrorState error={docs.error} />;
  const available = docs.data ?? [];
  const title = {
    summary: "Summary",
    arguments: "Arguments",
    compare: "Compare judgments",
  }[type];
  const run = async () => {
    setError("");
    try {
      setResult((await researchApi.analyse(type, selected, issue)).result);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Analysis failed.");
    }
  };
  return (
    <section className="max-w-4xl">
      <h1 className="text-2xl font-bold">{title}</h1>
      <fieldset className="mt-4 rounded bg-white p-4 shadow">
        <legend className="font-semibold">Choose READY judgments</legend>
        {available
          .filter((d) => d.status === "READY")
          .map((doc) => (
            <label key={doc.id} className="mr-4 inline-flex gap-2 p-2">
              <input
                type="checkbox"
                checked={selected.includes(doc.id)}
                onChange={() =>
                  setSelected((s) =>
                    s.includes(doc.id)
                      ? s.filter((id) => id !== doc.id)
                      : [...s, doc.id],
                  )
                }
              />
              {doc.title}
            </label>
          ))}
      </fieldset>
      {type !== "summary" && (
        <label className="mt-4 block">
          Issue to focus on
          <textarea
            value={issue}
            onChange={(e) => setIssue(e.target.value)}
            placeholder="For example: limitation or admissibility"
          />
        </label>
      )}
      <button
        className="mt-4 bg-blue-700 text-white"
        disabled={
          !selected.length || (type === "compare" && selected.length < 2)
        }
        onClick={run}
      >
        Generate {title}
      </button>
      {error && (
        <p role="alert" className="mt-3 text-red-700">
          {error}
        </p>
      )}
      {result !== undefined ? (
        <pre className="mt-5 overflow-auto rounded bg-slate-900 p-4 text-sm text-slate-100">
          {String(JSON.stringify(result, null, 2))}
        </pre>
      ) : null}
    </section>
  );
}
