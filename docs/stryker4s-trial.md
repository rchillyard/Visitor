# Mutation testing with Stryker4s

A record of the trial that led to `stryker4s.conf`, run on 2026-11-19 against what was then
`main` (1.8.1). The numbers are a snapshot and will drift; the reasoning is the part worth
keeping.

Proposal and discussion: [#38](https://github.com/rchillyard/Visitor/issues/38).

## Why

Statement coverage proves a line executed. It does not prove that any test *asserts* what the
line does. Two cases here made the distinction concrete: a method with coverage but no direct
assertion, and a hand-written graph invariant that ran under the tests while accepting
obviously wrong inputs. Mutation testing is the standard answer, so Stryker4s was trialled here
before being considered for the other Phasmid libraries.

## It works on Scala 3.9

`sbt-stryker4s` 1.1.1 on sbt 1.12.13, with `strykerScalaDialect := scala.meta.dialects.Scala3`.

That setting is **required** — the default dialect is Scala 2.13 and parses none of this
codebase. With it, all 13 files in `src/main/scala` parse cleanly: `given X with`, `using`,
`using val` in a secondary parameter list, `enum`, indentation syntax throughout, and the
F-bounded `trait Appendable[X, A <: Appendable[X, A]]`. No crashes, no unsupported constructs.
`-Werror` with `-Wunused:all` and `-Wnonunit-statement` does not break the instrumented compile.

## Runtime, and why the config is not scoped by file

| Scope | Mutants | Mutant phase | Wall clock |
|---|---|---|---|
| baseline `sbt test` (249 tests) | — | 1.2s test exec | 24.3s |
| `Journal.scala` only | 2 | 0.5s | 34.9s |
| `PrioQueue.scala` only | 34 | 7.1s | 35.6s |
| whole `src/main/scala`, unfiltered | 113 | 7.3s | 50.4s |
| whole tree, shipped config, after `clean` | 90 active | 7.1s | **40.6s** |

Stryker4s compiles every mutant into a single artifact (mutation switching) and flips between
them with an environment variable. The cost is therefore two compiles plus sbt start-up and is
almost independent of the mutant count: two mutants cost 35s, 113 cost 50s. **Scoping `mutate`
to a few files saves nothing** — the usual "start narrow" advice is wrong here. Run the whole
library or do not run it.

`Test / parallelExecution := false` turned out not to matter. Stryker creates three test runners
and parallelises across mutants, not within a suite.

## Why `StringLiteral` is excluded

That mutator rewrites every string literal to `""`. Here that means the text of `tracer.trace`
messages and `require` failure messages, which no test asserts and none should. It accounted for
**11 of the 17 survivors** in the unfiltered run, burying the three that mattered. With it
excluded the run leaves exactly six survivors, every one a `ConditionalExpression`.

## Why `break = 0`

The score never fails the build. A surviving mutant may be a genuine gap or an equivalent mutant
that no test could ever kill, and telling them apart needs a reader rather than a threshold —
two of the six survivors here are provably equivalent. Mutation testing is a diagnostic, not a
gate.

## What the trial found

90 active mutants, 70 detected, 6 survived, 7 uncovered: 84.34% of total, 92.11% of covered code.
scoverage over the same tree reports 77.75% statement and 88.24% branch.

Genuine gaps, each filed as its own issue and deliberately not fixed alongside the tooling:

- [#39](https://github.com/rchillyard/Visitor/issues/39) — the pop-time revisit guard in `dfs`
  and `traverseTree` is never exercised. Children are filtered at *push* time, which absorbs the
  ordinary diamond, so the guard only fires when a node is both a later sibling and a descendant
  of an earlier sibling. `VisitorSpec.TestGraph` is a diamond and still does not reach it.
- [#40](https://github.com/rchillyard/Visitor/issues/40) — nothing asserts that
  `Frontier[Stack]` reverses on `offerAll`, and nothing asserts that `Logging.logIt` stays
  silent when logging is disabled.

Two equivalent mutants, which turned out to be a note about the code rather than the tests:
[#42](https://github.com/rchillyard/Visitor/issues/42), now recorded in comments at the two
`BinaryHeap` guards.

## What it adds over scoverage — honestly

This was the point of the trial, so the answer should be precise rather than flattering.

`PrioQueue.scala` has **100.00% statement coverage (141/141) and still yielded four survivors**.
That is the demonstration that coverage is not assertion.

But all three *behavioural* gaps sit on lines scoverage already reports as `branch="true"
invocation-count="0"`. Branch coverage held the same information. What mutation testing added was
salience and falsifiability: it named the input that breaks the `dfs` revisit guard, and the gap
could be proved real by writing one test and watching the mutant flip from Survived to Killed.
That is a different experience from reading a percentage, but it is not a different fact.

The findings genuinely invisible to statement *and* branch coverage — every statement on the line
covered, both branches taken, mutant still alive — were `Logging.scala:104` and the two
equivalent mutants in `PrioQueue`. One minor test gap and two redundancy findings.

So mutation testing does reach past coverage, and past branch coverage too, but on an
already well-tested codebase its *unique* yield is small and tilts toward code redundancy rather
than missing assertions. It is worth 40 seconds. It is not worth a CI gate, and it is not worth
running on every commit.

## Rough edges

- 6 of 113 mutants fail to compile: the `MethodExpression` mutator rewrites `heap.isEmpty` to
  `heap.nonEmpty` on types that define only `isEmpty`, such as `BinaryHeap`. Stryker detects
  this, drops them and recompiles — one extra compile round, roughly 10s, and the reason the
  unfiltered run cost 50s rather than 35s.
- A run leaves mutation-switched classes in `target/scala-3.9.0/classes`. The next ordinary
  `sbt test` recompiles, so nothing leaks, but `target` should not be trusted straight after a run.
- `CompileError` mutants are excluded from the score denominator while `NoCoverage` ones are not,
  which is why the console prints two percentages.
- Stryker4s offers `@SuppressWarnings(Array("stryker4s.mutation.ConditionalExpression"))` to
  silence a known-equivalent survivor. It applies to a whole definition rather than a line, so it
  is usually too blunt: on `BinaryHeap.siftUp` it would also drop the two mutants on the
  `ord.lteq` comparison, both of which the suite kills.

## Applying it to the other libraries

The cost model is two compiles plus start-up, so this transfers to any project whose suite runs
in seconds. Where a suite takes minutes the mutant phase stops being free; the figure to check
first is `Mutation run finished! Took ...` against the baseline suite time, not the mutant count.
A parallel trial on Gryphon is recorded in
[rchillyard/Gryphon#91](https://github.com/rchillyard/Gryphon/pull/91).
