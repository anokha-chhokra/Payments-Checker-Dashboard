# Order / Payment Reconciler

A small full-stack app that ingests an orders export and a payments export,
reconciles them deterministically, and shows the result on a dashboard with
an LLM-powered "explain this" button.

Stack: **Spring Boot 2.7** (Java), **JSP** views (server-rendered, no
frontend framework), **Spring Security** (form login), **H2** (file-based
SQL database), **OpenAI** for explanations.

## Running it locally

Requirements: JDK 17+ and Maven (or use the wrapper if you add one).

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080` and redirects to `/login`. Sign
up for an account, then log in.

By default it uses a local H2 database file at `./data/reconciler.mv.db` -
nothing to install. To point it at Postgres/MySQL instead, set
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD` and `SPRING_DATASOURCE_DRIVER` (see
`.env.example`); Spring Boot picks these up automatically.

To get LLM explanations working, set `OPENAI_API_KEY`. Without it, the
"Explain" button on the discrepancies page still works end-to-end but
returns a clear error message instead of an explanation.

Two sample CSVs (`orders.csv`, `payments.csv`) are included at the repo
root for testing the upload flow.

## Architecture

```
JSP views  <-->  Spring MVC controllers  <-->  Services  <-->  Spring Data JPA  <-->  H2/Postgres
                                                   |
                                          OpenAI (server-side only)
```

- **Auth**: `AppUser` (email + BCrypt password hash), Spring Security form
  login. Every other table has an `ownerId` column, and every query is
  scoped to the logged-in user's id - there is no way to see another
  user's orders, payments or discrepancies.
- **Ingestion**: `CsvImportService` reads the two CSVs with a small
  hand-rolled parser (the files have no quoted/embedded commas, so a
  `split(",")` is enough) and stores rows as `OrderRecord` /
  `PaymentRecord`.
- **Reconciliation**: `ReconciliationService` - pure Java, no LLM, fully
  deterministic. See below.
- **Dashboard**: `DashboardController` aggregates headline numbers and a
  discrepancy-type breakdown for a bar chart (Chart.js).
- **Drill-down**: `DiscrepancyController` serves a filterable/searchable
  table and a same-origin JSON endpoint that asks the LLM to explain one
  discrepancy.

## Reconciliation logic

Matching key: `order_id` / `order_reference`, **trimmed and upper-cased**.
The payments export contains case and whitespace variants of the same
reference (` ord-1801 `, `ord-1802`), so normalising is required before
anything else works.

Amounts are compared with a **1-cent tolerance** to absorb export rounding,
not real discrepancies. Orders are matched against payments on
`net_amount` (the amount the store believes it's owed after its own
discount) vs. the payment's `amount` (what the processor actually
charged) - not `net_settled`, which already has the processor's fee
subtracted and isn't the number the store recognises as revenue.

Discrepancy types produced:

| Type | Meaning |
|---|---|
| `MISSING_PAYMENT` | Order is `completed` but no payment references it at all. |
| `MISSING_ORDER` | A payment references an order id that doesn't exist in the orders export. |
| `AMOUNT_MISMATCH` | Matched, but the charged amount differs from the order's net amount by more than 1 cent. |
| `CURRENCY_MISMATCH` | Matched, but the order and payment currencies differ. |
| `DUPLICATE_PAYMENT` | More than one settled charge exists for the same order - likely an overcharge. |
| `DUPLICATE_ORDER` | The order itself appears more than once in orders.csv. |
| `STATUS_MISMATCH` | The order and payment statuses disagree in a way that implies risk, e.g. a completed order whose payment is `failed`/`pending`, or a `cancelled`/`refunded` order that still has a settled charge with no refund on file. |

Orders that are `cancelled` or `refunded` with no payment at all are
**not** flagged - that's the expected, healthy case. A refund payment with
no matching order, or a `failed`-status orphan payment, is also not
flagged as risk since no money actually moved.

`amountAtRisk` is set per discrepancy (e.g. the shortfall for an amount
mismatch, the extra charge for a duplicate payment) and summed for the
"money at risk" headline figure. "Value reconciled" / "value in dispute"
split the total order value by whether that order has any discrepancy
attached at all.

## What's actually wrong with this data

Running the engine against the provided CSVs surfaces, among other
things:

- **Reference formatting drift**: some payment rows use lower-case or
  padded order references (`ord-1802`, ` ord-1801 `). Harmless once
  normalised, but it means a naive exact-string join between the two
  systems would silently miss real matches.
- **A currency mix-up**: `ORD-1601` was placed in USD but its payment was
  processed in EUR, and `ORD-1602` shows the opposite (placed in EUR,
  charged in USD) - a genuine FX/booking error, not a rounding issue.
- **Duplicate charges**: at least two orders (`ORD-1501`, `ORD-1502`) were
  charged twice, i.e. customers were billed twice for the same order.
- **A duplicate order row**: `ORD-1004` appears twice in orders.csv with
  identical data - an export/dedup bug on the store's side, not a payment
  problem.
- **Orphan payments**: a handful of transactions reference order ids that
  don't exist in the orders export at all.
- **Orders with no payment**: several `completed` orders have no
  corresponding payment record, meaning revenue was recognised that was
  never actually collected.
- **Missing/blank fields**: a blank `customer_email`, a blank
  `processed_at` timestamp, and a blank `discount` all appear at least
  once - handled by treating blanks as "unknown" rather than crashing the
  import.

For the business, the duplicate charges and the currency mix-up are the
most concrete money-at-risk items (real cash movement that doesn't match
intent); the missing-payment and orphan-payment cases are where revenue
reporting and actual cash collected disagree and need a human to chase.

## LLM approach

- Called from `LlmExplanationService` on the **backend only** - the API
  key never reaches the browser and is read from an environment variable.
- Model: `gpt-4o-mini` (configurable via `OPENAI_MODEL`).
- **Temperature 0.2**: this is a summarisation/explanation task over facts
  that are already fixed by the deterministic engine, not a creative task.
  Low temperature keeps explanations consistent and reduces the chance of
  the model inventing a cause that isn't supported by the data.
- Structured output via OpenAI's `response_format: json_object`, requesting
  exactly `explanation` and `recommended_action`. The response is parsed
  with Jackson; if the JSON is missing either key, or the HTTP call fails
  for any reason, the endpoint returns a typed error (`ExplanationResult`)
  instead of throwing, and the JSP shows that error message in place of
  the explanation rather than an unstyled 500 page.
- The model is explicitly told (system prompt) that matching has already
  happened and it is only explaining/summarising - it never decides
  whether records match.

## What I'd improve with more time

- Pagination on the discrepancies table (fine at ~200 rows, wouldn't
  scale to a real dataset).
- Batch "explain all" instead of one LLM call per row.
- A proper CSV parser (e.g. Apache Commons CSV) instead of `split(",")`,
  in case future exports contain quoted/comma-embedded fields.
- Re-running reconciliation as a background job instead of inline on the
  upload request, for larger files.

## AI tool use

Built with Claude Code
