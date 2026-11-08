#!/usr/bin/env python3
"""Portable ADB connection and privacy-limited, read-only device profiling.

Python standard library only. No install, reboot, tcpip, root, settings write,
HOME change, Binder transaction, raw log capture or location/media dump command.
"""
import argparse
import datetime as dt
import errno
import ipaddress
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import sys

STATUS = ("verified", "detected", "unknown", "unavailable")
PROPERTIES = {
    "manufacturer": "ro.product.manufacturer", "model": "ro.product.model",
    "android": "ro.build.version.release", "sdk": "ro.build.version.sdk",
    "board_platform": "ro.board.platform", "hardware": "ro.hardware",
    "abis": "ro.product.cpu.abilist", "abi_fallback": "ro.product.cpu.abi",
    "soc_model": "ro.soc.model", "fyt_platform": "ro.fyt.platform",
    "adb_tcp_port": "service.adb.tcp.port", "adb_persist_port": "persist.adb.tcp.port",
    "adb_tls_port": "service.adb.tls.port",
}
QUERIES = {"prop_" + name: ("shell", "getprop", key) for name, key in PROPERTIES.items()}
QUERIES.update({
    "screen_size": ("shell", "wm", "size"),
    "screen_density": ("shell", "wm", "density"),
    "memory": ("shell", "cat", "/proc/meminfo"),
    "features": ("shell", "pm", "list", "features"),
    "packages": ("shell", "pm", "list", "packages", "--show-versioncode"),
    "packages_fallback": ("shell", "pm", "list", "packages"),
    "binder_service_names": ("shell", "service", "list"),
    "dump_service_names": ("shell", "dumpsys", "-l"),
    "home": ("shell", "cmd", "package", "resolve-activity", "--brief", "--user", "current",
             "-a", "android.intent.action.MAIN", "-c", "android.intent.category.HOME"),
    "location_mode": ("shell", "settings", "--user", "current", "get", "secure", "location_mode"),
})
PACKAGE = re.compile(r"[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+\Z")
RELEVANT = re.compile(r"radio|navimods|amazon|waze|youtube|maps|music|syu|fyt|canbus", re.I)
SERVICE = re.compile(r"[A-Za-z0-9_.:/@-]{1,160}\Z")


class DiagnosticError(Exception):
    pass


def validate_host(value):
    value = value.strip()
    if value.startswith("[") and value.endswith("]"):
        value = value[1:-1]
    try:
        ipaddress.ip_address(value)
        return value
    except ValueError:
        if len(value) <= 253 and all(re.fullmatch(r"[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?", p)
                                     for p in value.rstrip(".").split(".")):
            return value
    raise argparse.ArgumentTypeError("Usa una IP o nombre de host, sin puerto ni comandos.")


def port(value):
    try:
        result = int(value)
    except (ValueError, TypeError):
        raise argparse.ArgumentTypeError("Puerto inválido.")
    if not 1 <= result <= 65535:
        raise argparse.ArgumentTypeError("El puerto debe estar entre 1 y 65535.")
    return result


def endpoint(host, number):
    return ("[" + host + "]" if ":" in host else host) + ":" + str(number)


def validate_device(value):
    if not re.fullmatch(r"[A-Za-z0-9_\[\].:%-]{1,253}", value) or value.startswith("-"):
        raise argparse.ArgumentTypeError("Identificador ADB inválido; cópialo de adb devices -l.")
    return value


def failure(text):
    low = text.lower()
    for marker, reason in [("unauthorized", "unauthorized"), ("offline", "offline"),
                           ("no route to host", "no_route_to_host"),
                           ("permission denied", "permission_denied"), ("securityexception", "permission_denied"),
                           ("operation not permitted", "host_permission_denied"),
                           ("unknown command", "unsupported"), ("unknown option", "unsupported"),
                           ("not found", "not_found"), ("refused", "connection_refused")]:
        if marker in low:
            return reason
    return "command_failed"


class Adb:
    def __init__(self, executable="adb", device=None):
        self.executable = shutil.which(executable)
        if not self.executable:
            raise DiagnosticError("ADB no encontrado. Instala Platform Tools y revisa PATH o --adb.")
        self.device = validate_device(device) if device else None

    def run(self, *args, interactive=False):
        command = [self.executable] + list(args)
        try:
            if interactive:
                return subprocess.run(command, timeout=120).returncode
            result = subprocess.run(command, capture_output=True, encoding="utf-8", errors="replace", timeout=12)
        except subprocess.TimeoutExpired:
            raise DiagnosticError("timeout")
        except OSError:
            raise DiagnosticError("host_execution_failed")
        text = result.stdout + result.stderr
        if len(text) > 262144:
            raise DiagnosticError("output_limit")
        if result.returncode:
            raise DiagnosticError(failure(text))
        return text

    def require_device(self):
        if not self.device:
            raise DiagnosticError("Selecciona explícitamente --device; nunca se elige el primer dispositivo.")
        # Listing is local; every remote command below uses -s.
        listing = self.run("devices")
        states = dict((parts[0], parts[1]) for line in listing.splitlines()
                      if len(parts := line.split()) >= 2 and parts[1] in ("device", "offline", "unauthorized"))
        state = states.get(self.device)
        if state != "device":
            raise DiagnosticError(state or "device_not_connected")
        if self.run("-s", self.device, "get-state").strip() != "device":
            raise DiagnosticError("device_not_ready")

    def read(self, name):
        # Deliberate allowlist: no arbitrary shell strings or commands from the device/profile.
        if name not in QUERIES:
            raise DiagnosticError("query_not_allowed")
        if not self.device:
            raise DiagnosticError("device_required")
        text = self.run("-s", self.device, *QUERIES[name]).strip()
        if re.search(r"permission denial|securityexception|unknown command|unknown option|error:", text, re.I):
            raise DiagnosticError(failure(text))
        return text


def observation(value=None, status="unknown", evidence=None, limitation=None):
    return {"status": status, "value": value, "evidence": evidence or [], "limitation": limitation}


def parse_size(text):
    result = {}
    for kind, width, height in re.findall(r"(Physical|Override) size:\s*(\d+)x(\d+)", text):
        if 0 < int(width) <= 32768 and 0 < int(height) <= 32768:
            result[kind.lower()] = {"width": int(width), "height": int(height)}
    if result:
        result["effective"] = result.get("override", result.get("physical"))
    return result or None


def parse_density(text):
    result = {kind.lower(): int(value) for kind, value in re.findall(r"(Physical|Override) density:\s*(\d+)", text)
              if 0 < int(value) <= 10000}
    if result:
        result["effective"] = result.get("override", result.get("physical"))
    return result or None


def relevant_packages(text):
    found = {}
    for line in text.splitlines():
        match = re.fullmatch(r"package:([A-Za-z0-9_.]+)(?:\s+versionCode:(\d+))?", line.strip())
        if match and PACKAGE.fullmatch(match[1]) and RELEVANT.search(match[1]):
            found[match[1]] = {"package": match[1], "version_code": int(match[2]) if match[2] else None}
    return sorted(found.values(), key=lambda p: p["package"])


def collect(adb):
    adb.require_device()
    errors = []
    succeeded = []

    def query(name):
        try:
            value = adb.read(name)
            succeeded.append(name)
            return value
        except DiagnosticError as error:
            errors.append({"query": name, "reason": str(error)})
            return None

    def property_value(name):
        value = query("prop_" + name)
        return value if value and len(value) <= 160 and not any(ord(c) < 32 for c in value) else None

    props = {name: property_value(name) for name in PROPERTIES}
    size, density = parse_size(query("screen_size") or ""), parse_density(query("screen_density") or "")
    memory_text = query("memory") or ""
    memory = {name: int(value) for name, value in re.findall(r"^(MemTotal|MemAvailable):\s+(\d+) kB$", memory_text, re.M)}
    features_text = query("features")
    features = sorted(set(re.findall(r"^feature:([A-Za-z0-9_.]+)(?:=\S+)?$", features_text or "", re.M)))
    package_text = query("packages")
    if package_text is None:
        package_text = query("packages_fallback")
    packages = relevant_packages(package_text or "")
    service_text, dump_text = query("binder_service_names"), query("dump_service_names")
    service_names = set()
    for line in (service_text or "").splitlines():
        match = re.match(r"^\s*\d+\s+([^\s:]+):", line)
        if match and SERVICE.fullmatch(match[1]):
            service_names.add(match[1])
    for line in (dump_text or "").splitlines():
        if SERVICE.fullmatch(line.strip()):
            service_names.add(line.strip())
    media_services = sorted(s for s in service_names if re.search(r"media|audio", s, re.I))
    vendor_services = sorted(s for s in service_names if re.search(r"fyt|syu|canbus|mcu", s, re.I))
    home_text = query("home") or ""
    home = next((line.strip() for line in reversed(home_text.splitlines())
                 if re.fullmatch(r"[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+", line.strip())), None)
    mode = query("location_mode")
    configured_ports = sorted(set(int(props[key]) for key in ("adb_tcp_port", "adb_persist_port", "adb_tls_port")
                                  if props[key] and props[key].isdigit() and 0 < int(props[key]) <= 65535))
    hardware_keys = ("manufacturer", "model", "android", "board_platform", "hardware", "soc_model", "fyt_platform")
    hardware = {name: props[name] for name in hardware_keys}
    hardware["sdk"] = int(props["sdk"]) if props["sdk"] and props["sdk"].isdigit() else None
    hardware["abis"] = (props["abis"] or props["abi_fallback"] or "").split(",")
    hardware["abis"] = [abi for abi in hardware["abis"] if re.fullmatch(r"[A-Za-z0-9_-]+", abi)]
    declared_gps = "android.hardware.location.gps" in features
    declared_compass = "android.hardware.sensor.compass" in features
    capabilities = {
        "gps": observation({"declared": declared_gps if features_text is not None else None,
                            "location_enabled_setting": mode != "0" if mode in ("0", "1", "2", "3") else None},
                           "detected" if features_text is not None else "unknown", ["features", "location_mode"],
                           "Una declaración o ajuste no verifica recepción, precisión ni velocidad real."),
        "compass": observation({"magnetic_sensor_declared": declared_compass if features_text is not None else None},
                               "detected" if features_text is not None else "unknown", ["features"],
                               "El dashboard actual usa rumbo GPS al moverse; no es una brújula magnética."),
        "media_sessions": observation(media_services, "detected" if media_services else "unknown", ["binder_service_names", "dump_service_names"],
                                      "La presencia del servicio no confirma permisos, metadatos o controles por aplicación."),
        "radio": observation([p for p in packages if "radio" in p["package"].lower()],
                             "detected" if any("radio" in p["package"].lower() for p in packages) else "unknown", ["packages"],
                             "Una aplicación instalada no demuestra una interfaz FM accesible."),
        "vehicle_information": observation(None, "unknown", ["vendor_services"],
                                           "Servicios FYT/SYU/CANBUS detectados no equivalen a telemetría legible. Sin llamadas Binder."),
        "outside_temperature": observation(None, "unknown", [],
                                           "No se ha verificado fuente física exterior; CPU/batería/clima no sustituyen este dato."),
        "wireless_pairing": observation(None, "unknown", ["prop_sdk", "prop_adb_tls_port"],
                                       "Android 11+ no garantiza que el fabricante exponga emparejamiento. Revisar su interfaz autorizada."),
    }
    profile = {
        "schema_version": 1, "kind": "device_diagnostic",
        "collected_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "privacy": {"serial_ip_mac_location_accounts_included": False, "raw_logs_or_app_data_included": False},
        "hardware": hardware,
        "display": {"size": observation(size, "detected" if size else "unknown", ["screen_size"]),
                    "density": observation(density, "detected" if density else "unknown", ["screen_density"])},
        "memory_kib": memory, "declared_sensor_features": [f for f in features if ".sensor." in f],
        "relevant_packages": packages, "current_home": observation(home, "detected" if home else "unknown", ["home"]),
        "vendor_services": vendor_services, "capabilities": capabilities,
        "adb": {"observed_transport": "tcp" if ":" in adb.device else "emulator" if adb.device.startswith("emulator-") else "usb_or_other",
                "connected": observation(True, "verified", ["get-state"], "Estado comprobado al inicio; no certifica todos los servicios ni una conexión permanente."), "reported_ports": configured_ports,
                "reported_ports_limitation": "Propiedades declaradas; no prueba de escucha ni de exposición accesible."},
        "query_summary": {"successful": succeeded, "errors": errors},
    }
    return profile


def markdown(profile):
    def safe(value):
        return "`" + json.dumps(value, ensure_ascii=False).replace("`", "\\u0060").replace("|", "\\|").replace("<", "\\u003c").replace(">", "\\u003e") + "`"
    lines = ["# Diagnóstico Android", "", "Informe técnico limitado; sin IP, serial, cuentas, ubicación, contenido multimedia o datos privados.",
             "", "## Hardware", "", "| Campo | Valor informado |", "|---|---|"]
    lines += ["| " + name + " | " + safe(value) + " |" for name, value in profile["hardware"].items()]
    lines += ["", "## Pantalla y memoria", "", safe(profile["display"]), "", safe(profile["memory_kib"]),
              "", "## Capacidades", "", "Verificado: comprobación concreta; detectado: evidencia indirecta; desconocido: no probado o no accesible.", ""]
    for name, item in profile["capabilities"].items():
        lines += ["### " + name, "", "Estado: **" + item["status"] + "**. Valor: " + safe(item["value"]), "", item["limitation"] or "", ""]
    lines += ["## Aplicaciones relevantes", ""] + ["- `" + app["package"] + "` · versionCode " + str(app["version_code"]) for app in profile["relevant_packages"]]
    lines += ["", "## HOME actual (solo lectura)", "", safe(profile["current_home"]), "", "## Servicios de fabricante (solo nombres)", "", safe(profile["vendor_services"]),
              "", "## ADB", "", safe(profile["adb"]), "", "## Consultas no disponibles", "", safe(profile["query_summary"]["errors"]),
              "", "No instalar ni adaptar código sin revisar este perfil y aprobar un plan. Un error de permisos no demuestra ausencia del hardware.", ""]
    return "\n".join(lines)


def write_profile(profile, output):
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    files = {"device-profile.json": json.dumps(profile, ensure_ascii=False, indent=2) + "\n", "device-report.md": markdown(profile)}
    if any((output / name).exists() for name in files):
        raise DiagnosticError("Los archivos de diagnóstico ya existen; elige otra carpeta, no se sobrescriben.")
    created = []
    try:
        for name, data in files.items():
            path = output / name
            with path.open("x", encoding="utf-8") as stream:
                created.append(path)
                stream.write(data)
            try:
                path.chmod(0o600)
            except OSError:
                pass
    except BaseException:
        for path in created:
            path.unlink()
        raise


def network(host, ports):
    if len(ports) > 8:
        raise DiagnosticError("Como máximo ocho puertos indicados explícitamente; no se escanea la red.")
    ping = shutil.which("ping")
    ping_ok = None
    if ping:
        args = [ping, "-n", "1", "-w", "1500", host] if os.name == "nt" else [ping, "-c", "1", "-W", "1500" if sys.platform == "darwin" else "2", host]
        try:
            ping_ok = subprocess.run(args, capture_output=True, timeout=4).returncode == 0
        except (subprocess.TimeoutExpired, OSError):
            ping_ok = False
    result = {"ping": ping_ok, "tcp": []}
    for number in ports:
        try:
            with socket.create_connection((host, number), timeout=2):
                result["tcp"].append({"port": number, "reachable": True})
        except OSError as error:
            reason = {errno.EHOSTUNREACH: "no_route_to_host", errno.ENETUNREACH: "network_unreachable",
                      errno.ECONNREFUSED: "connection_refused", errno.ETIMEDOUT: "timeout",
                      errno.EACCES: "host_permission_denied", errno.EPERM: "host_permission_denied"}.get(error.errno, error.__class__.__name__)
            result["tcp"].append({"port": number, "reachable": False, "reason": reason})
    return result


def main(argv=None):
    parser = argparse.ArgumentParser(description="Conexión ADB y diagnóstico Android limitado. Nunca instala ni cambia el sistema.")
    parser.add_argument("--adb", default="adb", help="Ruta a adb/adb.exe si no está en PATH")
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("doctor", help="Verificar ADB y listar transportes locales (no exportar su salida)")
    commands.add_parser("discover", help="Consultar anuncios mDNS; no garantiza descubrir equipos antiguos")
    for command in ("connect", "pair", "network"):
        sub = commands.add_parser(command)
        sub.add_argument("--host", type=validate_host, required=True)
        sub.add_argument("--port", type=port, required=True, action="append" if command == "network" else "store")
    sub = commands.add_parser("profile")
    sub.add_argument("--device", type=validate_device, required=True)
    sub.add_argument("--output", default="profiles/private/" + dt.datetime.now().strftime("%Y%m%d_%H%M%S"))
    args = parser.parse_args(argv)
    try:
        if args.command == "network":
            print(json.dumps(network(args.host, args.port), ensure_ascii=False, indent=2))
            print("Ping/TCP no confirman que ADB esté habilitado ni autorizado.")
            return 0
        adb = Adb(args.adb, getattr(args, "device", None))
        if args.command == "doctor":
            print(adb.run("version"));print(adb.run("devices", "-l"))
            print("Salida local: contiene identificadores de transporte. No publicarla sin revisar.")
        elif args.command == "discover":
            print(adb.run("mdns", "services"))
            print("Usa solo el equipo reconocido por su propietario. No guardar anuncios con identificadores en el repositorio.")
        elif args.command == "connect":
            response = adb.run("connect", endpoint(args.host, args.port))
            if not re.search(r"(?:already )?connected to", response, re.I):
                raise DiagnosticError(failure(response))
            print(response)
        elif args.command == "pair":
            if not sys.stdin.isatty():
                raise DiagnosticError("Empareja desde una terminal interactiva; nunca pases el código en un archivo o argumento.")
            result = adb.run("pair", endpoint(args.host, args.port), interactive=True)
            if result:
                raise DiagnosticError("pair_failed")
        elif args.command == "profile":
            profile = collect(adb)
            write_profile(profile, args.output)
            print("Creado " + str(Path(args.output) / "device-profile.json"))
            print("Creado " + str(Path(args.output) / "device-report.md"))
            print("Revisa antes de compartir. Funciones de vehículo y controles multimedia siguen sin verificar.")
        return 0
    except (DiagnosticError, OSError) as error:
        print("Diagnóstico detenido: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
