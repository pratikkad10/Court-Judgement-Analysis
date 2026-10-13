import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
const links = [
  ["/", "Library"],
  ["/chat", "Research chat"],
  ["/summary", "Summary"],
  ["/arguments", "Arguments"],
  ["/compare", "Compare"],
  ["/history", "History"],
  ["/dashboard", "Dashboard"],
] as const;
export function Layout() {
  const { user, signOut } = useAuth();
  return (
    <div className="min-h-screen">
      <header className="flex items-center justify-between bg-slate-900 px-4 py-3 text-white">
        <NavLink to="/" className="font-semibold">
          Judgment Analyzer
        </NavLink>
        <div className="flex items-center gap-3 text-sm">
          <span>{user?.email}</span>
          <button className="bg-slate-700" onClick={signOut}>
            Sign out
          </button>
        </div>
      </header>
      <div className="md:flex">
        <nav className="flex gap-1 overflow-auto bg-white p-3 md:min-h-screen md:w-52 md:flex-col">
          {links.map(([to, label]) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `rounded px-3 py-2 ${isActive ? "bg-blue-100 text-blue-800" : "hover:bg-slate-100"}`
              }
            >
              {label}
            </NavLink>
          ))}
          {user?.role === "ADMIN" && (
            <NavLink
              to="/admin"
              className="rounded px-3 py-2 hover:bg-slate-100"
            >
              Admin
            </NavLink>
          )}
        </nav>
        <main className="w-full p-4 md:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
