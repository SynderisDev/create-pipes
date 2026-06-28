# Create: Pipes

Create: Pipes is a Create addon for item transport.

The idea is simple: pipes move items around like Mekanism logistical transporters, but built to feel like they belong next to Create machines.

This is still an MVP. It works, but it is not trying to be a full logistics mod yet.

## What It Adds

- Steam Item Pipes for moving items between inventories
- per-side pipe modes: Normal, Pull, Push, and None
- Create wrench support for cycling pipe side modes
- Create filter support on pull and push sides
- a Network Control Tool that can turn a whole connected pipe network on or off
- server-side routing that retries instead of voiding items when a route fills or breaks

## Current State

Steam is planned, but it is not required by default right now.

The default config is:

```toml
require_steam = false
```

That means pipes work without steam intakes for now. The steam side is planned as the balance hook later, once it has had more testing and polish.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.233 or newer for 1.21.1
- Create 6.0.10

## Notes

This mod does not copy or redistribute Create's reserved assets. Create is required as a dependency.

## Planned

- proper steam-powered mode
- better visuals
- more testing on large networks
- more routing options after the core pipe behavior is solid
