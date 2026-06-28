import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PipePerfModel {
    private static final int EXTRACT_INTERVAL_TICKS = 10;
    private static final int ACTIVE_SLOTS = 27;
    private static final int ITEM_VARIANTS = 9;
    private static final int WARMUP_ROUNDS = 5;
    private static final int MEASURE_ROUNDS = 12;
    private static final int MEASURE_REPETITIONS = 20;

    public static void main(String[] args) {
        List<Scenario> scenarios = List.of(
                new Scenario("baseline 1 in / 1 out", 64, 1, 1, 1, FilterMode.NONE, FilterMode.NONE, false, false),
                new Scenario("source filter match, 1 in / 1 out", 64, 1, 1, 1, FilterMode.MATCH_ALL, FilterMode.NONE, false, false),
                new Scenario("source filter rejects, 1 in / 1 out", 64, 1, 1, 1, FilterMode.REJECT_ALL, FilterMode.NONE, false, false),
                new Scenario("destination filter match, 1 in / 1 out", 64, 1, 1, 1, FilterMode.NONE, FilterMode.MATCH_ALL, false, false),
                new Scenario("destination filter rejects, 1 in / 1 out", 64, 1, 1, 1, FilterMode.NONE, FilterMode.REJECT_ALL, false, false),
                new Scenario("disabled 1 in / 1 out", 64, 1, 1, 1, FilterMode.MATCH_ALL, FilterMode.MATCH_ALL, false, true),

                new Scenario("baseline 32 in / 32 out shared", 128, 1, 32, 32, FilterMode.NONE, FilterMode.NONE, false, false),
                new Scenario("source filters match, 32 in / 32 out shared", 128, 1, 32, 32, FilterMode.MATCH_ALL, FilterMode.NONE, false, false),
                new Scenario("destination filters match, 32 in / 32 out shared", 128, 1, 32, 32, FilterMode.NONE, FilterMode.MATCH_ALL, false, false),
                new Scenario("selective destination filters, 32 in / 32 out shared", 128, 1, 32, 32, FilterMode.NONE, FilterMode.SELECTIVE, false, false),
                new Scenario("source + selective destination filters, 32 in / 32 out shared", 128, 1, 32, 32, FilterMode.MATCH_ALL, FilterMode.SELECTIVE, false, false),
                new Scenario("destination filters reject, 32 in / 32 out shared", 128, 1, 32, 32, FilterMode.NONE, FilterMode.REJECT_ALL, false, false),
                new Scenario("disabled 32 in / 32 out shared", 128, 1, 32, 32, FilterMode.MATCH_ALL, FilterMode.SELECTIVE, false, true),

                new Scenario("baseline 128 in / 128 out shared", 512, 1, 128, 128, FilterMode.NONE, FilterMode.NONE, false, false),
                new Scenario("source filters match, 128 in / 128 out shared", 512, 1, 128, 128, FilterMode.MATCH_ALL, FilterMode.NONE, false, false),
                new Scenario("selective destination filters, 128 in / 128 out shared", 512, 1, 128, 128, FilterMode.NONE, FilterMode.SELECTIVE, false, false),
                new Scenario("source + selective destination filters, 128 in / 128 out shared", 512, 1, 128, 128, FilterMode.MATCH_ALL, FilterMode.SELECTIVE, false, false),
                new Scenario("destination filters reject, 128 in / 128 out shared", 512, 1, 128, 128, FilterMode.NONE, FilterMode.REJECT_ALL, false, false),
                new Scenario("disabled 128 in / 128 out shared", 512, 1, 128, 128, FilterMode.MATCH_ALL, FilterMode.SELECTIVE, false, true),

                new Scenario("mixed 64 buses, 8 in / 8 out each", 128, 64, 8, 8, FilterMode.NONE, FilterMode.NONE, false, false),
                new Scenario("mixed source filters, 64 buses, 8 in / 8 out each", 128, 64, 8, 8, FilterMode.MATCH_ALL, FilterMode.NONE, false, false),
                new Scenario("mixed selective destination filters, 64 buses, 8 in / 8 out each", 128, 64, 8, 8, FilterMode.NONE, FilterMode.SELECTIVE, false, false),
                new Scenario("mixed disabled, 64 buses, 8 in / 8 out each", 128, 64, 8, 8, FilterMode.MATCH_ALL, FilterMode.SELECTIVE, false, true),

                new Scenario("blocked baseline 64 in / 64 out shared", 256, 1, 64, 64, FilterMode.NONE, FilterMode.NONE, true, false),
                new Scenario("blocked source filters, 64 in / 64 out shared", 256, 1, 64, 64, FilterMode.MATCH_ALL, FilterMode.NONE, true, false),
                new Scenario("blocked destination filters, 64 in / 64 out shared", 256, 1, 64, 64, FilterMode.NONE, FilterMode.MATCH_ALL, true, false),
                new Scenario("blocked disabled 64 in / 64 out shared", 256, 1, 64, 64, FilterMode.MATCH_ALL, FilterMode.MATCH_ALL, true, true)
        );

        System.out.println("scenario,pipes_per_network,total_pipes,networks,sources,destinations,source_filter,destination_filter,blocked_outputs,network_disabled,idle_ms,pulse_ms,toggle_ms,avg_tick_ms,approx_tps");
        for (Scenario scenario : scenarios) {
            Result result = measure(scenario);
            double avgTickMs = result.idleMs + result.pulseMs / EXTRACT_INTERVAL_TICKS;
            double tps = Math.min(20.0, 1000.0 / Math.max(50.0, avgTickMs));
            System.out.printf("%s,%d,%d,%d,%d,%d,%s,%s,%s,%s,%.4f,%.4f,%.4f,%.4f,%.2f%n",
                    scenario.name,
                    scenario.pipesPerNetwork,
                    scenario.pipesPerNetwork * scenario.networks,
                    scenario.networks,
                    scenario.sourcesPerNetwork * scenario.networks,
                    scenario.destinationsPerNetwork * scenario.networks,
                    scenario.sourceFilter,
                    scenario.destinationFilter,
                    scenario.blockedOutputs,
                    scenario.networkDisabled,
                    result.idleMs,
                    result.pulseMs,
                    result.toggleMs,
                    avgTickMs,
                    tps);
        }
    }

    private static Result measure(Scenario scenario) {
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            runRepeated(scenario, Workload.IDLE);
            runRepeated(scenario, Workload.PULSE);
            runRepeated(scenario, Workload.TOGGLE);
        }

        double idle = 0;
        double pulse = 0;
        double toggle = 0;
        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            long start = System.nanoTime();
            runRepeated(scenario, Workload.IDLE);
            idle += elapsedMs(start) / MEASURE_REPETITIONS;

            start = System.nanoTime();
            runRepeated(scenario, Workload.PULSE);
            pulse += elapsedMs(start) / MEASURE_REPETITIONS;

            start = System.nanoTime();
            runRepeated(scenario, Workload.TOGGLE);
            toggle += elapsedMs(start) / MEASURE_REPETITIONS;
        }
        return new Result(idle / MEASURE_ROUNDS, pulse / MEASURE_ROUNDS, toggle / MEASURE_ROUNDS);
    }

    private static void runRepeated(Scenario scenario, Workload workload) {
        for (int i = 0; i < MEASURE_REPETITIONS; i++) {
            switch (workload) {
                case IDLE -> runIdle(scenario);
                case PULSE -> runPulse(scenario);
                case TOGGLE -> runToggle(scenario);
            }
        }
    }

    private static void runIdle(Scenario scenario) {
        if (scenario.networkDisabled) {
            return;
        }
        int totalPipes = scenario.pipesPerNetwork * scenario.networks;
        for (int pipe = 0; pipe < totalPipes; pipe++) {
            enabledIdleTick(pipe);
        }
    }

    private static void runPulse(Scenario scenario) {
        if (scenario.networkDisabled) {
            return;
        }

        for (int network = 0; network < scenario.networks; network++) {
            for (int source = 0; source < scenario.sourcesPerNetwork; source++) {
                int sourcePipe = sourcePipe(scenario.pipesPerNetwork, scenario.sourcesPerNetwork, source);
                List<Integer> sortedPipes = cachedSortedPipes(scenario.pipesPerNetwork, sourcePipe);
                Set<Integer> blockedItems = new HashSet<>();
                Set<Integer> cachedItems = new HashSet<>();
                for (int slot = 0; slot < ACTIVE_SLOTS; slot++) {
                    int item = slot % ITEM_VARIANTS;
                    if (!matchesFilter(scenario.sourceFilter, item, source)) {
                        continue;
                    }
                    if (blockedItems.contains(item)) {
                        blackhole(slot);
                        continue;
                    }
                    if (cachedItems.contains(item)) {
                        cachedInsertCheck(item);
                        continue;
                    }
                    boolean found = findDestination(sortedPipes, scenario, item);
                    if (found) {
                        cachedItems.add(item);
                    } else {
                        blockedItems.add(item);
                    }
                }
            }
        }
    }

    private static void runToggle(Scenario scenario) {
        for (int network = 0; network < scenario.networks; network++) {
            for (int pipe = 0; pipe < scenario.pipesPerNetwork; pipe++) {
                blackhole(pipe);
            }
            for (int pipe = 0; pipe < scenario.pipesPerNetwork; pipe++) {
                blackhole((pipe * 31) ^ scenario.pipesPerNetwork);
            }
        }
    }

    private static List<Integer> cachedSortedPipes(int pipes, int sourcePipe) {
        List<Integer> sortedPipes = new ArrayList<>(pipes);
        for (int distance = 0; distance < pipes; distance++) {
            int left = sourcePipe - distance;
            int right = sourcePipe + distance;
            if (left >= 0) {
                sortedPipes.add(left);
            }
            if (right < pipes && right != left) {
                sortedPipes.add(right);
            }
        }
        return sortedPipes;
    }

    private static boolean findDestination(List<Integer> sortedPipes, Scenario scenario, int item) {
        int checked = 0;
        for (int pipe : sortedPipes) {
            checked++;
            int destinationIndex = destinationIndex(pipe, scenario.pipesPerNetwork, scenario.destinationsPerNetwork);
            if (destinationIndex < 0) {
                continue;
            }
            if (!matchesFilter(scenario.destinationFilter, item, destinationIndex)) {
                continue;
            }
            if (!scenario.blockedOutputs) {
                insertableCheck(checked, item, destinationIndex);
                return true;
            }
        }
        blackhole(checked);
        return false;
    }

    private static int sourcePipe(int pipes, int sources, int source) {
        if (sources <= 1) {
            return 0;
        }
        return Math.min(pipes - 1, source * Math.max(1, pipes / sources));
    }

    private static int destinationIndex(int pipe, int pipes, int destinations) {
        if (destinations <= 1) {
            return pipe == pipes - 1 ? 0 : -1;
        }
        int spacing = Math.max(1, pipes / destinations);
        if (pipe % spacing == spacing - 1 || pipe == pipes - 1) {
            return Math.min(destinations - 1, pipe / spacing);
        }
        return -1;
    }

    private static boolean matchesFilter(FilterMode mode, int item, int endpointIndex) {
        if (mode == FilterMode.NONE) {
            return true;
        }

        int signature = item;
        signature = signature * 31 + endpointIndex;
        signature = signature * 31 + mode.ordinal();
        blackhole(signature);

        return switch (mode) {
            case NONE, MATCH_ALL -> true;
            case REJECT_ALL -> false;
            case SELECTIVE -> endpointIndex % ITEM_VARIANTS == item;
        };
    }

    private static void enabledIdleTick(int pipe) {
        blackhole((pipe * 31) ^ (pipe >>> 3));
    }

    private static void cachedInsertCheck(int item) {
        blackhole(item * 17);
    }

    private static void insertableCheck(int checked, int item, int destinationIndex) {
        blackhole((checked * 31 + item) ^ destinationIndex);
    }

    private static double elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000.0D;
    }

    private static volatile int sink;

    private static void blackhole(int value) {
        sink ^= value;
    }

    private enum FilterMode {
        NONE,
        MATCH_ALL,
        REJECT_ALL,
        SELECTIVE
    }

    private enum Workload {
        IDLE,
        PULSE,
        TOGGLE
    }

    private record Scenario(String name, int pipesPerNetwork, int networks, int sourcesPerNetwork,
                            int destinationsPerNetwork, FilterMode sourceFilter, FilterMode destinationFilter,
                            boolean blockedOutputs, boolean networkDisabled) {
    }

    private record Result(double idleMs, double pulseMs, double toggleMs) {
    }
}
