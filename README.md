# Create: Pipes

Create: Pipes is a Create addon for item transport.

The idea is simple: pipes move items around like Mekanism logistical transporters, with Create-style steam power planned as the balancing hook later.

This is still an MVP. It works, but it is not trying to be a full logistics mod yet.

## What It Adds

- `Steam Item Pipe`
  - item pipes with per-side modes
  - server-side routing
  - safe retries when destinations fill up
  - no item voiding when a route breaks

- `Steam Intake`
  - connects a pipe network to Create boiler/steam output
  - planned for the steam-powered mode
  - optional right now, because steam is disabled by default

- `Network Control Tool`
  - right-click a pipe to turn the connected pipe network on or off
  - off networks are actually offline and do not tick

## Pipe Modes

Each pipe side has a mode:

- `Normal`: connects and allows passive movement through the pipe
- `Pull`: actively extracts from the attached inventory
- `Push`: accepts routed items into the attached inventory
- `None`: disconnects that side

Use a Create wrench on the pipe. Click the pipe part closest to the inventory you want to configure.

Sneak-right-click with a wrench picks the pipe up like Create blocks do.

## Filters

Create filters can be used on pipe sides.

Filters only make sense on sides that are attached to inventories, so the pipe will not let you set a filter on an empty side or a pipe-to-pipe side.

Filters apply to both:

- pull sides, before extracting
- push sides, before choosing a destination

The filter visual is custom. It does not copy Create assets. When you look at a filter slot, it shows a small four-corner marker. When you are not looking at it, it disappears.

## Steam

Steam is planned, but it is not required by default right now.

The default is:

- `require_steam = false`

That means pipes work without steam intakes for now.

Server config:

- `require_steam = false`
- `items_per_extract = 8`
- `extract_interval_ticks = 10`
- `steam_units_per_boiler_level_per_tick = 1.0`
- `steam_units_per_item = 2.0`
- `network_steam_capacity = 1000`

When `require_steam` is turned on, pipe networks need steam from a Steam Intake. That side of the mod still needs more testing and polish.

## Performance Notes

The main cost is routing, not idle pipes.

Normal matching filters are fine. The expensive case is a network where destination filters reject everything, because the pipe has to scan through possible destinations before it can cache the item as blocked.

Recent synthetic model results:

| Setup | Average tick cost |
| --- | ---: |
| 32 in / 32 out shared baseline | `0.0086 ms` |
| 32 in / 32 out with selective destination filters | `0.0117 ms` |
| 32 in / 32 out with rejecting destination filters | `0.0350 ms` |
| 128 in / 128 out shared baseline | `0.0531 ms` |
| 128 in / 128 with selective destination filters | `0.0545 ms` |
| 128 in / 128 with rejecting destination filters | `0.4977 ms` |
| 64 isolated buses, 8192 pipes total baseline | `0.0993 ms` |
| 64 isolated buses with selective destination filters | `0.3164 ms` |
| Disabled/offline networks | `0.0000 ms` recurring tick cost |

An isolated bus means a separate pipe network, for example one chest-to-pipe-to-chest lane that is not connected to the other lanes.

## In-Game Test Commands

Operator-only commands:

```mcfunction
/createpipes perf setup
/createpipes perf setup <lanes> <length> <stacks>
/createpipes perf refill
/createpipes perf clear
```

Example:

```mcfunction
/createpipes perf setup 32 64 27
```

That builds 32 separate test lanes, each with 64 pipes and 27 source stacks.

## Development

Requirements:

- Java 21
- Minecraft `1.21.1`
- NeoForge `21.1.233`
- Create `6.0.10-280`

Build:

```powershell
.\gradlew.bat build
```

Run the dev client:

```powershell
.\gradlew.bat runClient
```

Run the synthetic pipe model:

```powershell
New-Item -ItemType Directory -Force -Path build\perf-tools
javac -d build\perf-tools tools\PipePerfModel.java
java -cp build\perf-tools PipePerfModel
```

## Current Scope

In scope right now:

- item pipes
- steam mode planned/configurable
- side modes
- Create filters
- network on/off tool
- basic performance tooling

Not in scope yet:

- tiers
- colors
- redstone routing
- restrictive pipes
- fancy packet rendering

## License

This project is currently All Rights Reserved.

Create is required as a dependency, but this project should not copy or redistribute Create's reserved assets.
