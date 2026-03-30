# Contributing to ZPL Label Viewer

Thank you for your interest in contributing!

## Reporting Issues

- Use the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md) for rendering issues.
- Include the ZPL that fails to render and a reference image (e.g., from labelary.com).

## Development Setup

```bash
git clone https://github.com/Milimarty/zpl-label-viewer.git
cd zpl-label-viewer
./gradlew buildPlugin -x buildSearchableOptions
```

## Running in a Sandbox IDE

```bash
./gradlew runIde
```

This launches a fresh IDE instance with the plugin installed.

## Pull Request Guidelines

1. Fork and create a branch from `main`.
2. Make focused changes with clear commit messages.
3. Open a PR describing what changed and why.
4. For rendering improvements, consider contributing to [zpl-renderer](https://github.com/Milimarty/zpl-renderer) directly.

## Code of Conduct

Be respectful and constructive. See the [Contributor Covenant](https://www.contributor-covenant.org).
