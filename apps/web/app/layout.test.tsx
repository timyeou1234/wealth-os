import React from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import RootLayout from "./layout";

describe("RootLayout", () => {
  it("wraps application content in the required document tags", () => {
    const markup = renderToStaticMarkup(
      <RootLayout>
        <main>Dashboard</main>
      </RootLayout>,
    );

    expect(markup).toContain("<html");
    expect(markup).toContain("<body>");
    expect(markup).toContain("<main>Dashboard</main>");
  });
});
