import { DEFAULT_DARK_THEME_ID, DEFAULT_LIGHT_THEME_ID, useThemeStore } from "@dbforge/ui";
import { beforeEach, describe, expect, it } from "vitest";

describe("useThemeStore", () => {
  beforeEach(() => {
    useThemeStore.setState({ themeId: DEFAULT_DARK_THEME_ID });
  });

  it("defaults to the dark-plus theme", () => {
    expect(useThemeStore.getState().themeId).toBe(DEFAULT_DARK_THEME_ID);
  });

  it("switches to a valid theme id", () => {
    useThemeStore.getState().setThemeId("dracula");
    expect(useThemeStore.getState().themeId).toBe("dracula");
  });

  it("switches between the two quick-toggle defaults", () => {
    useThemeStore.getState().setThemeId(DEFAULT_LIGHT_THEME_ID);
    expect(useThemeStore.getState().themeId).toBe(DEFAULT_LIGHT_THEME_ID);
  });

  it("ignores an unknown theme id and keeps the current selection", () => {
    useThemeStore.getState().setThemeId("dracula");
    useThemeStore.getState().setThemeId("not-a-real-theme");
    expect(useThemeStore.getState().themeId).toBe("dracula");
  });
});
