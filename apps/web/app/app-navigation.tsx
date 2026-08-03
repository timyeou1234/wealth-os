import Link from "next/link";

export function AppNavigation({ current }: { current: "dashboard" | "input" }) {
  return (
    <nav className="app-navigation" aria-label="Primary">
      <Link href="/" aria-current={current === "dashboard" ? "page" : undefined}>Dashboard</Link>
      <Link href="/entry" aria-current={current === "input" ? "page" : undefined}>Input</Link>
    </nav>
  );
}
