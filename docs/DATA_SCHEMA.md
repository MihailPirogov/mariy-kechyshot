# events.json — формат данных / data format

The real calendar data is **not** included in this repository (rights reserved).
`app/src/main/assets/events.json` contains only 3 clearly fake example entries so the app builds and runs.

A JSON array of objects:

| key | type | meaning |
|---|---|---|
| `id` | string | unique id, e.g. `hol-…` |
| `k` | string | kind: `hol` holiday, `pr` prayer (undated), `pd` dated prayer, `om` omen, `bd` birthday/anniversary, `rt` rite |
| `t` | string | title (Mari) |
| `t2` | string | subtitle / Russian title |
| `rule` | string | human-readable date rule |
| `r` | `MM-DD` | recurring yearly date |
| `d` | `[YYYY-MM-DD]` | exact dates |
| `da` | `[YYYY-MM-DD]` | approximate dates |
| `x`, `x2` | string | description texts |
| `c` | string | category |
| `s` | `[string]` | sources |
| `y` | int | year (birthdays) |
| `q`, `cf` | string | quality / confidence flags |
| `hol` | string | related holiday id (omens, rites) |

Omens (`om`) without `r` are "undated" and are used for the *omen of the day* card.
