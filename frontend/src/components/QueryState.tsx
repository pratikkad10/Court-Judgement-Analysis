export function Loading() {
  return (
    <p className="p-4 text-slate-600" role="status">
      Loading…
    </p>
  );
}
export function ErrorState({ error }: { error: unknown }) {
  return (
    <p role="alert" className="rounded bg-red-50 p-3 text-red-700">
      {error instanceof Error
        ? error.message
        : "Something went wrong. Please try again."}
    </p>
  );
}
