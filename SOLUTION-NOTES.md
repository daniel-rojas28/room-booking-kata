# Solution notes

This branch has a reference solution (`src/main`) and the hidden tests (`src/test/.../Hidden*Test.java`). `./mvnw test` runs all of them green.
It's one good answer, not the only one.

## How a reviewer would score an attempt

| area | weight | what "good" looks like |
|---|---|---|
| correctness | 35% | visible + hidden tests; rule order respected; half-open overlap; cancelled bookings ignored |
| Spring usage | 20% | `@Valid` + Bean Validation; one `@RestControllerAdvice` for all errors; `ResponseEntity.created(...)`; constructor injection; the `Clock` used everywhere |
| data access | 15% | overlap check in the database, not `findAll()`; paging in the query; no N+1 |
| code quality | 15% | rules in services, thin controllers; small methods; no duplicated rules; field injection removed |
| process | 15% | ran the tests first; one commit per task; own tests added; trade-offs explained in `NOTES.md` |

## What each task tests

1. **404 bug.** `findById(id).get()` throws `NoSuchElementException`, which becomes a 500. Use `orElseThrow` with your own exception, and map it once in the `@RestControllerAdvice`.
2. **Create.** Bean Validation on the request record (`@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Min`) plus handlers for `MethodArgumentNotValidException` and `HttpMessageNotReadableException` (bad JSON or a bad date). Then the rules in order. Common mistakes: `LocalDateTime.now()` without the clock; `isAfter` where "starting exactly now is fine" needs `!isBefore`; an overlap check that loads every booking; forgetting to lower-case the email; 200 instead of 201 + `Location`.
3. **Cancel.** Load the booking, call `booking.cancel()` inside a `@Transactional` method (dirty checking saves it). "Hasn't started" = `start.isAfter(now)`. A missing `X-User-Email` is a `MissingRequestHeaderException` → 400.
4. **Free rooms.** The reference runs two queries (the ids of rooms with an overlapping confirmed booking, and the active rooms) and filters and sorts in Java, which is fine for a few hundred rooms. With thousands, do it in one JPQL query: `not exists` an overlapping confirmed booking, `order by r.capacity, r.name` (see discussion question 1). Overlap of `[a, b)` and `[c, d)` is `a < d and b > c`.
5. **My bookings.** `Page<Booking> findByOrganizerEmail(String email, Pageable p)` with `PageRequest.of(page, size, Sort.by("start", "id"))`, so the database does `LIMIT/OFFSET` and a `count(*)`. `@EntityGraph(attributePaths = "room")` loads each booking's room in the same query; without it, mapping to `roomName` runs one query per room (N+1). `X-Total-Count` = `page.getTotalElements()`. Validate `page`/`size` yourself; `size=abc` is a `MethodArgumentTypeMismatchException`.
6. **Edit with ETag.** `@Version private long version` on the entity; ETag = `"\"" + version + "\""`. The PATCH body is a record with nullable fields (`null` = not sent); `@NotBlank` would wrongly reject a missing title, so use `@Pattern(".*\\S.*")` + `@Size`. **Trap:** `@Version` only goes up when Hibernate flushes, so after changing the entity call `repository.flush()` (or `saveAndFlush`) before building the response; otherwise the response carries the old ETag. Rule order: 403 before 412, so strangers can't probe versions.
7. **Report.** Load the day's confirmed bookings in one query (`join fetch` the room), clip each to 08:00–18:00 (`max(start, 08:00)` to `min(end, 18:00)`), sum the minutes per room, and include rooms with 0. Round with `BigDecimal.setScale(1, HALF_UP)`, not `Math.round` on a double.
8. **Performance.** The original code calls `findAll()` and filters in Java: millions of rows loaded and mapped, plus a lazy room load per booking. Fix: one query with the filters in `where`, `order by`, and `join fetch b.room`. In production, add an index on `(room_id, start_time)`.
9. **Request id.** A `@Component` that extends `OncePerRequestFilter`, sets the header **before** `chain.doFilter(...)`, and treats `getHeader(...) == null` as missing (calling `.isBlank()` on null throws for every request).
10. **Double bookings.** Check-then-insert is a race: two transactions both see "no overlap" and both insert. The reference locks the room row (`@Lock(PESSIMISTIC_WRITE)` on a `findByIdForUpdate`), so bookings for the same room take turns. See discussion question 4 for the alternatives.

## Discussion questions: model answers

**1. Slow availability search.** Measure first: request timing, SQL logging and the number of queries per request (Hibernate statistics), then `EXPLAIN ANALYZE` on the slow query, with production-sized data. Likely causes: loading rooms or bookings into Java and filtering there, or an overlap query without an index. Fixes: do it all in one query (`not exists` subquery); an index on `bookings(room_id, start_time)` (or `(room_id, status, start_time, end_time)`); an index on `rooms(active, capacity)`. Costs: indexes slow writes a little and take space. A cache only helps if the same searches repeat, and availability changes all the time, so it would need very short TTLs.

**2. Lost updates.** Task 6 uses optimistic concurrency over HTTP: the client sends the version it read (`If-Match`); if someone changed the booking since, the server answers 412 and the client has to re-read. It costs a retry for the loser, but nothing is held open. Alternatives: JPA `@Version` alone (the same idea inside the server; the second commit gets an `OptimisticLockException`, which you map to 409); a pessimistic lock (`SELECT … FOR UPDATE`) during the edit (simple, but holds the row and can block or deadlock under load); last-write-wins (no cost, but changes silently disappear).

**3. Year-long report.** Options: aggregate in SQL (`sum` of clipped minutes `group by room`) instead of loading every booking into Java; precompute one row per room per day (a nightly job or an update on every booking change), so a year is 365 × 500 rows to sum; cache finished reports for past dates, which never change; run large reports asynchronously (return 202 + a link, email it when done). Costs: precomputed tables can drift from the bookings (they need a rebuild or check job); async reports are more complex for the user and the code.

**4. Double-booking fixes.**
- *Lock the room row* (`PESSIMISTIC_WRITE`): simple, correct on any database; bookings for the same room wait for each other (fine unless one room is extremely hot); lock order matters if you ever lock two rooms.
- *Database exclusion constraint* (PostgreSQL `EXCLUDE USING gist (room_id WITH =, tsrange(start, end) WITH &&) WHERE status = 'CONFIRMED'`): the database guarantees it even for other writers; it's PostgreSQL-specific and needs mapping the constraint error to 409.
- *Optimistic locking* (`@Version` on the room, bumped on every booking): no waiting; under contention, losers must retry; every booking writes the room row.
- *Slot table* (one row per room per 15 minutes, unique key on `(room_id, slot_start)`): a plain unique index does the work on any database; many rows per booking (a 4-hour booking = 16 rows) and cancel must delete them.
