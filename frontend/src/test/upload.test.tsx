import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DocumentLibrary } from "../features/documents/DocumentLibrary";
import { documentsApi } from "../api/judgmentApi";
vi.mock("../api/judgmentApi", () => ({
  documentsApi: { list: vi.fn(), upload: vi.fn(), remove: vi.fn() },
}));
describe("upload library", () => {
  it("shows drag and drop target and document statuses", async () => {
    vi.mocked(documentsApi.list).mockResolvedValue([
      {
        id: "d",
        title: "Sample",
        filename: "sample.pdf",
        pageCount: 2,
        status: "READY",
        createdAt: "",
      },
    ]);
    render(
      <QueryClientProvider client={new QueryClient()}>
        <DocumentLibrary />
      </QueryClientProvider>,
    );
    expect(
      await screen.findByText("Drag a PDF here, or select a file"),
    ).toBeInTheDocument();
    expect(screen.getByText("READY")).toBeInTheDocument();
  });
});
