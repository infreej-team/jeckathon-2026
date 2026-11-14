import { formatChange } from "../_lib/format";

export function ChangeText({ price }: { price: number }) {
  const { arrow, text } = formatChange(price);
  return (
    <>
      <span className="font-sans text-[0.8em]">{arrow}</span> {text}
    </>
  );
}

export function Arrow({ up }: { up: boolean }) {
  return <span className="font-sans text-[0.8em]">{up ? "▲" : "▼"}</span>;
}
