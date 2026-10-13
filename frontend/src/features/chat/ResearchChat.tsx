import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { documentsApi, researchApi } from "../../api/judgmentApi";
import type { ChatMessage, Citation } from "../../api/types";
import { Loading, ErrorState } from "../../components/QueryState";
import { PdfViewer } from "./PdfViewer";
export function ChatAnswer({
  message,
  onCitation,
}: {
  message: ChatMessage;
  onCitation: (citation: Citation) => void;
}) {
  return (
    <article
      className={`max-w-3xl rounded p-4 ${message.role === "USER" ? "ml-auto bg-blue-100" : "bg-white shadow"}`}
    >
      <p className="whitespace-pre-wrap">{message.content}</p>
      {message.citations?.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-2">
          {message.citations.map((c) => (
            <button
              key={c.source_id}
              className="border border-blue-400 bg-blue-50 text-sm text-blue-800"
              onClick={() => onCitation(c)}
            >
              {c.document_title}, p. {c.page_start} · {c.section}
            </button>
          ))}
        </div>
      )}
    </article>
  );
}
export function ResearchChat() {
  const qc = useQueryClient();
  const docs = useQuery({
    queryKey: ["documents"],
    queryFn: documentsApi.list,
  });
  const [selected, setSelected] = useState<string[]>([]);
  const [session, setSession] = useState<string>();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [question, setQuestion] = useState("");
  const [citation, setCitation] = useState<Citation>();
  if (docs.isLoading) return <Loading />;
  if (docs.error) return <ErrorState error={docs.error} />;
  const ready = (docs.data ?? []).filter((d) => d.status === "READY");
  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question || !selected.length) return;
    let id = session;
    if (!id) {
      const created = await researchApi.createSession(question.slice(0, 80));
      id = created.id;
      setSession(id);
    }
    const user: ChatMessage = {
      id: crypto.randomUUID(),
      role: "USER",
      content: question,
      citations: [],
      createdAt: new Date().toISOString(),
    };
    setMessages((current) => [...current, user]);
    setQuestion("");
    try {
      const reply = await researchApi.ask(id, selected, user.content);
      setMessages((current) => [...current, reply]);
      qc.invalidateQueries({ queryKey: ["sessions"] });
    } catch (error) {
      setMessages((current) => [
        ...current,
        {
          id: crypto.randomUUID(),
          role: "ASSISTANT",
          content:
            error instanceof Error ? error.message : "Unable to research now.",
          citations: [],
          createdAt: new Date().toISOString(),
        },
      ]);
    }
  };
  return (
    <section>
      <h1 className="text-2xl font-bold">Research chat</h1>
      <div className="mt-4 grid gap-5 lg:grid-cols-[1fr_410px]">
        <div>
          <fieldset className="rounded bg-white p-4 shadow">
            <legend className="font-semibold">Selected ready judgments</legend>
            {ready.map((doc) => (
              <label key={doc.id} className="mr-4 inline-flex gap-2 p-2">
                <input
                  type="checkbox"
                  checked={selected.includes(doc.id)}
                  onChange={() =>
                    setSelected((current) =>
                      current.includes(doc.id)
                        ? current.filter((id) => id !== doc.id)
                        : [...current, doc.id],
                    )
                  }
                />
                {doc.title}
              </label>
            ))}
            {!ready.length && (
              <p>Upload and wait for a judgment to be READY.</p>
            )}
          </fieldset>
          <div className="mt-4 space-y-3">
            {messages.map((message) => (
              <ChatAnswer
                key={message.id}
                message={message}
                onCitation={setCitation}
              />
            ))}
          </div>
          <form className="mt-4" onSubmit={submit}>
            <label className="sr-only" htmlFor="question">
              Question
            </label>
            <textarea
              id="question"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="Ask about the selected judgments"
            />
            <button
              className="mt-2 bg-blue-700 text-white"
              disabled={!selected.length}
            >
              Ask with citations
            </button>
          </form>
        </div>
        <PdfViewer
          documentId={citation?.document_id}
          page={citation?.page_start}
        />
      </div>
    </section>
  );
}
