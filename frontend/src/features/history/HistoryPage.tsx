import { useQuery } from "@tanstack/react-query";
import { researchApi } from "../../api/judgmentApi";
import { ErrorState, Loading } from "../../components/QueryState";
export function HistoryPage() {
  const query = useQuery({
    queryKey: ["history"],
    queryFn: researchApi.history,
  });
  if (query.isLoading) return <Loading />;
  if (query.error) return <ErrorState error={query.error} />;
  const entries = query.data ?? [];
  return (
    <section>
      <h1 className="text-2xl font-bold">Saved analyses</h1>
      <div className="mt-5 space-y-3">
        {entries.map((item) => (
          <article className="rounded bg-white p-4 shadow" key={item.id}>
            <strong className="capitalize">{item.type}</strong>
            <time className="ml-3 text-sm text-slate-500">
              {new Date(item.createdAt).toLocaleString()}
            </time>
            <pre className="mt-2 overflow-auto text-sm">
              {JSON.stringify(item.result, null, 2)}
            </pre>
          </article>
        ))}
        {!entries.length && <p>No saved analyses yet.</p>}
      </div>
    </section>
  );
}
