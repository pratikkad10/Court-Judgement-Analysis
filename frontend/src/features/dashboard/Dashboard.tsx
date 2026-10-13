import { useQuery } from "@tanstack/react-query";
import { authApi, researchApi } from "../../api/judgmentApi";
import { ErrorState, Loading } from "../../components/QueryState";
export function Dashboard() {
  const query = useQuery({
    queryKey: ["dashboard"],
    queryFn: researchApi.dashboard,
  });
  if (query.isLoading) return <Loading />;
  if (query.error) return <ErrorState error={query.error} />;
  const values = query.data ?? { sessions: 0, messages: 0, analyses: 0 };
  return (
    <section>
      <h1 className="text-2xl font-bold">Your dashboard</h1>
      <div className="mt-5 grid gap-4 sm:grid-cols-3">
        {Object.entries(values).map(([label, value]) => (
          <div className="rounded bg-white p-5 shadow" key={label}>
            <p className="capitalize text-slate-500">{label}</p>
            <strong className="text-3xl">{value}</strong>
          </div>
        ))}
      </div>
    </section>
  );
}
export function AdminDashboard() {
  const stats = useQuery({
    queryKey: ["admin-dashboard"],
    queryFn: researchApi.adminDashboard,
  });
  const users = useQuery({ queryKey: ["users"], queryFn: authApi.users });
  const audit = useQuery({ queryKey: ["audit"], queryFn: researchApi.audit });
  if (stats.isLoading || users.isLoading || audit.isLoading) return <Loading />;
  if (stats.error || users.error || audit.error)
    return <ErrorState error={stats.error ?? users.error ?? audit.error} />;
  const statValues = stats.data ?? {};
  const userRows = users.data ?? [];
  const auditRows = audit.data ?? [];
  return (
    <section>
      <h1 className="text-2xl font-bold">Admin dashboard</h1>
      <div className="mt-4 grid gap-3 sm:grid-cols-4">
        {Object.entries(statValues).map(([label, value]) => (
          <div key={label} className="rounded bg-white p-4 shadow">
            <p className="text-slate-500">{label}</p>
            <strong className="text-2xl">{value}</strong>
          </div>
        ))}
      </div>
      <h2 className="mt-8 text-xl font-semibold">Users</h2>
      <table className="mt-3 w-full bg-white text-left shadow">
        <thead>
          <tr>
            <th className="p-3">Email</th>
            <th>Role</th>
            <th>Enabled</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {userRows.map((user) => (
            <tr className="border-t" key={user.id}>
              <td className="p-3">{user.email}</td>
              <td>{user.role}</td>
              <td>{String(user.enabled)}</td>
              <td>
                <button
                  className="border"
                  onClick={() =>
                    authApi
                      .setEnabled(user.id, !user.enabled)
                      .then(() => users.refetch())
                  }
                >
                  {user.enabled ? "Disable" : "Enable"}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <h2 className="mt-8 text-xl font-semibold">Audit log</h2>
      <table className="mt-3 w-full bg-white text-left shadow">
        <thead>
          <tr>
            <th className="p-3">Action</th>
            <th>Owner</th>
            <th>Time</th>
            <th>Correlation ID</th>
          </tr>
        </thead>
        <tbody>
          {auditRows.map((log) => (
            <tr className="border-t" key={log.id}>
              <td className="p-3">{log.action}</td>
              <td>{log.ownerId ?? "—"}</td>
              <td>{new Date(log.createdAt).toLocaleString()}</td>
              <td className="text-xs">{log.correlationId ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
