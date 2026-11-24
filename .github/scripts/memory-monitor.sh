#!/usr/bin/env bash
# Registra la memoria de un runner de macOS mientras dura el job, para saber si el enlace de
# Kotlin/Native se queda sin RAM y tira de swap.
#
#   memory-monitor.sh start    deja un muestreo en segundo plano (sigue vivo entre pasos del job)
#   memory-monitor.sh report   resume lo registrado: máquina, swap, memoria libre y quién la usa
#
# Cada muestra agrupa la RAM residente por tipo de proceso: el daemon de Gradle, el compilador de
# Kotlin/Native (que enlaza en su propio proceso, con `kotlin.native.jvmArgs`), Swift, Xcode y el
# resto. Así se ve no solo SI falta memoria, sino QUIÉN la tiene.
set -euo pipefail

LOG="${RUNNER_TEMP:-/tmp}/memory-monitor.log"
INTERVAL="${MEMORY_SAMPLE_SECONDS:-10}"

sample() {
    local ts swap free swapouts procs
    ts=$(date +%H:%M:%S)
    swap=$(sysctl -n vm.swapusage | sed -E 's/.*used = ([0-9.]+)M.*/\1/')
    free=$(memory_pressure -Q 2>/dev/null | sed -nE 's/.*free percentage: ([0-9]+)%.*/\1/p')
    swapouts=$(vm_stat | awk '/^Swapouts/ { gsub(/\./, "", $2); print $2 }')
    procs=$(ps -A -o rss=,command= | awk '
        {
            rss = $1; $1 = ""; cmd = $0
            if (cmd ~ /GradleDaemon/)                  label = "gradle-daemon"
            else if (cmd ~ /KotlinCompileDaemon/)      label = "kotlin-daemon"
            else if (cmd ~ /kotlin-native|konan/)      label = "kotlin-native"
            else if (cmd ~ /GradleWrapperMain/)        label = "gradle-cli"
            else if (cmd ~ /swift-frontend|swiftc/)    label = "swift"
            else if (cmd ~ /clang/)                    label = "clang"
            else if (cmd ~ /xcodebuild|XCBBuildService|SWBBuildService/) label = "xcode"
            else                                       label = "other"
            sum[label] += rss
        }
        END { for (l in sum) printf "%s:%d,", l, sum[l] / 1024 }')
    echo "$ts|$swap|$free|$swapouts|$procs" >> "$LOG"
}

case "${1:-}" in
    start)
        : > "$LOG"
        {
            echo "# cpu: $(sysctl -n machdep.cpu.brand_string)"
            echo "# ncpu: $(sysctl -n hw.ncpu)"
            echo "# mem_gb: $(( $(sysctl -n hw.memsize) / 1024 / 1024 / 1024 ))"
            echo "# swap_total: $(sysctl -n vm.swapusage | sed -E 's/total = ([0-9.]+M).*/\1/')"
        } >> "$LOG"
        # nohup y sin heredar la salida: si el muestreo escribiera en el log del paso, el paso
        # no terminaría hasta que acabara el muestreo, que es nunca.
        nohup bash -c "while true; do \"$0\" sample || true; sleep $INTERVAL; done" > /dev/null 2>&1 &
        echo "Registrando la memoria cada ${INTERVAL}s en $LOG"
        ;;
    sample)
        sample
        ;;
    report)
        python3 - "$LOG" <<'PY'
import sys
from collections import defaultdict

meta, rows = {}, []
for line in open(sys.argv[1]):
    line = line.rstrip("\n")
    if line.startswith("# "):
        key, _, value = line[2:].partition(": ")
        meta[key] = value
        continue
    ts, swap, free, swapouts, procs = line.split("|")
    usage = {}
    for item in filter(None, procs.split(",")):
        label, _, mb = item.partition(":")
        usage[label] = int(mb)
    rows.append((ts, float(swap or 0), int(free or 0), int(swapouts or 0), usage))

print(f"Máquina: {meta.get('cpu')} · {meta.get('ncpu')} CPU · {meta.get('mem_gb')} GB de RAM · swap {meta.get('swap_total')}")
if not rows:
    print("Sin muestras.")
    sys.exit(0)

PAGE = 16384
swapped_mb = (rows[-1][3] - rows[0][3]) * PAGE / 1024 / 1024
print(f"Muestras: {len(rows)}, de {rows[0][0]} a {rows[-1][0]}")
print(f"Swap en uso, máximo: {max(r[1] for r in rows):.0f} MB")
print(f"Memoria libre del sistema, mínimo: {min(r[2] for r in rows)} %")
print(f"Escrito a swap durante el job: {swapped_mb:.0f} MB")

peak = defaultdict(int)
for *_, usage in rows:
    for label, mb in usage.items():
        peak[label] = max(peak[label], mb)
print("Pico de RAM residente por tipo de proceso:")
for label, mb in sorted(peak.items(), key=lambda kv: -kv[1]):
    print(f"  {label:<14} {mb:>6} MB")

print("Evolución (una línea por minuto):")
last_minute = None
for ts, swap, free, _, usage in rows:
    minute = ts[:5]
    if minute == last_minute:
        continue
    last_minute = minute
    top = ", ".join(f"{l} {mb}" for l, mb in sorted(usage.items(), key=lambda kv: -kv[1])[:3])
    print(f"  {ts}  swap {swap:>6.0f} MB  libre {free:>3} %  · {top}")
PY
        ;;
    *)
        echo "Uso: $0 start|report" >&2
        exit 2
        ;;
esac
