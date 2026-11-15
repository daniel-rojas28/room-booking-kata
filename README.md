# Room booking kata

A practice exercise for Java 21 + Spring Boot 3: an **existing, half-finished codebase** for booking meeting rooms. Some features are missing, and some of the code that's there has problems. You read it, fix it and extend it, the way you would on a real team.

## Topics

What you'll practice, and where. Use it to pick tasks that cover your weak spots.

| topic | what you practice | tasks |
|---|---|---|
| Reading existing code | finding your way in a codebase you didn't write; spotting bugs and bad patterns (field injection, `Optional.get()`, `findAll()` + filtering in Java) | 1, 8, all |
| REST controllers | `@GetMapping`/`@PostMapping`/`@PatchMapping`, `@PathVariable`, `@RequestParam` (required by default, defaults), `ResponseEntity`, status codes 200/201/304/400/403/404/409/412 | 1–6 |
| Bean Validation | `@Valid`, `@NotNull`/`@NotBlank`/`@Email`/`@Size`/`@Min`, optional fields in a PATCH body, rules that need code (dates, duration, 15-minute boundaries) | 2, 4, 5, 6 |
| Error handling | one `@RestControllerAdvice` with a single error format; mapping framework exceptions (invalid body, bad JSON, missing parameter or header, wrong type) | 1–6 |
| Business rules | checking rules in a fixed order; half-open time intervals; "now" from an injected `Clock`; case-insensitive emails | 2, 3, 4, 6 |
| JPA and JPQL | entities and relationships, derived queries, `@Query`, overlap queries, `join fetch` / `@EntityGraph` | 2, 4, 5, 7, 8 |
| Transactions | `@Transactional`, dirty checking, when `@Version` goes up (flush) | 3, 6 |
| Pagination | `Pageable` / `Page`, sorting, paging in the database instead of in memory | 5 |
| HTTP headers | `Location`, `X-Total-Count`, `ETag` + `If-None-Match` (304), `If-Match` (412), custom request headers | 2, 3, 5, 6, 9 |
| Filters | a `OncePerRequestFilter` that applies to every response, errors included | 9 |
| Performance | N+1 queries, loading only what you return, indexes | 5, 8 |
| Concurrency | race conditions (check-then-insert), pessimistic vs optimistic locking, database constraints | 6, 10 |
| Algorithms in Java | clipping intervals to a window, grouping and summing, sorting by several keys, rounding with `BigDecimal` | 7 |
| Testing | MockMvc tests, a fixed `Clock`, adding your own tests for the cases you find | all |
| Design thinking | measure before fixing; options and their costs (indexes, caching, precomputing, locking) | discussion questions |
| Workflow | running the tests often, one commit per task, writing down trade-offs | all |

## How to use it

```bash
git clone <this repo> && cd room-booking-kata
git switch -c my-attempt
./mvnw test               # many tests fail at first; that's the point
```

1. Pick a time box (below) and start a timer.
2. Do the tasks **in order**. A few tasks done well beat all of them half done.
3. **Commit after each task** with a clear message.
4. When the time is up, write a few lines in `NOTES.md`: what's done, what isn't, and what you'd do with more time.
5. Run the hidden tests: `./grade.sh`.
6. Only then compare with the `solution` branch (`git diff my-attempt solution -- src/main`).

**Don't open the `solution` branch before you finish.** It holds a reference solution, the hidden tests and notes on every task.

| time box | tasks |
|---|---|
| 90 minutes | 1–4 |
| 3 hours | 1–9 |
| bonus | 10 and the discussion questions |

## Rules

- You can change any production code and add classes, methods and dependencies.
- **Don't change the existing tests**, `ApiTestBase` or `FixedClockConfig`. You can (and should) **add** your own tests.
- **Don't change the JSON contracts below**: field names, error codes, status codes and header names.
- The visible tests are not complete. `grade.sh` runs more tests, all based on this README.
- Documentation and search are fine. Try it without an AI assistant; that's where the practice is.

## Getting started

Requirements: Java 21. Maven comes with the wrapper.

```bash
./mvnw test             # run the test suite
./mvnw spring-boot:run  # start on http://localhost:8080 (H2 console at /h2-console)
```

`data.sql` loads a few rooms when the app starts. Tests create their own data.

## Domain

- **Room**: `id`, `name`, `floor`, `capacity`, `active`. Inactive rooms can't be booked and don't appear in search results.
- **Booking**: a room booked by an organizer from `start` to `end`, for a number of `attendees`. Status is `CONFIRMED` or `CANCELLED`. Bookings are never deleted.
- Times are `LocalDateTime` in ISO-8601 (`2026-03-03T10:00:00`), office local time.
- A booking covers the **half-open** interval `[start, end)`: a booking ending at 11:00 doesn't clash with one starting at 11:00.
- Only `CONFIRMED` bookings occupy a room.
- "Now" comes from the injected `java.time.Clock`. Don't call `LocalDateTime.now()` without it: the tests fix the clock at **Monday 2026-03-02 09:00**.
- Every booking has a **version**: it starts at `0` and goes up by 1 on every change (an edit or a cancel).

## Error format

Every error response (4xx) has this body:

```json
{ "error": "BOOKING_CONFLICT", "message": "free text for humans" }
```

| status | `error` | when |
|---|---|---|
| 400 | `VALIDATION_ERROR` | any invalid input: missing or malformed fields, bad JSON, missing or malformed query parameters or headers, wrongly formatted dates, rule violations listed below |
| 400 | `CAPACITY_EXCEEDED` | more attendees than the room fits |
| 403 | `NOT_ORGANIZER` | someone other than the organizer tries to change or cancel a booking |
| 404 | `ROOM_NOT_FOUND` / `BOOKING_NOT_FOUND` | unknown id |
| 409 | `ROOM_INACTIVE` | booking an inactive room |
| 409 | `BOOKING_CONFLICT` | the room is already booked in that interval |
| 409 | `ALREADY_CANCELLED` / `BOOKING_STARTED` | cancel and edit rules below |
| 412 | `PRECONDITION_FAILED` | the `If-Match` header doesn't match the booking's current `ETag` |

---

## Tasks

### 1. Bug: unknown room returns 500

`GET /rooms/{id}` with an id that doesn't exist returns `500 Internal Server Error`. It should return `404` with `ROOM_NOT_FOUND`.

### 2. Create a booking: `POST /bookings`

Request:

```json
{
  "roomId": 2,
  "organizerEmail": "ana@example.com",
  "title": "Sprint planning",
  "start": "2026-03-03T10:00:00",
  "end": "2026-03-03T11:30:00",
  "attendees": 5
}
```

Response: **`201 Created`**, a `Location: /bookings/{id}` header, and the booking as the body (`BookingResponse`, already in the code).

Rules, **checked in this order** (the first failing rule decides the response):

1. **Input** → `400 VALIDATION_ERROR`
   - every field is required; `title` is not blank, at most 100 characters
   - `organizerEmail` is a valid email
   - `attendees` ≥ 1
   - `start` is before `end`
   - `start` is not in the past (starting exactly now is fine)
   - `start` and `end` are on a 15-minute boundary (`:00`, `:15`, `:30`, `:45`, zero seconds)
   - a booking lasts **at most 4 hours**
2. The room exists → `404 ROOM_NOT_FOUND`
3. The room is active → `409 ROOM_INACTIVE`
4. `attendees` ≤ room capacity → `400 CAPACITY_EXCEEDED`
5. No overlap with a confirmed booking of the same room → `409 BOOKING_CONFLICT`

The organizer email is **case-insensitive**: `Ana@Example.com` and `ana@example.com` are the same person.

### 3. Cancel a booking: `POST /bookings/{id}/cancel`

The caller identifies themselves with the header `X-User-Email`. (Real authentication is out of scope.)

Response: `200 OK` with the booking, now `"status": "CANCELLED"`.

Rules, in this order:

1. The booking exists → `404 BOOKING_NOT_FOUND`
2. The caller is the organizer → `403 NOT_ORGANIZER`
3. The booking isn't cancelled already → `409 ALREADY_CANCELLED`
4. The booking hasn't started yet (`start` is after now) → `409 BOOKING_STARTED`

A cancelled slot can be booked again.

### 4. Find a free room: `GET /rooms/available?start=…&end=…&attendees=…`

Returns the **active** rooms that fit `attendees` and have no confirmed booking overlapping `[start, end)`, sorted by **capacity ascending, then name**. If nothing fits, the result is `200` with an empty list.

`start` must be before `end`, and `attendees` ≥ 1; otherwise `400 VALIDATION_ERROR`.

### 5. My bookings: `GET /bookings?organizer=…&page=0&size=20`

Returns one page of an organizer's bookings (confirmed **and** cancelled), sorted by `start`, then `id`.

- `organizer` is required and case-insensitive.
- `page` starts at 0 (default 0). `size` is 1 to 100 (default 20). Anything else → `400 VALIDATION_ERROR`.
- The response body is a plain JSON array of bookings. The **`X-Total-Count`** header holds the total number of the organizer's bookings, across all pages.
- A page after the last one is `200` with an empty array (and the real total).
- An organizer can have thousands of bookings: **load only the page you return**, in a fixed number of queries.

### 6. Edit a booking, safely: `PATCH /bookings/{id}`

First, make `GET /bookings/{id}` return an **`ETag`** header: the booking's version in double quotes, e.g. `"0"`.
If the request sends `If-None-Match` with the current ETag, return **`304 Not Modified`** with the ETag and no body.

Then add the edit. Body: any of these fields; a field that's missing or `null` keeps its current value.

```json
{ "title": "Sprint planning (moved)", "attendees": 6 }
```

Headers: `X-User-Email` (who's asking) and **`If-Match`** (the ETag they last saw). Both are required.

Response: `200 OK`, the updated booking and its **new** `ETag`.

Rules, in this order:

1. **Input** → `400 VALIDATION_ERROR`: a missing header; `title`, if sent, is not blank and at most 100 characters; `attendees`, if sent, ≥ 1
2. The booking exists → `404 BOOKING_NOT_FOUND`
3. The caller is the organizer (case-insensitive) → `403 NOT_ORGANIZER`
4. `If-Match` equals the current ETag → otherwise `412 PRECONDITION_FAILED` (someone changed it since they read it)
5. The booking isn't cancelled → `409 ALREADY_CANCELLED`
6. The booking hasn't started yet → `409 BOOKING_STARTED`
7. `attendees` ≤ room capacity → `400 CAPACITY_EXCEEDED`

### 7. Utilization report: `GET /reports/utilization?date=2026-03-03`

Facilities wants to know how busy each room is. For **every active room**, count how many minutes it's booked (confirmed only) during **business hours, 08:00 to 18:00** on that date. Bookings that start or end outside business hours count only for the part inside them.

```json
[
  { "roomId": 3, "roomName": "Quito", "bookedMinutes": 180, "utilizationPercent": 30.0 },
  { "roomId": 2, "roomName": "Oslo",  "bookedMinutes": 60,  "utilizationPercent": 10.0 },
  { "roomId": 1, "roomName": "Lisbon","bookedMinutes": 0,   "utilizationPercent": 0.0 }
]
```

- `utilizationPercent` = `bookedMinutes` / 600 × 100, **rounded to 1 decimal, half up** (7.45 → 7.5)
- sorted by `utilizationPercent` **descending, then name**
- rooms with no bookings are included, with 0

### 8. Performance: `GET /rooms/{id}/bookings?from=…&to=…`

This endpoint already exists and returns the right results: the confirmed bookings of one room that overlap `[from, to)`, sorted by `start`, or `404 ROOM_NOT_FOUND` for an unknown room.

In production the bookings table has **millions of rows**, and this endpoint has started timing out. Make it scale without changing its responses. Be ready to explain what was wrong.

### 9. Request id on every response

Every response, **including errors**, has an `X-Request-Id` header: the value the caller sent in `X-Request-Id`, or a new UUID if they sent none (or a blank one). Write it once, not in every controller method.

### 10. Bonus: double bookings

Support says that twice last month, **the same room was booked twice for the same time**. Both users got a `201`. It happens when two people book at the same moment. Find out why and fix it. `ConcurrentBookingTest` reproduces it. Explain your fix and its trade-offs.

---

## Discussion questions

No code needed. Write a few sentences each in `NOTES.md`, or talk them through with a friend. For every answer, say **what** you'd do, **why**, and **what it costs**.

1. With 2,000 rooms and 5 million bookings, `GET /rooms/available` (task 4) takes 3 seconds. Before changing any code, how do you find out where the time goes? What would you change?
2. Two people edit the same booking at the same moment, and one of the changes disappears without anyone noticing. How does task 6 prevent that? What other ways are there, and what does each cost?
3. Facilities now wants the utilization report (task 7) for a whole year and 500 rooms, and it times out. What are your options?
4. Task 10 has several possible fixes: locking the room row, a database constraint, optimistic locking, or a table with one row per room and 15-minute slot. Compare them.

## When you're done

```bash
./grade.sh    # your code + the visible tests + the hidden tests; your files aren't changed
```

Then compare with the `solution` branch, and read its `NOTES.md` for what each task was testing.
