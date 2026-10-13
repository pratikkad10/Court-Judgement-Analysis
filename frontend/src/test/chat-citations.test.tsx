import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ChatAnswer } from "../features/chat/ResearchChat";
describe("chat citations", () => {
  it("renders a clickable page citation", async () => {
    const onCitation = vi.fn();
    render(
      <ChatAnswer
        onCitation={onCitation}
        message={{
          id: "m",
          role: "ASSISTANT",
          content: "The appeal was dismissed [S1].",
          createdAt: "",
          citations: [
            {
              source_id: "S1",
              document_id: "d",
              document_title: "Sample judgment",
              page_start: 4,
              page_end: 4,
              section: "Holding",
              snippet: "dismissed",
            },
          ],
        }}
      />,
    );
    const button = screen.getByRole("button", {
      name: /Sample judgment, p. 4/,
    });
    await userEvent.click(button);
    expect(onCitation).toHaveBeenCalledWith(
      expect.objectContaining({ page_start: 4 }),
    );
  });
});
