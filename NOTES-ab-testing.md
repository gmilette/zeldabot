# A/B testing the bot on one room

Notes to self. Everything here is about `room_` mode: start from a saved state for one
room, fight it, record the trial, reset, repeat.

## How to run

Everything runs from the `paths/` directory. **This matters** — `DirectoryConstants`
uses paths relative to the working directory (`../../botoutput/`), so running from
anywhere else writes output to the wrong place or silently finds no save states.

### 1. Start the emulator first

```
open /Users/gregm/dev/zelda/Nintaco.jar     # or double-click it
```
Load `zelda.nes`, then **Tools → Start Program Server → Start Server**, then close that
window. The bot connects to `localhost:9999`. If you see
`Status message: Failed to establish connection` on a loop, the server is not running.

### 2. Run a batch

```
cd /Users/gregm/dev/zelda/zeldabot/paths
./gradlew run --args="room_8_62 dev label=armA trials=30 seed=1000"
```

| argument | meaning |
|---|---|
| `room_<level>_<mapLoc>` | which room. Optional suffixes: sword `_w` white `_m` magic `_d` wooden, then ring `_b` blue `_r` red `_g` none. e.g. `room_8_62_m_b` |
| `dev` | **required for any CSV to be written.** Sets `DirectoryConstants.enableInfo` |
| `label=<arm>` | names the arm. Stamped into every output row and the filenames. Without it the two batches cannot be told apart |
| `trials=<n>` | stop after n trials. Omit and it loops forever |
| `hearts=<n>` | heart containers Link starts each trial with, e.g. `hearts=16`. Omit to use whatever the save state has |
| `shield` | bare flag: give Link the magic shield |
| `seed=<n>` | seeds the bot's RNG per trial (see below) |
| `maxframes=<n>` | frame budget per trial, default 18000 (5 minutes of game time at 60fps). Exceeding it ends the trial as `timeout` |
| `noui` | no Compose window |

The batch stops on its own and prints `=== batch done: N trials ... ===`.

### 3. Edit the code, run the other arm

```
./gradlew run --args="room_8_62 dev label=armB trials=30 seed=1000"
```
Same seed, same room, same trial count. Only the code differs.

### 4. Compare

```
./gradlew analyzeAb -Pargs="armA armB"
./gradlew analyzeAb -Pargs="armA armB csv=/tmp/trials.csv"
```
Prints the stats and writes `botoutput/zexperiment/ab_armA_vs_armB.html`, which it also
opens in the browser. `csv=<path>` additionally exports a flat spreadsheet-openable CSV of
the trials it loaded. A positional third argument points at a different input file.

For ad-hoc questions, `jq` reads the JSONL directly:

```
jq -r 'select(.label=="armA") | .elapsedFrames' ../botoutput/zexperiment/experiments.jsonl
```

## Which rooms have save states

`../Nintaco_bin_2020-05-01/states/mapstate/mapstate_<level>_<mapLoc>.save` — 267 of them.
Level 8 has: 0, 8, 15, 30, 31, 44, 46, 60, 62, 63, 76, 78, 94, 109, 110, 111, 124, 125, 126.
Good combat rooms with real variance (measured over 39 old full-dungeon runs): **62**
(master b), **94** (mixed ba), **31** (key stai), **30** (arrow gu).

## Output

| file | one row per | what it is |
|---|---|---|
| `botoutput/zexperiment/experiments.jsonl` | run | **the A/B file.** One self-describing JSON object per trial |
| `botoutput/zexperiment/<label>_<exp>_<millis>.csv` | action | per-action detail for one trial |
| `botoutput/zexperiment/experiments.csv` | run | the old CSV summary, frozen. Nothing writes it now |
| `botoutput/zlog/cheatLog_<millis>.txt` | cheat | rupee/item writes the bot made to RAM |

## Which fields to compare, and which to ignore

Audited against 39 real `level8plan_*.csv` runs. Ignore anything in the second table —
those numbers are not measuring what their names suggest.

**Use these**

| field | why |
|---|---|
| `elapsedFrames` | **the duration.** Emulator frames, scrolling included. Speed-independent. Per-room CV 0.28–0.68 — the lowest-variance continuous metric available. Note it is *larger* than `totalFrames`. Per-action durations are not stored; subtract consecutive `frame` values in the detail CSV |
| `result` | `complete` / `dead` / `timeout`. The outcome that matters. Fisher's exact on clear rate |
| `netHeartsLost` | `heartsStart − heartsEnd`. The **net** heart delta reconciles exactly in every run |
| `damagedEvents` | transitions into the damage state, i.e. times hit |
| `damagedFrames / totalFrames` | fraction of the room spent damaged |
| `totalFrames` | frames the bot actually decided in — excludes scrolling, so it is the *smaller* number. Compare to `elapsedFrames` to see if a change moved time into screen transitions |

**Do not use these**

| field | why not |
|---|---|
| `time`, `totalTime` | wall clock, truncated to whole seconds. In one run 24 of 67 rows were `0`, and `sum(time)`=277 ≠ `totalTime`=299. Tracks emulator speed, not the bot |
| `hits`, `damage`, `heal` | sampled per frame off `lifeInHearts()`, so the heart-drain animation counts many times. Real row, level8 room 62: `hits=72 damage=40.1 heal=45.4` for a **net gain of 5.25 hearts**. Kept in the CSV as `rawHits`/`rawDamage`/`rawHeal` for debugging only |
| `totalFrames` in old detail CSVs | mislabeled — it was the emulator's *global* frame counter, which is why rows jump 409 → 192375 at the save-state load. Now written as `frame` |
| `rupees` | recorded, but the archery cheat rewrites it constantly — `cheatLog` shows 20+ `set rupee to 20` per run |
| `keys`, `potion`, `bombs` | inventory snapshots, and `runSetup` overwrites them at the start of every trial |

Deaths and timeouts are excluded from the duration comparison — they cut the room short,
so including them makes a worse arm look faster. Read the outcome rates first.

## Outcome rates

`analyzeAb` reports `cleared` / `died` / `timed out` as three separate rates, each with a
95% Wilson interval and its own Fisher's exact test. They are tested separately on
purpose: dying and timing out are different failures — dying means combat is losing,
timing out means the bot is stuck or slow — and a single "not cleared" test cannot tell
them apart.

This is the check that catches a change which trades survival for speed. A real example
from a test batch:

```
rate                         armA                   armB  fisher p
cleared            80% [ 63- 90%]         53% [ 36- 70%]    0.0539
died                3% [  1- 17%]         47% [ 30- 64%]    0.0002
timed out          17% [  7- 34%]          0% [  0- 11%]    0.0522
```

armB's cleared trials were 20% faster with p = 0.048, which looks like a win. It was
dying in half of them. The clear-rate test alone (p = 0.054) would not have flagged it;
the death-rate test does, decisively.

Where the intervals overlap, the sample does not establish a difference. The report also
prints the median frames before death and which rooms the deaths happened in, which
separates "dies instantly on entry" from "dies in a long fight it was losing".

## What the seed does and does not do

`seed=<n>` reseeds `ZRandom` at the start of each trial with `n + trialNumber`, so
trial 7 of arm A draws the same random numbers as trial 7 of arm B. That pairs the arms
and removes the bot's own RNG as a difference between them.

It does **not** make runs identical:
- The bot is a remote frame listener on Nintaco at `setSpeed(400)`. How many emulated
  frames pass between two bot decisions depends on host timing.
- The game's own RNG lives in NES RAM. The save state restores it identically, but it
  advances per emulated frame, so the timing jitter above still moves it.

So trials are still a distribution and the comparison is still statistical. Seeding
narrows the spread; it does not remove the need for n≈30.

## How many trials

At the measured CV ≈ 0.45 and n = 30 per arm, this detects roughly a **33% change** in
frames-to-clear (α .05, power .8). For clear rate, 60% → 90% needs about 33 per arm.
30 finds large effects only — the report prints its own sensitivity, read it before
concluding anything. To see smaller effects, raise `trials`.

## Confound to watch

Two separate launches means any drift between them (machine load, emulator state) is
aliased with the arm. Cheap mitigation: run **A, then B, then A again** as three batches
(`label=armA1`, `armB`, `armA2`) and check `analyzeAb armA1 armA2` shows nothing before
trusting the A-vs-B gap. That is also the pipeline self-test.

## Known issues not fixed here

- `GamePad.randomDirection(besides)` loops `while (dir != besides)`, so it returns
  exactly the direction it was told to avoid. Used by `PushAction`. Left alone
  deliberately — changing behaviour mid-experiment would confound the arms.
- `ActionPlan` `Random.nextInt(4) > 3` and `NeighborFinder` `true || ...` are both dead
  branches.
- `logCompletedStep` now appends incrementally instead of rewriting the whole file per
  action, but old CSVs in `zexperiment/` still use the old column names.
- `maxframes=` is only read on the `room_` launch branch. Passing it with a named
  experiment like `level8` is accepted and then ignored.
